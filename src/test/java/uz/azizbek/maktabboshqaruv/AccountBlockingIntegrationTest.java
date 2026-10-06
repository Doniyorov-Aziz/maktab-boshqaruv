package uz.azizbek.maktabboshqaruv;

import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.*;
import uz.azizbek.maktabboshqaruv.service.AccountStatusService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

/** Ta'tildagi xodim: login 403, allaqachon kirgan bo'lsa keyingi so'rov 401. Over real HTTP. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "telegram.mock=true",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000"
})
@ActiveProfiles("local")
class AccountBlockingIntegrationTest {

    private static final String PASSWORD = "test-pass-" + ThreadLocalRandom.current().nextInt(1_000_000);

    @LocalServerPort
    private int port;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private SchoolRepository schoolRepository;
    @Autowired
    private PositionRepository positionRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AccountStatusService accountStatusService;

    private final HttpClient http = HttpClient.newHttpClient();
    private Employee employee;
    private User user;

    @BeforeEach
    void setUp() {
        int n = ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999);
        employee = new Employee();
        employee.setSchool(schoolRepository.findAll().get(0));
        employee.setPosition(positionRepository.findAll().get(0));
        employee.setFirstName("Test");
        employee.setLastName("Tatilchi");
        employee.setPhone("+9989" + n);
        employee = employeeRepository.save(employee);
        user = new User();
        user.setUsername("tatil_" + n);
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setRole(Role.EDITOR);
        user.setEmployee(employee);
        user = userRepository.save(user);
    }

    @AfterEach
    void cleanUp() {
        userRepository.delete(user);
        employeeRepository.delete(employee);
        accountStatusService.evictAll();
    }

    private HttpResponse<String> login() throws Exception {
        String body = "{\"username\":\"" + user.getUsername() + "\",\"password\":\"" + PASSWORD + "\"}";
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private void setStatus(EmployeeStatus status, LocalDate from, LocalDate to) {
        employee.setStatus(status);
        employee.setLeaveFrom(from);
        employee.setLeaveTo(to);
        employee = employeeRepository.save(employee);
        accountStatusService.evictAll();
    }

    @Test
    void userOnLeave_cannotLogIn_403() throws Exception {
        setStatus(EmployeeStatus.ON_LEAVE, LocalDate.now().minusDays(1), LocalDate.now().plusDays(5));
        HttpResponse<String> r = login();
        assertEquals(403, r.statusCode());
        assertTrue(r.body().contains("ta'til"), r.body());

        setStatus(EmployeeStatus.DISMISSED, null, null);
        assertEquals(403, login().statusCode());
    }

    @Test
    void leavePlannedForLater_doesNotBlockYet() throws Exception {
        setStatus(EmployeeStatus.ON_LEAVE, LocalDate.now().plusDays(3), LocalDate.now().plusDays(10));
        assertEquals(200, login().statusCode());
    }

    @Test
    void signedInUser_blockedLater_nextRequest401() throws Exception {
        HttpResponse<String> r = login();
        assertEquals(200, r.statusCode());
        String token = r.body().trim();
        String path = "/api/employees?schoolId=" + employee.getSchool().getId() + "&size=1";
        assertEquals(200, get(path, token).statusCode());

        setStatus(EmployeeStatus.ON_LEAVE, null, null);
        HttpResponse<String> blocked = get(path, token);
        assertEquals(401, blocked.statusCode());
        assertEquals("1", blocked.headers().firstValue("X-Account-Blocked").orElse(null));
    }

    @Test
    void wrongPassword_staysA401_evenWhenBlocked() throws Exception {
        setStatus(EmployeeStatus.ON_LEAVE, null, null);
        String body = "{\"username\":\"" + user.getUsername() + "\",\"password\":\"wrong\"}";
        HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(401, r.statusCode(), "a 403 here would reveal that the username exists");
    }

    @Test
    void endedLeave_returnsToWork_byDailyJob() {
        setStatus(EmployeeStatus.ON_LEAVE, LocalDate.now().minusDays(10), LocalDate.now().minusDays(1));
        assertTrue(accountStatusService.returnFromLeave() >= 1);
        assertEquals(EmployeeStatus.ACTIVE, employeeRepository.findById(employee.getId()).orElseThrow().getStatus());
    }
}
