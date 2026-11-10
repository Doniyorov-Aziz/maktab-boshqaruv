package uz.azizbek.maktabboshqaruv.service.parent;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.azizbek.maktabboshqaruv.bot.BotI18n;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.*;
import uz.azizbek.maktabboshqaruv.service.*;
import uz.azizbek.maktabboshqaruv.telegram.MessageFormatter;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * What the parents' bot shows of the school modules — homework, exams, meetings, the tabel, payments and
 * achievements — read in one transaction per page and handed to the screens as plain records.
 */
@Service
public class ParentModulesService {

    public record HomeworkItem(Long id, String subject, String title, String description, LocalDate assigned,
                               LocalDate due, String fileName) {
    }

    public record ExamItem(Long id, String subject, Exam.Kind kind, String title, LocalDate date, LocalTime time,
                           String place, Integer score) {
    }

    public record TeacherFree(Long employeeId, String name, boolean classTeacher, int free) {
    }

    public record SlotItem(Long id, Long employeeId, String teacher, LocalDate date, LocalTime start, LocalTime end,
                           String place, String childName) {
    }

    public record AchievementItem(String title, Achievement.Category category, Achievement.Level level, Integer place,
                                  LocalDate date, String subject) {
    }

    /** How a request for a file came out. */
    public enum FileResult { QUEUED, ALREADY, NONE }

    private final HomeworkRepository homework;
    private final ExamRepository exams;
    private final GradeRepository grades;
    private final MeetingService meetings;
    private final ReportCardService reportCards;
    private final PaymentService payments;
    private final AchievementRepository achievements;
    private final StudentRepository students;
    private final NotificationService notifications;
    private final Clock clock;

    public ParentModulesService(HomeworkRepository homework, ExamRepository exams, GradeRepository grades,
                                MeetingService meetings, ReportCardService reportCards, PaymentService payments,
                                AchievementRepository achievements, StudentRepository students,
                                NotificationService notifications, Clock clock) {
        this.homework = homework;
        this.exams = exams;
        this.grades = grades;
        this.meetings = meetings;
        this.reportCards = reportCards;
        this.payments = payments;
        this.achievements = achievements;
        this.students = students;
        this.notifications = notifications;
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    // ------------------------------------------------------------------ homework

    @Transactional(readOnly = true)
    public List<HomeworkItem> homeworkUpcoming(Student s) {
        return homework.upcoming(s.getSchoolClass().getId(), today()).stream().map(ParentModulesService::item).toList();
    }

    @Transactional(readOnly = true)
    public List<HomeworkItem> homeworkPast(Student s, int days) {
        LocalDate today = today();
        return homework.givenBetween(s.getSchoolClass().getId(), today.minusDays(days), today).stream()
                .filter(h -> h.getDueDate().isBefore(today)).map(ParentModulesService::item).toList();
    }

    /** "📎" under a homework: the file in the chat (once a day per homework). */
    @Transactional
    public FileResult sendHomeworkFile(Student s, long chatId, Long homeworkId) {
        Homework h = homework.findById(homeworkId).orElse(null);
        if (h == null || h.getFilePath() == null || !h.getSchoolClass().getId().equals(s.getSchoolClass().getId())) {
            return FileResult.NONE;
        }
        String subject = h.getSubject().getName();
        String title = h.getTitle();
        int n = notifications.enqueueDirect(h.getSchool(), s, chatId, NotificationType.MODULE_FILE, h.getId(), today(),
                lang -> "📎 <b>" + MessageFormatter.escape(subject) + "</b> — " + MessageFormatter.escape(title),
                null, HomeworkMedia.ref(h.getId()));
        return n > 0 ? FileResult.QUEUED : FileResult.ALREADY;
    }

    // ------------------------------------------------------------------ exams

    /** The class's exams from a week ago to a month ahead, with the child's result when there is one. */
    @Transactional(readOnly = true)
    public List<ExamItem> exams(Student s) {
        LocalDate today = today();
        List<Exam> list = exams.ofClass(s.getSchoolClass().getId(), today.minusDays(7), today.plusDays(31));
        Map<Long, Integer> scores = new HashMap<>();
        for (Exam x : list) {
            if (x.getExamDate().isAfter(today)) continue;
            grades.findByExamId(x.getId()).stream().filter(g -> g.getStudent().getId().equals(s.getId())).findFirst()
                    .ifPresent(g -> scores.put(x.getId(), g.getScore()));
        }
        return list.stream().map(x -> new ExamItem(x.getId(), x.getSubject().getName(), x.getKind(), x.getTitle(),
                x.getExamDate(), x.getStartTime(), x.getPlace(), scores.get(x.getId()))).toList();
    }

    // ------------------------------------------------------------------ meetings

    @Transactional(readOnly = true)
    public List<TeacherFree> meetingTeachers(Student s) {
        Long ct = s.getSchoolClass().getClassTeacher() == null ? null : s.getSchoolClass().getClassTeacher().getId();
        return meetings.teachersWithFreeTimes(s).stream()
                .map(e -> new TeacherFree(e.getKey().getId(), name(e.getKey()), e.getKey().getId().equals(ct), e.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SlotItem> freeSlots(Student s, Long teacherId) {
        return meetings.freeFor(s, teacherId).stream().map(m -> slot(m, null)).toList();
    }

    @Transactional(readOnly = true)
    public List<SlotItem> myBookings(long chatId) {
        return meetings.bookingsOf(chatId).stream()
                .map(m -> slot(m, m.getStudent() == null ? null : NotificationService.fullName(m.getStudent()))).toList();
    }

    @Transactional(readOnly = true)
    public Optional<SlotItem> slot(Long id) {
        return meetings.slot(id).map(m -> slot(m, null));
    }

    public MeetingService.BookResult book(long chatId, Student s, Long slotId, String parentName) {
        return meetings.book(chatId, s, slotId, parentName, null);
    }

    public boolean cancel(long chatId, Long slotId) {
        return meetings.cancelByParent(chatId, slotId);
    }

    // ------------------------------------------------------------------ tabel

    @Transactional(readOnly = true)
    public ReportCardService.StudentYear tabel(Student s) {
        return reportCards.studentYear(s.getId(), null);
    }

    /** "📄 PDF" of the tabel in the chat (once a day). The outbox key is the pupil (negative, to stay apart from homework). */
    @Transactional
    public FileResult sendTabel(Student s, long chatId) {
        Student st = students.findById(s.getId()).orElse(null);
        if (st == null) return FileResult.NONE;
        String name = NotificationService.fullName(st);
        int n = notifications.enqueueDirect(NotificationService.schoolOf(st), st, chatId, NotificationType.MODULE_FILE,
                -st.getId(), today(), lang -> BotI18n.get().t(lang, "tab.caption", "name", MessageFormatter.escape(name)),
                null, "reportcard:" + st.getId() + ":");
        return n > 0 ? FileResult.QUEUED : FileResult.ALREADY;
    }

    // ------------------------------------------------------------------ payments

    public boolean paymentsEnabled(Student s) {
        return payments.enabled(NotificationService.schoolOf(s).getId());
    }

    public PaymentService.StudentAccount account(Student s) {
        return payments.account(s.getId());
    }

    // ------------------------------------------------------------------ achievements

    @Transactional(readOnly = true)
    public List<AchievementItem> achievements(Student s) {
        return achievements.ofStudent(s.getId()).stream()
                .map(a -> new AchievementItem(a.getTitle(), a.getCategory(), a.getLevel(), a.getPlace(), a.getAchievedOn(),
                        a.getSubject() == null ? null : a.getSubject().getName()))
                .toList();
    }

    // ------------------------------------------------------------------ helpers

    private static HomeworkItem item(Homework h) {
        return new HomeworkItem(h.getId(), h.getSubject().getName(), h.getTitle(), h.getDescription(), h.getAssignedDate(),
                h.getDueDate(), h.getFileName());
    }

    private static SlotItem slot(MeetingSlot m, String childName) {
        return new SlotItem(m.getId(), m.getEmployee().getId(), name(m.getEmployee()), m.getDate(), m.getStartTime(),
                m.getEndTime(), m.getPlace(), childName);
    }

    private static String name(Employee e) {
        return (e.getLastName() + " " + e.getFirstName()).strip();
    }
}
