package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.EmployeeRequestDto;
import uz.azizbek.maktabboshqaruv.dto.EmployeeResponseDto;
import uz.azizbek.maktabboshqaruv.entity.Employee;
import uz.azizbek.maktabboshqaruv.entity.EmployeeStatus;
import uz.azizbek.maktabboshqaruv.entity.Position;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.repository.EmployeeRepository;
import uz.azizbek.maktabboshqaruv.repository.LessonSlotRepository;
import uz.azizbek.maktabboshqaruv.repository.PositionRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolClassRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private BotCache botCache;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private LessonSlotRepository lessonSlotRepository;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private AccountStatusService accountStatusService;

    public Page<EmployeeResponseDto> getAllEmployees(Long schoolId, Pageable pageable) {
        return getAllEmployees(schoolId, null, null, pageable);
    }

    /** One page of employees with what they teach: 3 queries per page, whatever its size. */
    public Page<EmployeeResponseDto> getAllEmployees(Long schoolId, Long positionId, EmployeeStatus status,
                                                     Pageable pageable) {
        Page<Employee> page = employeeRepository.search(schoolId, positionId, status, pageable);
        List<EmployeeResponseDto> rows = withTeaching(page.getContent());
        return new PageImpl<>(rows, pageable, page.getTotalElements());
    }

    public EmployeeResponseDto getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday xodim topilmadi: " + id));
        return withTeaching(List.of(employee)).get(0);
    }

    @Transactional
    public EmployeeResponseDto createEmployee(EmployeeRequestDto request) {
        if (employeeRepository.existsByPhone(request.getPhone())) {
            throw new IllegalStateException("Bu telefon raqam bilan xodim allaqachon mavjud");
        }

        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new IllegalStateException("Bunday maktab mavjud emas"));
        Position position = positionRepository.findById(request.getPositionId())
                .orElseThrow(() -> new IllegalStateException("Bunday lavozim mavjud emas"));

        Employee employee = new Employee();
        employee.setSchool(school);
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setPhone(request.getPhone());
        employee.setPosition(position);
        applyStatus(employee, request);

        Employee saved = employeeRepository.save(employee);
        return toResponseDto(saved);
    }

    @Transactional
    public EmployeeResponseDto updateEmployee(Long id, EmployeeRequestDto request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday xodim topilmadi: " + id));

        if (!employee.getPhone().equals(request.getPhone()) && employeeRepository.existsByPhone(request.getPhone())) {
            throw new IllegalStateException("Bu telefon raqam bilan xodim allaqachon mavjud");
        }

        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new IllegalStateException("Bunday maktab mavjud emas"));
        Position position = positionRepository.findById(request.getPositionId())
                .orElseThrow(() -> new IllegalStateException("Bunday lavozim mavjud emas"));

        employee.setSchool(school);
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setPhone(request.getPhone());
        employee.setPosition(position);
        applyStatus(employee, request);

        Employee updated = employeeRepository.save(employee);
        botCache.evictAllClasses(); // teacher names in the bot's cached timetables
        return getEmployeeById(updated.getId());
    }

    /** Status and leave dates; refuses to lock out the current user or the last admin. */
    private void applyStatus(Employee employee, EmployeeRequestDto request) {
        EmployeeStatus status = request.getStatus() != null ? request.getStatus() : employee.getStatus();
        java.time.LocalDate from = status == EmployeeStatus.ON_LEAVE ? request.getLeaveFrom() : null;
        java.time.LocalDate to = status == EmployeeStatus.ON_LEAVE ? request.getLeaveTo() : null;
        if (from != null && to != null && to.isBefore(from)) {
            throw new IllegalStateException("Ta'til tugash sanasi boshlanishidan oldin bo'lishi mumkin emas");
        }
        accountStatusService.checkCanSetStatus(employee, status, from, to);
        boolean changed = employee.getStatus() != status || !Objects.equals(employee.getLeaveFrom(), from)
                || !Objects.equals(employee.getLeaveTo(), to);
        employee.setStatus(status);
        employee.setLeaveFrom(from);
        employee.setLeaveTo(to);
        if (changed) accountStatusService.evictAll();
    }

    @Transactional
    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new IllegalStateException("Bunday xodim topilmadi: " + id);
        }
        employeeRepository.deleteById(id);
    }

    private List<EmployeeResponseDto> withTeaching(List<Employee> employees) {
        if (employees.isEmpty()) return List.of();
        List<Long> ids = employees.stream().map(Employee::getId).toList();
        Map<Long, TreeSet<String>> subjects = new HashMap<>();
        Map<Long, TreeSet<String>> classes = new HashMap<>();
        for (Object[] r : lessonSlotRepository.teachingOf(ids)) {
            Long eid = (Long) r[0];
            subjects.computeIfAbsent(eid, k -> new TreeSet<>()).add((String) r[1]);
            classes.computeIfAbsent(eid, k -> new TreeSet<>(CLASS_ORDER)).add(r[2] + "-" + r[3]);
        }
        Map<Long, TreeSet<String>> leads = new HashMap<>();
        for (Object[] r : schoolClassRepository.ledBy(ids)) {
            leads.computeIfAbsent((Long) r[0], k -> new TreeSet<>(CLASS_ORDER)).add(r[1] + "-" + r[2]);
        }
        List<EmployeeResponseDto> rows = new ArrayList<>();
        for (Employee e : employees) {
            EmployeeResponseDto dto = toResponseDto(e);
            dto.setSubjects(join(subjects.get(e.getId())));
            dto.setClasses(join(classes.get(e.getId())));
            dto.setClassTeacherOf(join(leads.get(e.getId())));
            rows.add(dto);
        }
        return rows;
    }

    /** "5-A" before "10-A": by grade number, then letter. */
    private static final Comparator<String> CLASS_ORDER = Comparator
            .comparingInt((String c) -> Integer.parseInt(c.substring(0, c.indexOf('-'))))
            .thenComparing(c -> c.substring(c.indexOf('-') + 1));

    private static String join(Collection<String> values) {
        return values == null || values.isEmpty() ? null : String.join(", ", values);
    }

    private EmployeeResponseDto toResponseDto(Employee employee) {
        EmployeeResponseDto dto = new EmployeeResponseDto();
        dto.setId(employee.getId());
        dto.setFirstName(employee.getFirstName());
        dto.setLastName(employee.getLastName());
        dto.setFullName(employee.getFirstName() + " " + employee.getLastName());
        dto.setPhone(employee.getPhone());
        dto.setPositionId(employee.getPosition().getId());
        dto.setPositionTitle(employee.getPosition().getTitle());
        dto.setStatus(employee.getStatus().name());
        dto.setLeaveFrom(employee.getLeaveFrom());
        dto.setLeaveTo(employee.getLeaveTo());
        if (employee.getSchool() != null) {
            dto.setSchoolId(employee.getSchool().getId());
            dto.setSchoolName(employee.getSchool().getName());
        }
        return dto;
    }
}
