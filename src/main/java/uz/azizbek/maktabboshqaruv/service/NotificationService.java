package uz.azizbek.maktabboshqaruv.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.event.AnnouncementCreatedEvent;
import uz.azizbek.maktabboshqaruv.event.AttendanceMarkedEvent;
import uz.azizbek.maktabboshqaruv.event.GradeSavedEvent;
import uz.azizbek.maktabboshqaruv.repository.*;
import uz.azizbek.maktabboshqaruv.telegram.MessageFormatter;
import uz.azizbek.maktabboshqaruv.telegram.QuietHours;
import uz.azizbek.maktabboshqaruv.telegram.TelegramProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns committed domain events into outbox rows (NotificationLog). Never
 * talks to Telegram itself — NotificationSender does that on its own schedule.
 *
 * Each enqueue method runs in a REQUIRES_NEW transaction: it is invoked from
 * an AFTER_COMMIT listener, where the original transaction has already
 * finished, so joining it would silently never commit.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Autowired
    private ParentTelegramLinkRepository linkRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private LessonSlotRepository lessonSlotRepository;

    @Autowired
    private NotificationSettingsService settingsService;

    @Autowired
    private TelegramProperties telegramProperties;

    @Autowired
    private Clock clock;

    record Recipient(Student student, Long chatId) {
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int enqueueAttendance(AttendanceMarkedEvent event) {
        if (!AttendanceMarkedEvent.isNotifiable(event.previousStatus(), event.newStatus())) {
            return 0;
        }
        Attendance attendance = attendanceRepository.findById(event.attendanceId()).orElse(null);
        // Re-marked again (or deleted) before we got here — the newer save decides.
        if (attendance == null || attendance.getStatus() != event.newStatus()) {
            return 0;
        }

        NotificationType type = attendance.getStatus() == AttendanceStatus.ABSENT
                ? NotificationType.ATTENDANCE_ABSENT : NotificationType.ATTENDANCE_LATE;
        Student student = attendance.getStudent();
        if (notificationLogRepository.existsByStudentIdAndTypeAndReferenceIdAndRecordDate(
                student.getId(), type, attendance.getId(), attendance.getRecordDate())) {
            log.debug("Takroriy xabar o'tkazib yuborildi: {} attendance={}", type, attendance.getId());
            return 0;
        }

        LessonSlot slot = attendance.getLessonSlot();
        School school = schoolOf(student);
        LocalDate today = LocalDate.now(clock);
        String text = type == NotificationType.ATTENDANCE_ABSENT
                ? MessageFormatter.attendanceAbsent(fullName(student), className(student), lessonNumber(slot),
                slot.getSubject().getName(), slot.getStartTime(), attendance.getRecordDate(), today, school.getName())
                : MessageFormatter.attendanceLate(fullName(student), className(student), lessonNumber(slot),
                slot.getSubject().getName(), slot.getStartTime(), attendance.getRecordDate(), today, school.getName());

        return createRows(school, type, attendance.getId(), attendance.getRecordDate(), text, recipientsOf(student));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int enqueueGrade(GradeSavedEvent event) {
        Grade grade = gradeRepository.findById(event.gradeId()).orElse(null);
        if (grade == null) return 0;

        NotificationType type = event.created() ? NotificationType.GRADE_NEW : NotificationType.GRADE_UPDATED;
        // A new grade is keyed by its own date. Edits are keyed by the day of the
        // edit, so a teacher fixing the same grade five times in one day sends one update.
        LocalDate recordDate = event.created() ? grade.getGradeDate() : LocalDate.now(clock);
        Student student = grade.getStudent();
        if (notificationLogRepository.existsByStudentIdAndTypeAndReferenceIdAndRecordDate(
                student.getId(), type, grade.getId(), recordDate)) {
            log.debug("Takroriy xabar o'tkazib yuborildi: {} grade={}", type, grade.getId());
            return 0;
        }

        School school = schoolOf(student);
        String text = event.created()
                ? MessageFormatter.gradeNew(fullName(student), grade.getSubject().getName(), grade.getScore(),
                grade.getType(), grade.getGradeDate(), school.getName())
                : MessageFormatter.gradeUpdated(fullName(student), grade.getSubject().getName(), grade.getScore(),
                grade.getType(), grade.getGradeDate(), school.getName());

        return createRows(school, type, grade.getId(), recordDate, text, recipientsOf(student));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int enqueueAnnouncement(AnnouncementCreatedEvent event) {
        Announcement announcement = announcementRepository.findById(event.announcementId()).orElse(null);
        if (announcement == null || announcement.getAudience() == AnnouncementAudience.TEACHERS) {
            return 0;
        }
        if (notificationLogRepository.existsByTypeAndReferenceId(NotificationType.ANNOUNCEMENT, announcement.getId())) {
            return 0;
        }

        List<ParentTelegramLink> links;
        String classLabel = null;
        if (announcement.getAudience() == AnnouncementAudience.CLASS) {
            if (announcement.getSchoolClass() == null) return 0;
            links = linkRepository.findActiveByClassId(announcement.getSchoolClass().getId());
            classLabel = announcement.getSchoolClass().getGradeNumber() + "-" + announcement.getSchoolClass().getSectionLetter();
        } else {
            links = linkRepository.findActiveBySchoolId(announcement.getSchool().getId());
        }

        // A parent with two children in the school gets the announcement once, not twice.
        Map<Long, Student> byChat = new LinkedHashMap<>();
        for (ParentTelegramLink link : links) {
            byChat.putIfAbsent(link.getChatId(), link.getStudent());
        }
        List<Recipient> recipients = new ArrayList<>();
        byChat.forEach((chatId, student) -> recipients.add(new Recipient(student, chatId)));

        School school = announcement.getSchool();
        String text = MessageFormatter.announcement(announcement.getTitle(), announcement.getContent(),
                classLabel, school.getName());
        LocalDate recordDate = announcement.getCreatedDate() != null
                ? announcement.getCreatedDate().toLocalDate() : LocalDate.now(clock);

        return createRows(school, NotificationType.ANNOUNCEMENT, announcement.getId(), recordDate, text, recipients);
    }

    /** Writes one outbox row per recipient; the row's status already reflects bot mode, school settings and quiet hours. */
    int createRows(School school, NotificationType type, Long referenceId, LocalDate recordDate,
                   String text, List<Recipient> recipients) {
        if (recipients.isEmpty()) {
            log.debug("{} ref={}: bog'langan ota-ona yo'q", type, referenceId);
            return 0;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        NotificationSettings settings = settingsService.getOrDefault(school);

        NotificationStatus status = NotificationStatus.PENDING;
        String skipReason = null;
        if (!telegramProperties.isActive()) {
            status = NotificationStatus.SKIPPED;
            skipReason = telegramProperties.mode() == TelegramProperties.Mode.NO_TOKEN
                    ? "Bot tokeni berilmagan (TELEGRAM_BOT_TOKEN)"
                    : "Telegram o'chirilgan (TELEGRAM_ENABLED=false)";
        } else if (!settings.isTypeEnabled(type)) {
            status = NotificationStatus.SKIPPED;
            skipReason = "Maktab sozlamalarida bu turdagi xabarlar o'chirilgan";
        }

        LocalDateTime scheduledAt = Boolean.TRUE.equals(settings.getQuietHoursEnabled())
                ? QuietHours.deliveryTime(now, settings.getQuietHoursStart(), settings.getQuietHoursEnd())
                : now;

        int created = 0;
        for (Recipient r : recipients) {
            NotificationLog row = new NotificationLog();
            row.setSchool(school);
            row.setStudent(r.student());
            row.setChatId(r.chatId());
            row.setType(type);
            row.setReferenceId(referenceId);
            row.setRecordDate(recordDate);
            row.setText(text);
            row.setStatus(status);
            row.setAttempts(0);
            row.setLastError(skipReason);
            row.setCreatedAt(now);
            row.setScheduledAt(scheduledAt);
            notificationLogRepository.save(row);
            created++;
        }

        if (status == NotificationStatus.SKIPPED) {
            log.info("Xabarnoma SKIPPED: {} ref={} ({} ta qabul qiluvchi) — {}", type, referenceId, created, skipReason);
        } else if (scheduledAt.isAfter(now)) {
            log.info("Xabarnoma navbatga qo'shildi: {} ref={} ({} ta) — tinch soatlar, {} da yuboriladi",
                    type, referenceId, created, scheduledAt.toLocalTime());
        } else {
            log.info("Xabarnoma navbatga qo'shildi: {} ref={} ({} ta)", type, referenceId, created);
        }
        return created;
    }

    private List<Recipient> recipientsOf(Student student) {
        List<Recipient> recipients = new ArrayList<>();
        for (ParentTelegramLink link : linkRepository.findByStudentIdAndActiveTrueOrderByLinkedAtAsc(student.getId())) {
            recipients.add(new Recipient(student, link.getChatId()));
        }
        return recipients;
    }

    /** 1-based position of the lesson among the class's lessons that weekday ("2-dars"). */
    private Integer lessonNumber(LessonSlot slot) {
        List<LessonSlot> sameDay = lessonSlotRepository.findBySchoolClassId(slot.getSchoolClass().getId()).stream()
                .filter(l -> l.getWeekday().equals(slot.getWeekday()))
                .sorted(Comparator.comparing(LessonSlot::getStartTime))
                .toList();
        for (int i = 0; i < sameDay.size(); i++) {
            if (sameDay.get(i).getId().equals(slot.getId())) return i + 1;
        }
        return null;
    }

    private static School schoolOf(Student student) {
        return student.getSchoolClass().getAcademicYear().getSchool();
    }

    private static String fullName(Student student) {
        return student.getFirstName() + " " + student.getLastName();
    }

    private static String className(Student student) {
        return student.getSchoolClass().getGradeNumber() + "-" + student.getSchoolClass().getSectionLetter();
    }
}
