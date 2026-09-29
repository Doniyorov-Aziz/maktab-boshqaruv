package uz.azizbek.maktabboshqaruv.config;

import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Seeds the operational modules (attendance, grades, announcements, calendar
 * events, behavior records) on top of whatever DataSeeder already created:
 * schools, buildings, classes, students, employees and the weekly timetable.
 *
 * Runs once per school: if a school already has attendance rows, its
 * operational data is left untouched. A freshly added school with classes
 * but no attendance yet gets a full 60-day history generated for it.
 */
@Component
@Order(3)
public class OperationalDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(OperationalDataSeeder.class);

    private static final Map<DayOfWeek, String> WEEKDAY_NAMES = Map.of(
            DayOfWeek.MONDAY, "Dushanba", DayOfWeek.TUESDAY, "Seshanba", DayOfWeek.WEDNESDAY, "Chorshanba",
            DayOfWeek.THURSDAY, "Payshanba", DayOfWeek.FRIDAY, "Juma", DayOfWeek.SATURDAY, "Shanba");

    private static final String[] ANNOUNCEMENT_TITLES = {
            "O'quv yili boshlanishi haqida", "Ota-onalar yig'ilishi bo'lib o'tadi",
            "Maktab formasi talablari", "Sog'liqni saqlash bo'yicha tekshiruv",
            "Fanlar olimpiadasi ishtirokchilari", "Ta'til jadvali e'lon qilindi",
            "Kutubxona yangi kitoblari", "Sport musobaqalariga ro'yxatdan o'tish",
            "Maktab oshxonasi menyusi yangilandi", "Yong'in xavfsizligi mashqi",
            "Elektron jurnal bo'yicha ko'rsatma", "Iqtidorli o'quvchilar uchun to'garaklar"
    };

    private static final String[] ANNOUNCEMENT_BODIES = {
            "Hurmatli ota-onalar va o'quvchilar, ushbu masala yuzasidan barchani e'tiborli bo'lishga chaqiramiz. Batafsil ma'lumot uchun sinf rahbariga murojaat qiling.",
            "Iltimos, belgilangan muddatgacha barcha kerakli hujjatlarni maktab boshqaruviga topshiring. Kechikish holatlari alohida ko'rib chiqiladi.",
            "Tadbir maktabning asosiy binosida bo'lib o'tadi. Barcha ishtirokchilardan vaqtida kelish so'raladi.",
            "Ushbu masala bo'yicha qo'shimcha savollar uchun maktab qabulxonasiga murojaat qilishingiz mumkin."
    };

    private static final String[] BEHAVIOR_REWARD_DESCRIPTIONS = {
            "Fan olimpiadasida faol ishtirok etgani uchun", "Sinf tozaligiga hissa qo'shgani uchun",
            "Chorak davomida a'lo baholarga o'qigani uchun", "Sinfdoshlariga yordam bergani uchun",
            "Maktab tadbirida faol ishtirok etgani uchun", "Intizomli va tartibli xulqi uchun"
    };
    private static final String[] BEHAVIOR_WARNING_DESCRIPTIONS = {
            "Darsga kechikib kelgani uchun", "Uy vazifasini bajarmagani uchun",
            "Sinfda intizomni buzgani uchun", "Maktab formasiga rioya qilmagani uchun",
            "Darsda diqqatini jamlay olmagani uchun"
    };

    private static final String[] CALENDAR_TITLES_HOLIDAY = {"Mustaqillik kuni", "Navro'z bayrami", "Xotira va qadrlash kuni"};
    private static final String[] CALENDAR_TITLES_EXAM = {"Chorak yakuniy nazorat ishlari", "Oraliq baholash", "Fanlar bo'yicha test sinovi"};

    private final SchoolRepository schoolRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StudentRepository studentRepository;
    private final LessonSlotRepository lessonSlotRepository;
    private final AttendanceRepository attendanceRepository;
    private final GradeRepository gradeRepository;
    private final AnnouncementRepository announcementRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final BehaviorRecordRepository behaviorRecordRepository;
    private final UserRepository userRepository;
    private final ActivityLogRepository activityLogRepository;

    private final Random random = new Random(20260101L);

    public OperationalDataSeeder(SchoolRepository schoolRepository, SchoolClassRepository schoolClassRepository,
                                  StudentRepository studentRepository, LessonSlotRepository lessonSlotRepository,
                                  AttendanceRepository attendanceRepository, GradeRepository gradeRepository,
                                  AnnouncementRepository announcementRepository, CalendarEventRepository calendarEventRepository,
                                  BehaviorRecordRepository behaviorRecordRepository, UserRepository userRepository,
                                  ActivityLogRepository activityLogRepository) {
        this.schoolRepository = schoolRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.studentRepository = studentRepository;
        this.lessonSlotRepository = lessonSlotRepository;
        this.attendanceRepository = attendanceRepository;
        this.gradeRepository = gradeRepository;
        this.announcementRepository = announcementRepository;
        this.calendarEventRepository = calendarEventRepository;
        this.behaviorRecordRepository = behaviorRecordRepository;
        this.userRepository = userRepository;
        this.activityLogRepository = activityLogRepository;
    }

    private static final String[] GUARDIAN_MALE_FIRST_NAMES = {
            "Bahodir", "Farhod", "Jasur", "Sherzod", "Akmal", "Rustam", "Anvar", "Otabek", "Ulug'bek", "Damir"
    };
    private static final String[] GUARDIAN_FEMALE_FIRST_NAMES = {
            "Gulnora", "Nodira", "Sabina", "Malika", "Zulfiya", "Dildora", "Nigora", "Shirin", "Mohira", "Robiya"
    };
    private static final String[] GUARDIAN_SURNAME_STEMS = {
            "Karimov", "Yusupov", "Rashidov", "Abdullayev", "Nazarov", "Ergashev", "Tursunov", "Xolmatov",
            "Mirzayev", "Islomov", "Sodiqov", "Ochilov", "Saidov", "Ibragimov"
    };

    @Override
    @Transactional
    public void run(String... args) {
        upgradeTeacherAccountRoles();
        backfillGuardianInfo();

        for (School school : schoolRepository.findAll()) {
            List<SchoolClass> classes = schoolClassRepository.findByAcademicYearSchoolId(school.getId());
            if (classes.isEmpty()) {
                continue;
            }

            assignClassTeachers(classes);
            seedActivityLog(school, classes);

            if (attendanceRepository.countBySchoolId(school.getId()) > 0) {
                log.info("OperationalDataSeeder: {} uchun operatsion ma'lumotlar allaqachon mavjud, o'tkazib yuborildi", school.getName());
                continue;
            }

            log.info("OperationalDataSeeder: {} uchun davomat, baholar, e'lonlar, tadbirlar va xulq yozuvlari yaratilmoqda...", school.getName());

            int[] counts = seedAttendanceAndGrades(classes);
            int behaviorCount = seedBehaviorRecords(classes);
            int announcementCount = seedAnnouncements(school, classes);
            int eventCount = seedCalendarEvents(school);

            log.info("OperationalDataSeeder: {} - davomat={}, baholar={}, xulq={}, e'lonlar={}, tadbirlar={}",
                    school.getName(), counts[0], counts[1], behaviorCount, announcementCount, eventCount);
        }
    }

    private void upgradeTeacherAccountRoles() {
        for (String username : List.of("ustoz1", "ustoz2")) {
            userRepository.findByUsername(username).ifPresent(user -> {
                if (user.getRole() != Role.EDITOR) {
                    user.setRole(Role.EDITOR);
                    userRepository.save(user);
                    log.info("Migratsiya: {} hisobi EDITOR rolига ko'tarildi (davomat/baho kiritish uchun)", username);
                }
            });
        }
    }

    private int guardianPhoneSequence = 970_000_000;

    private void backfillGuardianInfo() {
        List<Student> missing = studentRepository.findAll().stream()
                .filter(s -> s.getGuardianName() == null)
                .collect(Collectors.toList());
        if (missing.isEmpty()) {
            return;
        }
        for (Student s : missing) {
            boolean male = random.nextBoolean();
            String first = male
                    ? GUARDIAN_MALE_FIRST_NAMES[random.nextInt(GUARDIAN_MALE_FIRST_NAMES.length)]
                    : GUARDIAN_FEMALE_FIRST_NAMES[random.nextInt(GUARDIAN_FEMALE_FIRST_NAMES.length)];
            String stem = GUARDIAN_SURNAME_STEMS[random.nextInt(GUARDIAN_SURNAME_STEMS.length)];
            String last = male ? stem : stem + "a";
            s.setGuardianName(first + " " + last);
            guardianPhoneSequence++;
            s.setGuardianPhone("+998" + guardianPhoneSequence);
        }
        studentRepository.saveAll(missing);
        log.info("Migratsiya: {} ta o'quvchi uchun ota-ona kontakt ma'lumotlari to'ldirildi", missing.size());
    }

    private void assignClassTeachers(List<SchoolClass> classes) {
        for (SchoolClass cls : classes) {
            if (cls.getClassTeacher() != null) {
                continue;
            }
            List<LessonSlot> lessons = lessonSlotRepository.findBySchoolClassId(cls.getId());
            Employee teacher = lessons.stream()
                    .filter(l -> l.getSubject().getName().equals("Ona tili"))
                    .map(LessonSlot::getEmployee)
                    .findFirst()
                    .orElseGet(() -> lessons.isEmpty() ? null : lessons.get(0).getEmployee());
            if (teacher != null) {
                cls.setClassTeacher(teacher);
                schoolClassRepository.save(cls);
            }
        }
    }

    // ---------- activity log ----------

    private static final String[] ACTIVITY_ACTORS = {"admin", "ustoz1", "ustoz2"};

    private void seedActivityLog(School school, List<SchoolClass> classes) {
        if (activityLogRepository.countBySchoolId(school.getId()) > 0) {
            return;
        }
        List<Student> allStudents = new ArrayList<>();
        for (SchoolClass cls : classes) {
            allStudents.addAll(studentRepository.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(cls.getId()));
        }
        if (allStudents.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<ActivityLog> batch = new ArrayList<>();
        int attempts = 0;
        while (batch.size() < 26 && attempts < 80) {
            attempts++;
            ActivityLog entry = buildRandomActivityEntry(school, classes, allStudents);
            if (entry == null) continue;
            LocalDateTime ts = now.minusDays(random.nextInt(9))
                    .withHour(8 + random.nextInt(9)).withMinute(random.nextInt(60)).withSecond(0).withNano(0);
            if (ts.isAfter(now)) ts = now.minusMinutes(random.nextInt(180));
            entry.setOccurredAt(ts);
            entry.setActorUsername(ACTIVITY_ACTORS[random.nextInt(ACTIVITY_ACTORS.length)]);
            batch.add(entry);
        }
        activityLogRepository.saveAll(batch);
    }

    private ActivityLog buildRandomActivityEntry(School school, List<SchoolClass> classes, List<Student> allStudents) {
        ActivityLog log = new ActivityLog();
        log.setSchool(school);
        int kind = random.nextInt(5);
        switch (kind) {
            case 0 -> {
                SchoolClass cls = classes.get(random.nextInt(classes.size()));
                List<Subject> subjects = classSubjects(cls);
                if (subjects.isEmpty()) return null;
                Subject subject = subjects.get(random.nextInt(subjects.size()));
                log.setIcon("fact_check");
                log.setDescription(cls.getGradeNumber() + "-" + cls.getSectionLetter()
                        + " sinfida " + subject.getName() + " darsidan davomat olindi");
            }
            case 1 -> {
                Student s = allStudents.get(random.nextInt(allStudents.size()));
                List<Subject> subjects = classSubjects(s.getSchoolClass());
                if (subjects.isEmpty()) return null;
                Subject subject = subjects.get(random.nextInt(subjects.size()));
                int score = 2 + random.nextInt(4);
                log.setIcon("grade");
                log.setDescription(s.getFirstName() + " " + s.getLastName() + "ga " + subject.getName()
                        + " fanidan " + score + " baho qo'yildi");
            }
            case 2 -> {
                Student s = allStudents.get(random.nextInt(allStudents.size()));
                log.setIcon("person_add");
                log.setDescription(s.getFirstName() + " " + s.getLastName() + " "
                        + s.getSchoolClass().getGradeNumber() + "-" + s.getSchoolClass().getSectionLetter()
                        + " sinfiga qo'shildi");
            }
            case 3 -> {
                String title = ANNOUNCEMENT_TITLES[random.nextInt(ANNOUNCEMENT_TITLES.length)];
                log.setIcon("campaign");
                log.setDescription("\"" + title + "\" e'loni joylandi");
            }
            default -> {
                Student s = allStudents.get(random.nextInt(allStudents.size()));
                boolean reward = random.nextDouble() < 0.6;
                String[] pool = reward ? BEHAVIOR_REWARD_DESCRIPTIONS : BEHAVIOR_WARNING_DESCRIPTIONS;
                log.setIcon(reward ? "star" : "warning");
                log.setDescription(s.getFirstName() + " " + s.getLastName() + " - "
                        + pool[random.nextInt(pool.length)]);
            }
        }
        return log;
    }

    private List<Subject> classSubjects(SchoolClass cls) {
        return lessonSlotRepository.findBySchoolClassId(cls.getId()).stream()
                .map(LessonSlot::getSubject).distinct().collect(Collectors.toList());
    }

    // ---------- attendance + grades ----------

    private int[] seedAttendanceAndGrades(List<SchoolClass> classes) {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(59);
        LocalTime now = LocalTime.now();

        int attendanceCount = 0;
        int gradeCount = 0;

        // pick a few students per class-group to deliberately look struggling / chronically absent
        List<Student> allStudents = new ArrayList<>();
        for (SchoolClass cls : classes) {
            allStudents.addAll(studentRepository.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(cls.getId()));
        }
        Set<Long> lowGradeStudentIds = pickRandomSubset(allStudents, 3);
        Set<Long> chronicAbsenceStudentIds = pickRandomSubset(allStudents, 3, lowGradeStudentIds);

        for (SchoolClass cls : classes) {
            List<LessonSlot> classLessons = lessonSlotRepository.findBySchoolClassId(cls.getId());
            Map<String, LessonSlot> firstLessonByWeekday = new HashMap<>();
            for (LessonSlot ls : classLessons) {
                firstLessonByWeekday.merge(ls.getWeekday(), ls,
                        (a, b) -> a.getStartTime().isBefore(b.getStartTime()) ? a : b);
            }
            List<Subject> classSubjects = classLessons.stream()
                    .map(LessonSlot::getSubject).distinct().collect(Collectors.toList());
            if (classSubjects.isEmpty()) {
                continue;
            }

            List<Student> students = studentRepository.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(cls.getId());

            // --- attendance: once-daily (first lesson of the day) per class ---
            List<Attendance> attendanceBatch = new ArrayList<>();
            for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {
                String weekday = WEEKDAY_NAMES.get(d.getDayOfWeek());
                if (weekday == null) continue;
                LessonSlot lesson = firstLessonByWeekday.get(weekday);
                if (lesson == null) continue;
                if (d.equals(today) && lesson.getStartTime().isAfter(now)) continue;

                if (d.equals(today) && random.nextDouble() < 0.15) continue;
                if (d.equals(today.minusDays(1)) && random.nextDouble() < 0.05) continue;

                for (Student s : students) {
                    AttendanceStatus status = rollAttendanceStatus(chronicAbsenceStudentIds.contains(s.getId()), d, today);
                    Attendance a = new Attendance();
                    a.setLessonSlot(lesson);
                    a.setStudent(s);
                    a.setRecordDate(d);
                    a.setStatus(status);
                    attendanceBatch.add(a);
                }
            }
            attendanceRepository.saveAll(attendanceBatch);
            attendanceCount += attendanceBatch.size();

            // --- grades: ~2-3 events per student per week across the class's subjects ---
            List<Grade> gradeBatch = new ArrayList<>();
            for (Student s : students) {
                boolean struggling = lowGradeStudentIds.contains(s.getId());
                for (LocalDate weekStart = from; !weekStart.isAfter(today); weekStart = weekStart.plusDays(7)) {
                    int entriesThisWeek = 2 + random.nextInt(2);
                    for (int i = 0; i < entriesThisWeek; i++) {
                        int offset = random.nextInt(7);
                        LocalDate gradeDate = weekStart.plusDays(offset);
                        if (gradeDate.isAfter(today) || gradeDate.isBefore(from)) continue;

                        Subject subject = classSubjects.get(random.nextInt(classSubjects.size()));
                        Grade g = new Grade();
                        g.setStudent(s);
                        g.setSubject(subject);
                        g.setGradeDate(gradeDate);
                        g.setScore(rollScore(struggling));
                        g.setType(rollGradeType());
                        gradeBatch.add(g);
                    }
                }
            }
            gradeRepository.saveAll(gradeBatch);
            gradeCount += gradeBatch.size();
        }

        return new int[]{attendanceCount, gradeCount};
    }

    private AttendanceStatus rollAttendanceStatus(boolean chronicAbsence, LocalDate date, LocalDate today) {
        if (chronicAbsence && date.isAfter(today.minusDays(4))) {
            return AttendanceStatus.ABSENT;
        }
        double r = random.nextDouble();
        if (r < 0.90) return AttendanceStatus.PRESENT;
        if (r < 0.94) return AttendanceStatus.LATE;
        if (r < 0.98) return AttendanceStatus.ABSENT;
        return AttendanceStatus.EXCUSED;
    }

    private int rollScore(boolean struggling) {
        double r = random.nextDouble();
        if (struggling) {
            if (r < 0.35) return 2;
            if (r < 0.75) return 3;
            if (r < 0.95) return 4;
            return 5;
        }
        if (r < 0.05) return 2;
        if (r < 0.25) return 3;
        if (r < 0.60) return 4;
        return 5;
    }

    private GradeType rollGradeType() {
        double r = random.nextDouble();
        if (r < 0.72) return GradeType.CURRENT;
        if (r < 0.90) return GradeType.EXAM;
        return GradeType.QUARTERLY;
    }

    private Set<Long> pickRandomSubset(List<Student> students, int count) {
        return pickRandomSubset(students, count, Set.of());
    }

    private Set<Long> pickRandomSubset(List<Student> students, int count, Set<Long> exclude) {
        List<Student> pool = students.stream().filter(s -> !exclude.contains(s.getId())).collect(Collectors.toList());
        Collections.shuffle(pool, random);
        return pool.stream().limit(count).map(Student::getId).collect(Collectors.toSet());
    }

    // ---------- behavior records ----------

    private int seedBehaviorRecords(List<SchoolClass> classes) {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(59);
        List<Student> allStudents = new ArrayList<>();
        for (SchoolClass cls : classes) {
            allStudents.addAll(studentRepository.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(cls.getId()));
        }
        if (allStudents.isEmpty()) return 0;

        int recordCount = Math.max(20, allStudents.size() / 6);
        List<BehaviorRecord> batch = new ArrayList<>();
        for (int i = 0; i < recordCount; i++) {
            Student s = allStudents.get(random.nextInt(allStudents.size()));
            BehaviorRecord r = new BehaviorRecord();
            r.setStudent(s);
            long daysAgo = random.nextInt(60);
            r.setRecordDate(today.minusDays(daysAgo));
            boolean reward = random.nextDouble() < 0.6;
            r.setType(reward ? BehaviorType.REWARD : BehaviorType.WARNING);
            String[] pool = reward ? BEHAVIOR_REWARD_DESCRIPTIONS : BEHAVIOR_WARNING_DESCRIPTIONS;
            r.setDescription(pool[random.nextInt(pool.length)]);
            batch.add(r);
        }
        behaviorRecordRepository.saveAll(batch);
        return batch.size();
    }

    // ---------- announcements ----------

    private int seedAnnouncements(School school, List<SchoolClass> classes) {
        LocalDate today = LocalDate.now();
        int count = 0;
        for (int i = 0; i < ANNOUNCEMENT_TITLES.length; i++) {
            Announcement a = new Announcement();
            a.setSchool(school);
            a.setTitle(ANNOUNCEMENT_TITLES[i]);
            a.setContent(ANNOUNCEMENT_BODIES[random.nextInt(ANNOUNCEMENT_BODIES.length)]);

            double audienceRoll = random.nextDouble();
            if (audienceRoll < 0.15 && !classes.isEmpty()) {
                a.setAudience(AnnouncementAudience.CLASS);
                a.setSchoolClass(classes.get(random.nextInt(classes.size())));
            } else if (audienceRoll < 0.35) {
                a.setAudience(AnnouncementAudience.TEACHERS);
            } else {
                a.setAudience(AnnouncementAudience.ALL);
            }

            double priorityRoll = random.nextDouble();
            a.setPriority(priorityRoll < 0.15 ? AnnouncementPriority.HIGH
                    : priorityRoll < 0.55 ? AnnouncementPriority.NORMAL : AnnouncementPriority.LOW);

            if (random.nextDouble() < 0.4) {
                a.setDeadline(today.plusDays(3 + random.nextInt(20)));
            }

            announcementRepository.save(a);
            count++;
        }
        return count;
    }

    // ---------- calendar events ----------

    private int seedCalendarEvents(School school) {
        LocalDate today = LocalDate.now();
        int count = 0;

        for (String title : CALENDAR_TITLES_HOLIDAY) {
            CalendarEvent e = new CalendarEvent();
            e.setSchool(school);
            e.setTitle(title);
            e.setType(CalendarEventType.HOLIDAY);
            LocalDate start = today.plusDays(random.nextInt(120) - 30);
            e.setStartDate(start);
            e.setEndDate(start);
            calendarEventRepository.save(e);
            count++;
        }

        for (String title : CALENDAR_TITLES_EXAM) {
            CalendarEvent e = new CalendarEvent();
            e.setSchool(school);
            e.setTitle(title);
            e.setType(CalendarEventType.EXAM);
            LocalDate start = today.plusDays(random.nextInt(60) - 10);
            e.setStartDate(start);
            e.setEndDate(start.plusDays(1 + random.nextInt(3)));
            calendarEventRepository.save(e);
            count++;
        }

        CalendarEvent meeting = new CalendarEvent();
        meeting.setSchool(school);
        meeting.setTitle("Ota-onalar yig'ilishi");
        meeting.setDescription("Barcha sinflar uchun umumiy ota-onalar yig'ilishi.");
        meeting.setType(CalendarEventType.PARENT_MEETING);
        LocalDate meetingDate = today.plusDays(5 + random.nextInt(15));
        meeting.setStartDate(meetingDate);
        meeting.setEndDate(meetingDate);
        calendarEventRepository.save(meeting);
        count++;

        CalendarEvent vacation = new CalendarEvent();
        vacation.setSchool(school);
        vacation.setTitle("Kuzgi ta'til");
        vacation.setType(CalendarEventType.VACATION);
        LocalDate vacationStart = today.plusDays(25);
        vacation.setStartDate(vacationStart);
        vacation.setEndDate(vacationStart.plusDays(9));
        calendarEventRepository.save(vacation);
        count++;

        return count;
    }
}
