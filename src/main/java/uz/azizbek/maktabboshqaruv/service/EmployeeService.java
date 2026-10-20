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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.service.pages.ListStatsService;
import uz.azizbek.maktabboshqaruv.util.PageSort;

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

    @Autowired
    private ListStatsService listStats;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    public Page<EmployeeResponseDto> getAllEmployees(Long schoolId, Pageable pageable) {
        return getAllEmployees(schoolId, null, null, null, pageable);
    }

    public Page<EmployeeResponseDto> getAllEmployees(Long schoolId, Long positionId, EmployeeStatus status,
                                                     Pageable pageable) {
        return getAllEmployees(schoolId, positionId, status, null, pageable);
    }

    /** What the list may sort by: its columns → the entity's fields. */
    private static final Map<String, List<String>> SORTABLE = Map.of(
            "id", List.of("id"),
            "fullName", List.of("lastName", "firstName"),
            "lastName", List.of("lastName", "firstName"),
            "positionTitle", List.of("position.title", "lastName"),
            "status", List.of("status", "lastName"),
            "phone", List.of("phone"));

    /**
     * One page of employees with what they teach and their numbers: the page (position and school
     * fetched with it), its count, what they teach, which classes they lead, lessons a week — 5
     * queries per page whatever its size; journal completeness comes from the cache.
     */
    public Page<EmployeeResponseDto> getAllEmployees(Long schoolId, Long positionId, EmployeeStatus status, String q,
                                                     Pageable pageable) {
        return getAllEmployees(schoolId, positionId, status, q, false, pageable);
    }

    /** classTeacher: only those who lead a class ("Sinf rahbarlari" chip). */
    public Page<EmployeeResponseDto> getAllEmployees(Long schoolId, Long positionId, EmployeeStatus status, String q,
                                                     boolean classTeacher, Pageable pageable) {
        Pageable sorted = PageSort.map(pageable, SORTABLE);
        Page<Employee> page = employeeRepository.search(schoolId, positionId, status, PageSort.like(q), classTeacher, sorted);
        List<EmployeeResponseDto> rows = withTeaching(page.getContent());
        Map<Long, Double> journal = listStats.journal7(schoolId);
        for (EmployeeResponseDto dto : rows) dto.setJournal7(journal.get(dto.getId()));
        return new PageImpl<>(rows, sorted, page.getTotalElements());
    }

    public EmployeeResponseDto getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday xodim topilmadi: " + id));
        return withTeaching(List.of(employee)).get(0);
    }

    @Transactional
    public EmployeeResponseDto createEmployee(EmployeeRequestDto request) {
        if (employeeRepository.existsByPhone(request.getPhone())) {
            throw new IllegalStateException("Bu telefon raqam bilan xodim allaqachon mavjud");
        }

        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday maktab mavjud emas"));
        Position position = positionRepository.findById(request.getPositionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday lavozim mavjud emas"));

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday xodim topilmadi: " + id));

        if (!employee.getPhone().equals(request.getPhone()) && employeeRepository.existsByPhone(request.getPhone())) {
            throw new IllegalStateException("Bu telefon raqam bilan xodim allaqachon mavjud");
        }

        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday maktab mavjud emas"));
        Position position = positionRepository.findById(request.getPositionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday lavozim mavjud emas"));

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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday xodim topilmadi: " + id);
        }
        employeeRepository.deleteById(id);
    }

    /**
     * What the employees teach (subjects, classes, lessons a week) and which classes they lead, in
     * ONE statement for the whole page (through Hibernate, so its statistics count it).
     */
    @SuppressWarnings("unchecked")
    private List<EmployeeResponseDto> withTeaching(List<Employee> employees) {
        if (employees.isEmpty()) return List.of();
        String ids = "{" + String.join(",", employees.stream().map(e -> String.valueOf(e.getId())).toList()) + "}";
        Map<Long, TreeSet<String>> subjects = new HashMap<>();
        Map<Long, TreeSet<String>> classes = new HashMap<>();
        Map<Long, TreeSet<String>> leads = new HashMap<>();
        Map<Long, Integer> weekly = new HashMap<>();
        List<Object[]> found = em.createNativeQuery("""
                        select 'T' as kind, ls.employee_id, s.name, c.grade_number, c.section_letter, count(*) as n
                          from lesson_slot ls join subject s on s.id = ls.subject_id join school_class c on c.id = ls.class_id
                         where ls.employee_id = any(cast(:ids as bigint[]))
                         group by ls.employee_id, s.name, c.grade_number, c.section_letter
                        union all
                        select 'L', c.class_teacher_id, null, c.grade_number, c.section_letter, 0
                          from school_class c where c.class_teacher_id = any(cast(:ids as bigint[]))""")
                .setParameter("ids", ids).getResultList();
        for (Object[] r : found) {
            long eid = ((Number) r[1]).longValue();
            String cls = r[3] + "-" + r[4];
            if ("L".equals(String.valueOf(r[0]))) {
                leads.computeIfAbsent(eid, k -> new TreeSet<>(CLASS_ORDER)).add(cls);
                continue;
            }
            subjects.computeIfAbsent(eid, k -> new TreeSet<>()).add((String) r[2]);
            classes.computeIfAbsent(eid, k -> new TreeSet<>(CLASS_ORDER)).add(cls);
            weekly.merge(eid, ((Number) r[5]).intValue(), Integer::sum);
        }
        List<EmployeeResponseDto> rows = new ArrayList<>();
        for (Employee e : employees) {
            EmployeeResponseDto dto = toResponseDto(e);
            dto.setSubjects(join(subjects.get(e.getId())));
            dto.setClasses(join(classes.get(e.getId())));
            dto.setClassTeacherOf(join(leads.get(e.getId())));
            dto.setClassTeacher(leads.containsKey(e.getId()));
            dto.setWeeklyLessons(weekly.getOrDefault(e.getId(), 0));
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
        dto.setPositionDefaultRole(employee.getPosition().getDefaultRole() == null ? null
                : employee.getPosition().getDefaultRole().name());
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
