package uz.azizbek.maktabboshqaruv.config;

import uz.azizbek.maktabboshqaruv.entity.AcademicYear;
import uz.azizbek.maktabboshqaruv.entity.Building;
import uz.azizbek.maktabboshqaruv.entity.Employee;
import uz.azizbek.maktabboshqaruv.entity.LessonSlot;
import uz.azizbek.maktabboshqaruv.entity.Position;
import uz.azizbek.maktabboshqaruv.entity.Role;
import uz.azizbek.maktabboshqaruv.entity.Room;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.entity.SchoolClass;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.entity.Subject;
import uz.azizbek.maktabboshqaruv.entity.User;
import uz.azizbek.maktabboshqaruv.repository.AcademicYearRepository;
import uz.azizbek.maktabboshqaruv.repository.BuildingRepository;
import uz.azizbek.maktabboshqaruv.repository.EmployeeRepository;
import uz.azizbek.maktabboshqaruv.repository.LessonSlotRepository;
import uz.azizbek.maktabboshqaruv.repository.PositionRepository;
import uz.azizbek.maktabboshqaruv.repository.RoomRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolClassRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import uz.azizbek.maktabboshqaruv.repository.SubjectRepository;
import uz.azizbek.maktabboshqaruv.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Populates the database with a large, internally-consistent set of demo
 * data (schools, buildings, rooms, staff, classes, students, weekly
 * timetable) so the admin panel has something realistic to look at.
 *
 * Runs once PER SCHOOL: for each school, if it already has classes tied to
 * its current academic year, that school's operational data (subjects,
 * employees, classes, students, timetable) is left untouched and only a
 * summary is logged. A school with no classes yet gets a full, independent
 * data set generated for it. This makes the seeder safe to leave enabled
 * permanently: adding a new school and restarting the app will seed just
 * that school, without touching or duplicating anything for schools that
 * already have data.
 */
@Component
@Order(2)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final List<String> WEEKDAYS =
            List.of("Dushanba", "Seshanba", "Chorshanba", "Payshanba", "Juma", "Shanba");

    private static final LocalTime[] PERIOD_START = {
            LocalTime.of(8, 30), LocalTime.of(9, 25), LocalTime.of(10, 20),
            LocalTime.of(11, 15), LocalTime.of(12, 40), LocalTime.of(13, 35)
    };
    private static final LocalTime[] PERIOD_END = {
            LocalTime.of(9, 15), LocalTime.of(10, 10), LocalTime.of(11, 5),
            LocalTime.of(12, 0), LocalTime.of(13, 25), LocalTime.of(14, 20)
    };

    private static final List<String> POSITION_TITLES = List.of(
            "Direktor", "O'quv ishlari bo'yicha direktor o'rinbosari",
            "Ma'naviy-ma'rifiy ishlar bo'yicha o'rinbosar", "Sinf rahbari",
            "Fan o'qituvchisi", "Psixolog", "Kutubxonachi", "Hisobchi", "Qorovul");

    private static final List<String> SUBJECT_NAMES = List.of(
            "Ona tili", "O'zbek adabiyoti", "Matematika", "Algebra", "Geometriya",
            "Fizika", "Kimyo", "Biologiya", "Geografiya", "O'zbekiston tarixi",
            "Jahon tarixi", "Ingliz tili", "Rus tili", "Informatika",
            "Jismoniy tarbiya", "Musiqa", "Tasviriy san'at", "Texnologiya", "Tarbiya");

    // grade 1-4 weekly subject load (must sum to 29 = 5*5 + 4)
    private static final Map<String, Integer> ELEMENTARY_LOAD = Map.ofEntries(
            Map.entry("Ona tili", 6), Map.entry("Matematika", 6), Map.entry("O'zbek adabiyoti", 3),
            Map.entry("Tarbiya", 2), Map.entry("Jismoniy tarbiya", 3), Map.entry("Musiqa", 2),
            Map.entry("Tasviriy san'at", 2), Map.entry("Texnologiya", 2), Map.entry("Ingliz tili", 3));
    private static final Set<String> ELEMENTARY_CORE =
            Set.of("Ona tili", "Matematika", "O'zbek adabiyoti", "Tarbiya");

    // grade 5-11 weekly subject load (must sum to 34 = 6*5 + 4)
    private static final Map<String, Integer> SECONDARY_LOAD = Map.ofEntries(
            Map.entry("Ona tili", 3), Map.entry("O'zbek adabiyoti", 2), Map.entry("Algebra", 2),
            Map.entry("Geometriya", 2), Map.entry("Fizika", 2), Map.entry("Kimyo", 2),
            Map.entry("Biologiya", 2), Map.entry("Geografiya", 2), Map.entry("O'zbekiston tarixi", 2),
            Map.entry("Jahon tarixi", 1), Map.entry("Ingliz tili", 4), Map.entry("Rus tili", 2),
            Map.entry("Informatika", 2), Map.entry("Jismoniy tarbiya", 2), Map.entry("Musiqa", 1),
            Map.entry("Tasviriy san'at", 1), Map.entry("Texnologiya", 1), Map.entry("Tarbiya", 1));

    // one teacher per subject, extra teacher for the busiest ones
    private static final Map<String, Integer> SECONDARY_TEACHERS_PER_SUBJECT = Map.ofEntries(
            Map.entry("Ona tili", 1), Map.entry("O'zbek adabiyoti", 1), Map.entry("Matematika", 2),
            Map.entry("Algebra", 1), Map.entry("Geometriya", 1), Map.entry("Fizika", 1),
            Map.entry("Kimyo", 1), Map.entry("Biologiya", 1), Map.entry("Geografiya", 1),
            Map.entry("O'zbekiston tarixi", 1), Map.entry("Jahon tarixi", 1), Map.entry("Ingliz tili", 2),
            Map.entry("Rus tili", 1), Map.entry("Informatika", 1), Map.entry("Jismoniy tarbiya", 1),
            Map.entry("Musiqa", 1), Map.entry("Tasviriy san'at", 1), Map.entry("Texnologiya", 1),
            Map.entry("Tarbiya", 1));
    private static final List<String> ELEMENTARY_SPECIALIST_SUBJECTS =
            List.of("Jismoniy tarbiya", "Musiqa", "Tasviriy san'at", "Texnologiya", "Ingliz tili");

    private static final String[] MALE_FIRST_NAMES = {
            "Aziz", "Alisher", "Bobur", "Davron", "Elyor", "Farrux", "G'ayrat", "Husan", "Ilhom",
            "Jahongir", "Kamol", "Laziz", "Muzaffar", "Nodir", "Olim", "Rustam", "Sardor", "Temur",
            "Ulug'bek", "Xurshid", "Yusuf", "Zafar", "Sherzod", "Shavkat", "Akmal", "Bekzod",
            "Dilshod", "Farhod", "Jasur", "Murod", "Sanjar", "Otabek", "Anvar", "Bahodir", "Damir"
    };
    private static final String[] FEMALE_FIRST_NAMES = {
            "Aziza", "Barno", "Dilnoza", "Ezoza", "Feruza", "Gulnora", "Hilola", "Iroda", "Jamila",
            "Kamola", "Laylo", "Madina", "Nargiza", "Nodira", "Ozoda", "Sabina", "Shahnoza", "Umida",
            "Zarina", "Zulfiya", "Dildora", "Gulbahor", "Kumush", "Malika", "Nigora", "Rayhona",
            "Sevara", "Shirin", "Yulduz", "Zebo", "Sarvinoz", "Mohira", "Gulchehra", "Dilorom", "Robiya"
    };
    private static final String[] SURNAME_STEMS = {
            "Karimov", "Yusupov", "Rashidov", "Abdullayev", "Nazarov", "Ergashev", "Tursunov",
            "Xolmatov", "Mirzayev", "Islomov", "Sodiqov", "Ochilov", "Saidov", "Ibragimov",
            "Yo'ldoshev", "Norqobilov", "Xasanov", "Qodirov", "Rustamov", "Boltayev", "Ahmedov",
            "Yoqubov", "Usmonov", "G'ulomov", "Ne'matov", "Umarov", "Xudoyberdiyev", "Choriyev",
            "Sultonov", "Rahimov", "Safarov", "Tojiboyev", "Eshonqulov", "Berdiyev"
    };

    private final SchoolRepository schoolRepository;
    private final BuildingRepository buildingRepository;
    private final RoomRepository roomRepository;
    private final AcademicYearRepository academicYearRepository;
    private final PositionRepository positionRepository;
    private final SubjectRepository subjectRepository;
    private final EmployeeRepository employeeRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StudentRepository studentRepository;
    private final LessonSlotRepository lessonSlotRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final Random random = new Random(20250928L);
    private int phoneSequence = 930_000_000;

    public DataSeeder(SchoolRepository schoolRepository, BuildingRepository buildingRepository,
                       RoomRepository roomRepository, AcademicYearRepository academicYearRepository,
                       PositionRepository positionRepository, SubjectRepository subjectRepository,
                       EmployeeRepository employeeRepository, SchoolClassRepository schoolClassRepository,
                       StudentRepository studentRepository, LessonSlotRepository lessonSlotRepository,
                       UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.schoolRepository = schoolRepository;
        this.buildingRepository = buildingRepository;
        this.roomRepository = roomRepository;
        this.academicYearRepository = academicYearRepository;
        this.positionRepository = positionRepository;
        this.subjectRepository = subjectRepository;
        this.employeeRepository = employeeRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.studentRepository = studentRepository;
        this.lessonSlotRepository = lessonSlotRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private static class SchoolInfra {
        Building mainBuilding;
        Building elemBuilding;
        Building sportBuilding;
        List<Room> generalRooms;
        Room computerRoom;
        List<Room> elemHomeRooms;
        List<Room> sportRooms;
        AcademicYear currentYear;
    }

    @Override
    @Transactional
    public void run(String... args) {
        School school1 = findOrCreateSchool("1-maktab", "Toshkent sh., Chilonzor tumani, Bunyodkor ko'chasi 12");
        School school2 = findOrCreateSchool("Toshkent shahar 110-maktab", "Toshkent sh., Yunusobod tumani, Amir Temur ko'chasi 45");

        // Older rows created before Subject/Employee became school-scoped have a null
        // school_id; attribute them to school1 (that's where all pre-existing timetable
        // data actually points) so every row ends up consistently owned by a school.
        migrateLegacySchoolLinks(school1);

        Map<String, Position> positions = findOrCreatePositions();

        SchoolInfra infra1 = setupInfra(school1);
        SchoolInfra infra2 = setupInfra(school2);

        log.info("DataSeeder: {} uchun ma'lumotlarni tekshirish...", school1.getName());
        String summary1 = seedSchoolOperationalData(school1, infra1, positions);
        log.info("DataSeeder: {} uchun ma'lumotlarni tekshirish...", school2.getName());
        String summary2 = seedSchoolOperationalData(school2, infra2, positions);

        List<String[]> createdUsers = seedUsers();

        log.info("=== DataSeeder yakunlandi ===");
        log.info("{}: {}", school1.getName(), summary1);
        log.info("{}: {}", school2.getName(), summary2);
        for (String[] u : createdUsers) {
            log.info("Login yaratildi: username={} password={} role={}", u[0], u[1], u[2]);
        }
    }

    private void migrateLegacySchoolLinks(School school1) {
        List<Subject> orphanSubjects = subjectRepository.findAll().stream()
                .filter(s -> s.getSchool() == null)
                .collect(Collectors.toList());
        if (!orphanSubjects.isEmpty()) {
            orphanSubjects.forEach(s -> s.setSchool(school1));
            subjectRepository.saveAll(orphanSubjects);
            log.info("Migratsiya: {} ta eski fan '{}' maktabga biriktirildi", orphanSubjects.size(), school1.getName());
        }

        List<Employee> orphanEmployees = employeeRepository.findAll().stream()
                .filter(e -> e.getSchool() == null)
                .collect(Collectors.toList());
        if (!orphanEmployees.isEmpty()) {
            orphanEmployees.forEach(e -> e.setSchool(school1));
            employeeRepository.saveAll(orphanEmployees);
            log.info("Migratsiya: {} ta eski xodim '{}' maktabga biriktirildi", orphanEmployees.size(), school1.getName());
        }
    }

    // ---------- schools / buildings / rooms ----------

    private School findOrCreateSchool(String name, String address) {
        return schoolRepository.findAll().stream()
                .filter(s -> s.getName().equals(name))
                .findFirst()
                .orElseGet(() -> {
                    School s = new School();
                    s.setName(name);
                    s.setAddress(address);
                    return schoolRepository.save(s);
                });
    }

    private Building findOrCreateBuilding(School school, String name) {
        return buildingRepository.findAll().stream()
                .filter(b -> b.getSchool().getId().equals(school.getId()) && b.getName().equals(name))
                .findFirst()
                .orElseGet(() -> {
                    Building b = new Building();
                    b.setSchool(school);
                    b.setName(name);
                    return buildingRepository.save(b);
                });
    }

    private Room findOrCreateRoom(Building building, String roomNumber, int capacity) {
        return roomRepository.findAll().stream()
                .filter(r -> r.getBuilding().getId().equals(building.getId()) && r.getRoomNumber().equals(roomNumber))
                .findFirst()
                .orElseGet(() -> {
                    Room r = new Room();
                    r.setBuilding(building);
                    r.setRoomNumber(roomNumber);
                    r.setCapacity(capacity);
                    r.setType(inferRoomType(roomNumber));
                    r.setFloor(inferFloor(roomNumber));
                    return roomRepository.save(r);
                });
    }

    static uz.azizbek.maktabboshqaruv.entity.RoomType inferRoomType(String roomNumber) {
        String n = roomNumber.toLowerCase();
        if (n.contains("kompyuter")) return uz.azizbek.maktabboshqaruv.entity.RoomType.COMPUTER_LAB;
        if (n.contains("kutubxona")) return uz.azizbek.maktabboshqaruv.entity.RoomType.LIBRARY;
        if (n.contains("sport")) return uz.azizbek.maktabboshqaruv.entity.RoomType.GYM;
        if (n.contains("laborat")) return uz.azizbek.maktabboshqaruv.entity.RoomType.LAB;
        return uz.azizbek.maktabboshqaruv.entity.RoomType.CLASSROOM;
    }

    static Integer inferFloor(String roomNumber) {
        if (roomNumber.matches("\\d{3}")) {
            return Character.getNumericValue(roomNumber.charAt(0));
        }
        return 1;
    }

    private List<Room> findOrCreateRooms(Building building, List<String> numbers, int capacity) {
        List<Room> result = new ArrayList<>();
        for (String n : numbers) {
            result.add(findOrCreateRoom(building, n, capacity));
        }
        return result;
    }

    private AcademicYear findOrCreateAcademicYear(School school, String title, LocalDate start, LocalDate end) {
        return academicYearRepository.findAll().stream()
                .filter(a -> a.getSchool().getId().equals(school.getId()) && a.getTitle().equals(title))
                .findFirst()
                .orElseGet(() -> {
                    AcademicYear a = new AcademicYear();
                    a.setSchool(school);
                    a.setTitle(title);
                    a.setStartDate(start);
                    a.setEndDate(end);
                    return academicYearRepository.save(a);
                });
    }

    private SchoolInfra setupInfra(School school) {
        SchoolInfra infra = new SchoolInfra();
        infra.mainBuilding = findOrCreateBuilding(school, "Asosiy bino");
        infra.elemBuilding = findOrCreateBuilding(school, "Boshlang'ich sinflar binosi");
        infra.sportBuilding = findOrCreateBuilding(school, "Sport majmuasi");

        infra.generalRooms = findOrCreateRooms(infra.mainBuilding,
                List.of("101", "102", "103", "104", "105", "106", "107", "108", "109", "110"), 32);
        infra.computerRoom = findOrCreateRoom(infra.mainBuilding, "Kompyuter xonasi", 24);
        findOrCreateRoom(infra.mainBuilding, "Kutubxona", 40);

        infra.elemHomeRooms = findOrCreateRooms(infra.elemBuilding,
                List.of("201", "202", "203", "204", "205", "206", "207", "208"), 28);
        findOrCreateRoom(infra.elemBuilding, "Musiqa xonasi", 25);
        findOrCreateRoom(infra.elemBuilding, "Tasviriy san'at xonasi", 25);

        infra.sportRooms = findOrCreateRooms(infra.sportBuilding,
                List.of("Katta sport zali", "Kichik sport zali"), 50);

        findOrCreateAcademicYear(school, "2024-2025", LocalDate.of(2024, 9, 2), LocalDate.of(2025, 5, 30));
        infra.currentYear = findOrCreateAcademicYear(school, "2025-2026",
                LocalDate.of(2025, 9, 1), LocalDate.of(2026, 5, 29));

        return infra;
    }

    // ---------- positions / subjects ----------

    private Map<String, Position> findOrCreatePositions() {
        Map<String, Position> existing = positionRepository.findAll().stream()
                .collect(Collectors.toMap(Position::getTitle, p -> p, (a, b) -> a));
        Map<String, Position> result = new HashMap<>(existing);
        for (String title : POSITION_TITLES) {
            result.computeIfAbsent(title, t -> {
                Position p = new Position();
                p.setTitle(t);
                return positionRepository.save(p);
            });
        }
        return result;
    }

    private Map<String, Subject> findOrCreateSubjectsForSchool(School school) {
        Map<String, Subject> existing = subjectRepository.findBySchoolId(school.getId()).stream()
                .collect(Collectors.toMap(Subject::getName, s -> s, (a, b) -> a));
        Map<String, Subject> result = new HashMap<>(existing);
        for (String name : SUBJECT_NAMES) {
            result.computeIfAbsent(name, n -> {
                Subject s = new Subject();
                s.setSchool(school);
                s.setName(n);
                return subjectRepository.save(s);
            });
        }
        return result;
    }

    // ---------- employees ----------

    private String nextPhone() {
        phoneSequence++;
        String phone = "+998" + phoneSequence;
        while (employeeRepository.existsByPhone(phone)) {
            phoneSequence++;
            phone = "+998" + phoneSequence;
        }
        return phone;
    }

    private Employee createEmployee(School school, String firstName, String lastName, Position position) {
        Employee e = new Employee();
        e.setSchool(school);
        e.setFirstName(firstName);
        e.setLastName(lastName);
        e.setPosition(position);
        e.setPhone(nextPhone());
        return employeeRepository.save(e);
    }

    private String[] randomName(boolean male) {
        String first = male
                ? MALE_FIRST_NAMES[random.nextInt(MALE_FIRST_NAMES.length)]
                : FEMALE_FIRST_NAMES[random.nextInt(FEMALE_FIRST_NAMES.length)];
        String stem = SURNAME_STEMS[random.nextInt(SURNAME_STEMS.length)];
        String last = male ? stem : stem + "a";
        return new String[]{first, last};
    }

    private List<Employee> seedLeadershipEmployees(School school, Map<String, Position> positions) {
        // Idempotent per (school, position): reuse whatever already exists for a title and
        // only create what's missing, instead of skipping all leadership seeding just because
        // someone already exists.
        Map<String, List<Employee>> byPosition = employeeRepository.findBySchoolId(school.getId()).stream()
                .collect(Collectors.groupingBy(e -> e.getPosition().getTitle()));

        List<Employee> directors = ensureAtLeastOne(school, byPosition, "Direktor", positions, true);
        ensureAtLeastOne(school, byPosition, "O'quv ishlari bo'yicha direktor o'rinbosari", positions, random.nextBoolean());
        ensureAtLeastOne(school, byPosition, "Ma'naviy-ma'rifiy ishlar bo'yicha o'rinbosar", positions, random.nextBoolean());
        ensureAtLeastOne(school, byPosition, "Psixolog", positions, false);
        ensureAtLeastOne(school, byPosition, "Kutubxonachi", positions, false);
        ensureAtLeastOne(school, byPosition, "Hisobchi", positions, false);
        int existingGuards = byPosition.getOrDefault("Qorovul", List.of()).size();
        for (int i = existingGuards; i < 2; i++) {
            String[] guard = randomName(true);
            createEmployee(school, guard[0], guard[1], positions.get("Qorovul"));
        }
        return directors;
    }

    private List<Employee> ensureAtLeastOne(School school, Map<String, List<Employee>> byPosition, String positionTitle,
                                             Map<String, Position> positions, boolean male) {
        List<Employee> existing = byPosition.get(positionTitle);
        if (existing != null && !existing.isEmpty()) {
            return existing;
        }
        String[] name = randomName(male);
        Employee created = createEmployee(school, name[0], name[1], positions.get(positionTitle));
        return List.of(created);
    }

    private List<Employee> seedHomeroomTeachers(School school, Map<String, Position> positions) {
        List<Employee> result = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            String[] name = randomName(random.nextBoolean());
            result.add(createEmployee(school, name[0], name[1], positions.get("Sinf rahbari")));
        }
        return result;
    }

    private Map<String, List<Employee>> seedSecondaryTeachers(School school, Map<String, Position> positions) {
        Map<String, List<Employee>> result = new HashMap<>();
        Position teacherPosition = positions.get("Fan o'qituvchisi");
        for (Map.Entry<String, Integer> entry : SECONDARY_TEACHERS_PER_SUBJECT.entrySet()) {
            List<Employee> teachers = new ArrayList<>();
            for (int i = 0; i < entry.getValue(); i++) {
                String[] name = randomName(random.nextBoolean());
                teachers.add(createEmployee(school, name[0], name[1], teacherPosition));
            }
            result.put(entry.getKey(), teachers);
        }
        return result;
    }

    // ---------- per-school orchestration ----------

    private String seedSchoolOperationalData(School school, SchoolInfra infra, Map<String, Position> positions) {
        Map<String, Subject> subjects = findOrCreateSubjectsForSchool(school);

        if (schoolClassRepository.countByAcademicYearSchoolId(school.getId()) > 0) {
            long classCount = schoolClassRepository.countByAcademicYearSchoolId(school.getId());
            long studentCount = studentRepository.countBySchoolClassAcademicYearSchoolId(school.getId());
            long lessonCount = lessonSlotRepository.countBySchoolClassAcademicYearSchoolId(school.getId());
            long employeeCount = employeeRepository.countBySchoolId(school.getId());
            log.info("DataSeeder: {} uchun sinflar allaqachon mavjud, operatsion qism o'tkazib yuborildi", school.getName());
            return String.format(
                    "allaqachon mavjud (fanlar=%d, xodimlar=%d, sinflar=%d, o'quvchilar=%d, dars jadvali=%d)",
                    subjects.size(), employeeCount, classCount, studentCount, lessonCount);
        }

        List<Employee> leadership = seedLeadershipEmployees(school, positions);
        List<Employee> homeroomTeachers = seedHomeroomTeachers(school, positions);
        Map<String, List<Employee>> secondaryTeachers = seedSecondaryTeachers(school, positions);

        List<SchoolClass> elementaryClasses = new ArrayList<>();
        List<SchoolClass> secondaryClasses = new ArrayList<>();
        List<SchoolClass> allClasses = seedClasses(infra.currentYear, elementaryClasses, secondaryClasses);

        int totalStudents = seedStudents(elementaryClasses, secondaryClasses);

        Map<Integer, Room> elemClassRoom = new HashMap<>();
        Map<Integer, Employee> elemClassTeacher = new HashMap<>();
        for (int i = 0; i < elementaryClasses.size(); i++) {
            elemClassRoom.put(i, infra.elemHomeRooms.get(i % infra.elemHomeRooms.size()));
            elemClassTeacher.put(i, homeroomTeachers.get(i % homeroomTeachers.size()));
        }

        Map<String, List<Employee>> elemSpecialists = new HashMap<>();
        for (String subj : ELEMENTARY_SPECIALIST_SUBJECTS) {
            elemSpecialists.put(subj, secondaryTeachers.getOrDefault(subj, List.of()));
        }

        int lessonCount = seedElementaryLessons(elementaryClasses, elemClassRoom, elemClassTeacher,
                elemSpecialists, infra.sportRooms, subjects);
        lessonCount += seedSecondaryLessons(secondaryClasses, secondaryTeachers,
                infra.generalRooms, infra.computerRoom, infra.sportRooms, subjects);

        log.info("DataSeeder: {} uchun yaratildi - lavozimlar={}, fanlar={}, xodimlar={}, sinflar={}, o'quvchilar={}, dars jadvali={}",
                school.getName(), positions.size(), subjects.size(), employeeRepository.countBySchoolId(school.getId()),
                allClasses.size(), totalStudents, lessonCount);

        return String.format(
                "yaratildi (fanlar=%d, xodimlar=%d, sinflar=%d [boshlang'ich=%d, katta=%d], o'quvchilar=%d, dars jadvali=%d, rahbariyat=%d)",
                subjects.size(), employeeRepository.countBySchoolId(school.getId()), allClasses.size(),
                elementaryClasses.size(), secondaryClasses.size(), totalStudents, lessonCount, leadership.size());
    }

    // ---------- classes / students ----------

    private List<SchoolClass> seedClasses(AcademicYear year, List<SchoolClass> elementaryOut, List<SchoolClass> secondaryOut) {
        List<SchoolClass> all = new ArrayList<>();
        for (int grade = 1; grade <= 11; grade++) {
            for (String section : List.of("A", "B")) {
                SchoolClass cls = new SchoolClass();
                cls.setAcademicYear(year);
                cls.setGradeNumber(grade);
                cls.setSectionLetter(section);
                cls.setMaxStudents(30);
                cls = schoolClassRepository.save(cls);
                all.add(cls);
                if (grade <= 4) {
                    elementaryOut.add(cls);
                } else {
                    secondaryOut.add(cls);
                }
            }
        }
        return all;
    }

    private int seedStudents(List<SchoolClass> elementaryClasses, List<SchoolClass> secondaryClasses) {
        int count = 0;
        List<SchoolClass> all = new ArrayList<>(elementaryClasses);
        all.addAll(secondaryClasses);
        int currentYear = LocalDate.now().getYear();
        List<Student> batch = new ArrayList<>();
        for (SchoolClass cls : all) {
            int studentCount = 20 + random.nextInt(8); // 20-27
            for (int i = 0; i < studentCount; i++) {
                boolean male = random.nextBoolean();
                String[] name = randomName(male);
                Student student = new Student();
                student.setSchoolClass(cls);
                student.setFirstName(name[0]);
                student.setLastName(name[1]);
                int age = 6 + cls.getGradeNumber();
                int birthYear = currentYear - age;
                int month = 1 + random.nextInt(12);
                int day = 1 + random.nextInt(28);
                student.setBirthDate(LocalDate.of(birthYear, month, day));
                batch.add(student);
                count++;
            }
        }
        studentRepository.saveAll(batch);
        return count;
    }

    // ---------- timetable ----------

    private List<String> buildWeekDeck(Map<String, Integer> load, int totalPeriods) {
        List<String> deck = new ArrayList<>(totalPeriods);
        for (Map.Entry<String, Integer> e : load.entrySet()) {
            for (int i = 0; i < e.getValue(); i++) {
                deck.add(e.getKey());
            }
        }
        java.util.Collections.shuffle(deck, random);
        return deck;
    }

    private int[] periodsForDay(boolean isElementary, int dayIndex) {
        boolean saturday = dayIndex == 5;
        int periods = saturday ? 4 : (isElementary ? 5 : 6);
        int[] result = new int[periods];
        for (int i = 0; i < periods; i++) result[i] = i;
        return result;
    }

    private int seedElementaryLessons(List<SchoolClass> classes, Map<Integer, Room> classRoom,
                                       Map<Integer, Employee> classTeacher,
                                       Map<String, List<Employee>> specialists, List<Room> sportRooms,
                                       Map<String, Subject> subjects) {
        Set<String> employeeBusy = new HashSet<>();
        Set<String> roomBusy = new HashSet<>();
        List<LessonSlot> batch = new ArrayList<>();

        for (int ci = 0; ci < classes.size(); ci++) {
            SchoolClass cls = classes.get(ci);
            Room home = classRoom.get(ci);
            Employee homeroom = classTeacher.get(ci);
            List<String> deck = buildWeekDeck(ELEMENTARY_LOAD, 29);
            int deckIndex = 0;

            for (int day = 0; day < WEEKDAYS.size(); day++) {
                String weekday = WEEKDAYS.get(day);
                int[] periods = periodsForDay(true, day);
                for (int period : periods) {
                    if (deckIndex >= deck.size()) break;
                    String subject = deck.get(deckIndex++);
                    LocalTime start = PERIOD_START[period];
                    LocalTime end = PERIOD_END[period];

                    Employee teacher;
                    Room room;
                    if (ELEMENTARY_CORE.contains(subject)) {
                        teacher = homeroom;
                        room = home;
                    } else if (subject.equals("Jismoniy tarbiya")) {
                        teacher = pickFreeTeacher(specialists.get(subject), weekday, period, employeeBusy);
                        room = pickFreeRoom(sportRooms, weekday, period, roomBusy);
                    } else {
                        teacher = pickFreeTeacher(specialists.get(subject), weekday, period, employeeBusy);
                        room = home; // specialists come to the class's own room
                    }

                    if (teacher == null || room == null) {
                        continue; // no free teacher/room this slot - leave a gap
                    }

                    employeeBusy.add(teacher.getId() + "|" + weekday + "|" + period);
                    roomBusy.add(room.getId() + "|" + weekday + "|" + period);

                    batch.add(buildLessonSlot(cls, subject, teacher, room, weekday, start, end, subjects));
                }
            }
        }
        lessonSlotRepository.saveAll(batch);
        return batch.size();
    }

    private int seedSecondaryLessons(List<SchoolClass> classes, Map<String, List<Employee>> teachers,
                                      List<Room> generalRooms, Room computerRoom, List<Room> sportRooms,
                                      Map<String, Subject> subjects) {
        Set<String> employeeBusy = new HashSet<>();
        Set<String> roomBusy = new HashSet<>();
        List<LessonSlot> batch = new ArrayList<>();

        for (SchoolClass cls : classes) {
            List<String> deck = buildWeekDeck(SECONDARY_LOAD, 34);
            int deckIndex = 0;

            for (int day = 0; day < WEEKDAYS.size(); day++) {
                String weekday = WEEKDAYS.get(day);
                int[] periods = periodsForDay(false, day);
                for (int period : periods) {
                    if (deckIndex >= deck.size()) break;
                    String subject = deck.get(deckIndex++);
                    LocalTime start = PERIOD_START[period];
                    LocalTime end = PERIOD_END[period];

                    Employee teacher = pickFreeTeacher(teachers.get(subject), weekday, period, employeeBusy);
                    if (teacher == null) continue;

                    Room room;
                    if (subject.equals("Jismoniy tarbiya")) {
                        room = pickFreeRoom(sportRooms, weekday, period, roomBusy);
                    } else if (subject.equals("Informatika")) {
                        room = roomBusy.contains(computerRoom.getId() + "|" + weekday + "|" + period)
                                ? null : computerRoom;
                    } else {
                        room = pickFreeRoom(generalRooms, weekday, period, roomBusy);
                    }
                    if (room == null) continue;

                    employeeBusy.add(teacher.getId() + "|" + weekday + "|" + period);
                    roomBusy.add(room.getId() + "|" + weekday + "|" + period);

                    batch.add(buildLessonSlot(cls, subject, teacher, room, weekday, start, end, subjects));
                }
            }
        }
        lessonSlotRepository.saveAll(batch);
        return batch.size();
    }

    private Employee pickFreeTeacher(List<Employee> candidates, String weekday, int period, Set<String> busy) {
        if (candidates == null) return null;
        for (Employee e : candidates) {
            if (!busy.contains(e.getId() + "|" + weekday + "|" + period)) {
                return e;
            }
        }
        return null;
    }

    private Room pickFreeRoom(List<Room> candidates, String weekday, int period, Set<String> busy) {
        for (Room r : candidates) {
            if (!busy.contains(r.getId() + "|" + weekday + "|" + period)) {
                return r;
            }
        }
        return null;
    }

    private LessonSlot buildLessonSlot(SchoolClass cls, String subjectName, Employee teacher, Room room,
                                        String weekday, LocalTime start, LocalTime end,
                                        Map<String, Subject> subjects) {
        LessonSlot slot = new LessonSlot();
        slot.setSchoolClass(cls);
        Subject subject = subjects.get(subjectName);
        if (subject == null) {
            throw new IllegalStateException("Fan topilmadi: " + subjectName);
        }
        slot.setSubject(subject);
        slot.setEmployee(teacher);
        slot.setRoom(room);
        slot.setWeekday(weekday);
        slot.setStartTime(start);
        slot.setEndTime(end);
        return slot;
    }

    // ---------- login accounts ----------

    private List<String[]> seedUsers() {
        List<String[]> created = new ArrayList<>();
        created.add(createUserIfAbsent("direktor", "direktor123", Role.EDITOR));
        created.add(createUserIfAbsent("ustoz1", "ustoz123", Role.VIEWER));
        created.add(createUserIfAbsent("ustoz2", "ustoz123", Role.VIEWER));
        return created.stream().filter(u -> u != null).collect(Collectors.toList());
    }

    private String[] createUserIfAbsent(String username, String rawPassword, Role role) {
        if (userRepository.existsByUsername(username)) {
            return null;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        userRepository.save(user);
        return new String[]{username, rawPassword, role.name()};
    }
}
