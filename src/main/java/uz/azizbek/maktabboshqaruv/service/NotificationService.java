package uz.azizbek.maktabboshqaruv.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.azizbek.maktabboshqaruv.bot.BotI18n;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.event.AnnouncementCreatedEvent;
import uz.azizbek.maktabboshqaruv.event.AttendanceMarkedEvent;
import uz.azizbek.maktabboshqaruv.event.GradeSavedEvent;
import uz.azizbek.maktabboshqaruv.repository.*;
import uz.azizbek.maktabboshqaruv.telegram.MessageFormatter;
import uz.azizbek.maktabboshqaruv.telegram.QuietHours;
import uz.azizbek.maktabboshqaruv.telegram.TelegramJson;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineButton;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineKeyboardMarkup;
import uz.azizbek.maktabboshqaruv.telegram.TelegramProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;

/**
 * Turns committed domain events (and scheduled digests, replies, broadcasts)
 * into outbox rows (NotificationLog). Never talks to Telegram itself —
 * NotificationSender does that on its own schedule.
 *
 * Every row is personal: written in the parent's language, honouring the
 * parent's own switches and quiet hours on top of the school's settings.
 *
 * Event-driven enqueue methods run in a REQUIRES_NEW transaction: they are
 * invoked from an AFTER_COMMIT listener, where the original transaction has
 * already finished, so joining it would silently never commit.
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
    private ParentSessionRepository sessionRepository;

    @Autowired
    private BotSettingService botSettingService;

    @Autowired
    private TelegramProperties telegramProperties;

    @Autowired
    private Clock clock;

    public record Recipient(Student student, Long chatId) {
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
        Integer lessonNumber = lessonNumber(slot);
        Function<String, String> text = lang -> type == NotificationType.ATTENDANCE_ABSENT
                ? MessageFormatter.attendanceAbsent(lang, fullName(student), className(student), lessonNumber,
                slot.getSubject().getName(), slot.getStartTime(), attendance.getRecordDate(), today, school.getName())
                : MessageFormatter.attendanceLate(lang, fullName(student), className(student), lessonNumber,
                slot.getSubject().getName(), slot.getStartTime(), attendance.getRecordDate(), today, school.getName());

        return createRows(school, type, attendance.getId(), attendance.getRecordDate(), recipientsOf(student), text,
                actions("att:s:" + student.getId() + ":n:1", "/attendance", student.getId()));
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
                student.getId(), type, grade.getId(), recordDate)
                || notificationLogRepository.existsByStudentIdAndTypeAndReferenceIdAndRecordDate(
                student.getId(), NotificationType.GRADE_LOW, grade.getId(), recordDate)) {
            log.debug("Takroriy xabar o'tkazib yuborildi: {} grade={}", type, grade.getId());
            return 0;
        }

        School school = schoolOf(student);
        List<Recipient> recipients = recipientsOf(student);
        Function<String, String> normalText = lang -> event.created()
                ? MessageFormatter.gradeNew(lang, fullName(student), grade.getSubject().getName(), grade.getScore(),
                grade.getType(), grade.getGradeDate(), school.getName())
                : MessageFormatter.gradeUpdated(lang, fullName(student), grade.getSubject().getName(), grade.getScore(),
                grade.getType(), grade.getGradeDate(), school.getName());

        int threshold = botSettingService.getOrDefault(school).getLowGradeThreshold();
        if (!event.created() || grade.getScore() > threshold || recipients.isEmpty()) {
            return createRows(school, type, grade.getId(), recordDate, recipients, normalText,
                    actions("gr:s:" + student.getId() + ":n:1", "/grades", student.getId()));
        }

        // Low grade: parents who keep the "past baho" switch on get the gentle
        // version with a "write to the teacher" button; others get the normal one.
        Map<Long, ParentSession> sessions = sessionsOf(recipients);
        List<Recipient> gentle = new ArrayList<>();
        List<Recipient> normal = new ArrayList<>();
        for (Recipient r : recipients) {
            ParentSession s = sessions.get(r.chatId());
            (s == null || s.wants(NotificationType.GRADE_LOW) ? gentle : normal).add(r);
        }
        int created = createRows(school, NotificationType.GRADE_LOW, grade.getId(), recordDate, gentle,
                lang -> MessageFormatter.gradeLow(lang, fullName(student), grade.getSubject().getName(), grade.getScore(),
                        grade.getGradeDate(), school.getName()),
                lang -> InlineKeyboardMarkup.builder().row(InlineButton.callback(BotI18n.get().t(lang, "notif.btn.write_teacher"),
                        "msg:a:to:to:CT:s:" + student.getId() + ":n:1")).build());
        created += createRows(school, type, grade.getId(), recordDate, normal, normalText,
                actions("gr:s:" + student.getId() + ":n:1", "/grades", student.getId()));
        return created;
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

        School school = announcement.getSchool();
        String label = classLabel;
        LocalDate recordDate = announcement.getCreatedDate() != null
                ? announcement.getCreatedDate().toLocalDate() : LocalDate.now(clock);
        return createRows(school, NotificationType.ANNOUNCEMENT, announcement.getId(), recordDate, oncePerChat(links),
                lang -> MessageFormatter.announcement(lang, announcement.getTitle(), announcement.getContent(), label, school.getName()),
                actions("ann:id:" + announcement.getId() + ":n:1", "/announcements", null));
    }

    /**
     * One personal message (a reply, a decision, a digest) through the outbox,
     * skipping silently if an identical one was already queued for this chat.
     */
    @Transactional
    public int enqueueDirect(School school, Student student, Long chatId, NotificationType type, Long referenceId,
                             LocalDate recordDate, Function<String, String> text, Function<String, Object> markup) {
        if (notificationLogRepository.existsByChatIdAndTypeAndReferenceIdAndRecordDate(chatId, type, referenceId, recordDate)) {
            return 0;
        }
        return createRows(school, type, referenceId, recordDate, List.of(new Recipient(student, chatId)), text, markup);
    }

    /** A personal message with files (see {@link NotificationLog#getMedia()}), e.g. an appeal reply. */
    @Transactional
    public int enqueueDirect(School school, Student student, Long chatId, NotificationType type, Long referenceId,
                             LocalDate recordDate, Function<String, String> text, Function<String, Object> markup,
                             String media) {
        if (notificationLogRepository.existsByChatIdAndTypeAndReferenceIdAndRecordDate(chatId, type, referenceId, recordDate)) {
            return 0;
        }
        return createRows(school, type, referenceId, recordDate, List.of(new Recipient(student, chatId)), text, markup, media);
    }

    /**
     * Buttons under an automatic message: "📱 Batafsil" opens the matching Mini App
     * page when TELEGRAM_WEBAPP_URL is set (else the bot page as a new card), and
     * "💬 Sinf rahbariga yozish" starts a message to the class teacher.
     */
    Function<String, Object> actions(String callbackData, String webAppPage, Long teacherForStudentId) {
        String webApp = telegramProperties.webappUrlIfValid();
        return lang -> {
            BotI18n i18n = BotI18n.get();
            String label = i18n.t(lang, "notif.btn.app_details");
            InlineButton details = webApp != null
                    ? InlineButton.webApp(label, webApp + webAppPage)
                    : InlineButton.callback(label, callbackData).styled("primary");
            InlineKeyboardMarkup.Builder kb = InlineKeyboardMarkup.builder().row(details);
            if (teacherForStudentId != null) {
                kb.row(InlineButton.callback(i18n.t(lang, "notif.btn.write_class_teacher"),
                        "msg:a:to:to:CT:s:" + teacherForStudentId + ":n:1"));
            }
            return kb.build();
        };
    }

    /** A parent with two children in the same audience gets one message, not two. */
    public static List<Recipient> oncePerChat(List<ParentTelegramLink> links) {
        Map<Long, Student> byChat = new LinkedHashMap<>();
        for (ParentTelegramLink link : links) {
            byChat.putIfAbsent(link.getChatId(), link.getStudent());
        }
        List<Recipient> recipients = new ArrayList<>();
        byChat.forEach((chatId, student) -> recipients.add(new Recipient(student, chatId)));
        return recipients;
    }

    /**
     * Writes one outbox row per recipient. Each row's status already reflects
     * the bot mode, the school's switches and the parent's own switches; its
     * delivery time reflects quiet hours (the parent's, else the school's).
     */
    public int createRows(School school, NotificationType type, Long referenceId, LocalDate recordDate,
                          List<Recipient> recipients, Function<String, String> text, Function<String, Object> markup) {
        return createRows(school, type, referenceId, recordDate, recipients, text, markup, null);
    }

    /** @param media files that go with every row ("broadcast:7", "appeal:12,13"), or null */
    public int createRows(School school, NotificationType type, Long referenceId, LocalDate recordDate,
                          List<Recipient> recipients, Function<String, String> text, Function<String, Object> markup,
                          String media) {
        if (recipients.isEmpty()) {
            log.debug("{} ref={}: bog'langan ota-ona yo'q", type, referenceId);
            return 0;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        NotificationSettings settings = settingsService.getOrDefault(school);
        Map<Long, ParentSession> sessions = sessionsOf(recipients);

        int created = 0;
        int skipped = 0;
        String lastSkipReason = null;
        LocalDateTime lastScheduled = now;
        for (Recipient r : recipients) {
            ParentSession session = sessions.get(r.chatId());
            String lang = session == null ? BotI18n.DEFAULT_LANG : session.lang();

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
            } else if (session != null && !session.wants(type)) {
                status = NotificationStatus.SKIPPED;
                skipReason = "Ota-ona bot sozlamalarida bu turdagi xabarlarni o'chirgan";
            }

            LocalDateTime scheduledAt = deliveryTime(now, settings, session);

            NotificationLog row = new NotificationLog();
            row.setSchool(school);
            row.setStudent(r.student());
            row.setChatId(r.chatId());
            row.setType(type);
            row.setReferenceId(referenceId);
            row.setRecordDate(recordDate);
            row.setText(text.apply(lang));
            row.setReplyMarkup(markup == null ? null : TelegramJson.write(markup.apply(lang)));
            row.setStatus(status);
            row.setAttempts(0);
            row.setLastError(skipReason);
            row.setCreatedAt(now);
            row.setScheduledAt(scheduledAt);
            row.setMedia(media);
            // Held back by quiet hours: goes out in the single morning note with the others
            // (a message with files keeps its own message).
            if (scheduledAt.isAfter(now) && status == NotificationStatus.PENDING && media == null) row.setQuietBundle(true);
            notificationLogRepository.save(row);
            created++;
            if (status == NotificationStatus.SKIPPED) {
                skipped++;
                lastSkipReason = skipReason;
            }
            lastScheduled = scheduledAt;
        }

        if (skipped == created) {
            log.info("Xabarnoma SKIPPED: {} ref={} ({} ta qabul qiluvchi) — {}", type, referenceId, created, lastSkipReason);
        } else if (lastScheduled.isAfter(now)) {
            log.info("Xabarnoma navbatga qo'shildi: {} ref={} ({} ta, {} tasi o'tkazildi) — tinch soatlar, {} da yuboriladi",
                    type, referenceId, created, skipped, lastScheduled.toLocalTime());
        } else {
            log.info("Xabarnoma navbatga qo'shildi: {} ref={} ({} ta, {} tasi o'tkazildi)", type, referenceId, created, skipped);
        }
        return created;
    }

    /** Parent's own quiet hours win over the school's; "off" means deliver immediately. */
    LocalDateTime deliveryTime(LocalDateTime now, NotificationSettings school, ParentSession session) {
        if (session != null && session.getQuietHoursEnabled() != null) {
            if (!session.getQuietHoursEnabled()) return now;
            LocalTime start = session.getQuietStart() != null ? session.getQuietStart() : school.getQuietHoursStart();
            LocalTime end = session.getQuietEnd() != null ? session.getQuietEnd() : school.getQuietHoursEnd();
            return QuietHours.deliveryTime(now, start, end);
        }
        return Boolean.TRUE.equals(school.getQuietHoursEnabled())
                ? QuietHours.deliveryTime(now, school.getQuietHoursStart(), school.getQuietHoursEnd())
                : now;
    }

    private Map<Long, ParentSession> sessionsOf(List<Recipient> recipients) {
        Set<Long> chatIds = new HashSet<>();
        for (Recipient r : recipients) chatIds.add(r.chatId());
        Map<Long, ParentSession> map = new HashMap<>();
        List<ParentSession> found = sessionRepository.findByChatIdIn(chatIds);
        if (found != null) {
            for (ParentSession s : found) map.put(s.getChatId(), s);
        }
        return map;
    }

    public List<Recipient> recipientsOf(Student student) {
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

    public static School schoolOf(Student student) {
        return student.getSchoolClass().getAcademicYear().getSchool();
    }

    public static String fullName(Student student) {
        return student.getFirstName() + " " + student.getLastName();
    }

    public static String className(Student student) {
        return student.getSchoolClass().getGradeNumber() + "-" + student.getSchoolClass().getSectionLetter();
    }
}
