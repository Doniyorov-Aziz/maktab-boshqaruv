package uz.azizbek.maktabboshqaruv.config;

import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

/**
 * Presentation data for the parent bot — ONLY with the "dev" profile
 * (never in prod): a demo parent linked to one student, and a realistic last
 * week for that student (attendance, grades), an announcement due tomorrow and
 * an upcoming parent meeting. Rows are written straight to the repositories,
 * so no notification is sent. Running it again adds nothing twice.
 */
@Component
@Profile("dev")
@Order(10)
public class DemoParentSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoParentSeeder.class);
    private static final Map<DayOfWeek, String> WEEKDAY_NAMES = Map.of(
            DayOfWeek.MONDAY, "Dushanba", DayOfWeek.TUESDAY, "Seshanba", DayOfWeek.WEDNESDAY, "Chorshanba",
            DayOfWeek.THURSDAY, "Payshanba", DayOfWeek.FRIDAY, "Juma", DayOfWeek.SATURDAY, "Shanba");
    static final String DEMO_ANNOUNCEMENT = "Ertangi ochiq dars uchun daftar va forma";
    static final String DEMO_MEETING = "Ota-onalar yig'ilishi (demo)";
    private static final int[] SCORES = {5, 4, 5, 3, 4, 5, 4, 5, 2, 4};

    private final StudentRepository studentRepository;
    private final LessonSlotRepository lessonSlotRepository;
    private final AttendanceRepository attendanceRepository;
    private final GradeRepository gradeRepository;
    private final AnnouncementRepository announcementRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final ParentTelegramLinkRepository linkRepository;
    private final ParentSessionRepository sessionRepository;
    private final Clock clock;
    private final long demoChatId;

    public DemoParentSeeder(StudentRepository studentRepository, LessonSlotRepository lessonSlotRepository,
                            AttendanceRepository attendanceRepository, GradeRepository gradeRepository,
                            AnnouncementRepository announcementRepository, CalendarEventRepository calendarEventRepository,
                            ParentTelegramLinkRepository linkRepository, ParentSessionRepository sessionRepository,
                            Clock clock, @Value("${demo.parent-chat-id:990000001}") long demoChatId) {
        this.studentRepository = studentRepository;
        this.lessonSlotRepository = lessonSlotRepository;
        this.attendanceRepository = attendanceRepository;
        this.gradeRepository = gradeRepository;
        this.announcementRepository = announcementRepository;
        this.calendarEventRepository = calendarEventRepository;
        this.linkRepository = linkRepository;
        this.sessionRepository = sessionRepository;
        this.clock = clock;
        this.demoChatId = demoChatId;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Student student = demoStudent();
        if (student == null) {
            log.warn("Demo: jadvali bor sinf topilmadi, demo ota-ona yaratilmadi");
            return;
        }
        link(student);
        LocalDate today = LocalDate.now(clock);
        int attendance = 0;
        int grades = 0;
        List<LessonSlot> slots = lessonSlotRepository.findBySchoolClassId(student.getSchoolClass().getId());
        for (int back = 6; back >= 0; back--) {
            LocalDate day = today.minusDays(back);
            String weekday = WEEKDAY_NAMES.get(day.getDayOfWeek());
            if (weekday == null) continue; // Sunday
            List<LessonSlot> lessons = slots.stream().filter(s -> weekday.equals(s.getWeekday()))
                    .sorted(Comparator.comparing(LessonSlot::getStartTime)).toList();
            for (int i = 0; i < lessons.size(); i++) {
                attendance += attend(student, lessons.get(i), day, statusFor(back, i));
            }
            grades += grade(student, lessons, day, back);
        }
        School school = student.getSchoolClass().getAcademicYear().getSchool();
        announce(school, student.getSchoolClass(), today);
        meeting(school, today);
        log.info("Demo ota-ona tayyor: chat_id={}, o'quvchi={} {} ({} ta davomat, {} ta baho qo'shildi). "
                        + "Mock rejimda: POST /api/telegram/mock/updates {\"chatId\":{},\"text\":\"/start\"}",
                demoChatId, student.getFirstName(), student.getLastName(), attendance, grades, demoChatId);
    }

    /** The first student of the first class that has a timetable. */
    private Student demoStudent() {
        return lessonSlotRepository.findAll().stream()
                .map(LessonSlot::getSchoolClass)
                .distinct()
                .sorted(Comparator.comparing(SchoolClass::getId))
                .map(c -> studentRepository.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(c.getId()))
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .findFirst().orElse(null);
    }

    private void link(Student student) {
        ParentTelegramLink link = linkRepository.findByStudentIdAndChatId(student.getId(), demoChatId).orElseGet(() -> {
            ParentTelegramLink l = new ParentTelegramLink();
            l.setStudent(student);
            l.setChatId(demoChatId);
            l.setFirstName("Demo ota-ona");
            l.setTelegramUsername("demo_parent");
            l.setLinkedAt(LocalDateTime.now(clock));
            return l;
        });
        link.setActive(true);
        linkRepository.save(link);

        ParentSession session = sessionRepository.findByChatId(demoChatId).orElseGet(() -> {
            ParentSession s = new ParentSession();
            s.setChatId(demoChatId);
            s.setLanguage("uz");
            s.setCreatedAt(LocalDateTime.now(clock));
            return s;
        });
        session.setSelectedStudentId(student.getId());
        session.setFirstName("Demo ota-ona");
        sessionRepository.save(session);
    }

    /** A realistic week: present almost always, one late arrival and one missed lesson. */
    private static AttendanceStatus statusFor(int daysBack, int lessonIndex) {
        if (daysBack == 3 && lessonIndex == 0) return AttendanceStatus.LATE;
        if (daysBack == 5 && lessonIndex == 2) return AttendanceStatus.ABSENT;
        return AttendanceStatus.PRESENT;
    }

    private int attend(Student student, LessonSlot slot, LocalDate day, AttendanceStatus status) {
        if (attendanceRepository.existsByLessonSlotIdAndStudentIdAndRecordDate(slot.getId(), student.getId(), day)) return 0;
        Attendance a = new Attendance();
        a.setLessonSlot(slot);
        a.setStudent(student);
        a.setRecordDate(day);
        a.setStatus(status);
        a.setComment("");
        attendanceRepository.save(a);
        return 1;
    }

    /** One or two grades a day in that day's subjects, unless the day already has grades. */
    private int grade(Student student, List<LessonSlot> lessons, LocalDate day, int daysBack) {
        if (lessons.isEmpty()) return 0;
        if (!gradeRepository.findByStudentIdAndGradeDateBetweenOrderByGradeDateDescIdDesc(student.getId(), day, day).isEmpty()) {
            return 0;
        }
        int count = daysBack % 2 == 0 ? 2 : 1;
        for (int i = 0; i < count && i < lessons.size(); i++) {
            Grade g = new Grade();
            g.setStudent(student);
            g.setSubject(lessons.get(i * 2 % lessons.size()).getSubject());
            g.setGradeDate(day);
            g.setScore(SCORES[(daysBack * 2 + i) % SCORES.length]);
            g.setType(GradeType.CURRENT);
            g.setComment("");
            gradeRepository.save(g);
        }
        return count;
    }

    private void announce(School school, SchoolClass schoolClass, LocalDate today) {
        boolean exists = announcementRepository.findForParents(school.getId(), schoolClass.getId()).stream()
                .anyMatch(a -> DEMO_ANNOUNCEMENT.equals(a.getTitle()) && today.plusDays(1).equals(a.getDeadline()));
        if (exists) return;
        Announcement a = new Announcement();
        a.setSchool(school);
        a.setSchoolClass(schoolClass);
        a.setAudience(AnnouncementAudience.CLASS);
        a.setPriority(AnnouncementPriority.HIGH);
        a.setTitle(DEMO_ANNOUNCEMENT);
        a.setContent("Hurmatli ota-onalar! Ertaga ochiq dars bo'ladi: farzandingiz katakli daftar va maktab formasi bilan kelsin.");
        a.setDeadline(today.plusDays(1));
        announcementRepository.save(a);
    }

    private void meeting(School school, LocalDate today) {
        LocalDate day = today.plusDays(2);
        boolean exists = calendarEventRepository.findInRange(school.getId(), day, day).stream()
                .anyMatch(e -> DEMO_MEETING.equals(e.getTitle()));
        if (exists) return;
        CalendarEvent e = new CalendarEvent();
        e.setSchool(school);
        e.setTitle(DEMO_MEETING);
        e.setDescription("Soat 18:00 da maktab majlislar zalida. Choraklik natijalar muhokama qilinadi.");
        e.setType(CalendarEventType.PARENT_MEETING);
        e.setStartDate(day);
        e.setEndDate(day);
        calendarEventRepository.save(e);
    }
}
