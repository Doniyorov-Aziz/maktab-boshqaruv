package uz.azizbek.maktabboshqaruv.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.azizbek.maktabboshqaruv.bot.BotI18n;
import uz.azizbek.maktabboshqaruv.bot.SubjectIcons;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.*;
import uz.azizbek.maktabboshqaruv.telegram.MessageFormatter;
import uz.azizbek.maktabboshqaruv.telegram.TelegramProperties;

import java.time.*;
import java.util.*;

/**
 * The school modules' scheduled messages (every 5 minutes, like {@link BotScheduledJobs}; a message is due from its
 * time until 3 hours later and the outbox key sends it once):
 * <ul>
 *   <li>"📝 Ertaga nazorat ishi" the day before an exam, at the school's event-reminder time (18:00);</li>
 *   <li>"📅 Ertaga uchrashuv" to a parent who booked a teacher's time for tomorrow, at the same time;</li>
 *   <li>"💳 To'lov eslatmasi" the day after the month's due day at 10:00, when the school switched it on.</li>
 * </ul>
 */
@Service
public class ModuleJobs {

    private static final Logger log = LoggerFactory.getLogger(ModuleJobs.class);
    private static final Duration WINDOW = Duration.ofHours(3);
    static final LocalTime PAYMENT_TIME = LocalTime.of(10, 0);

    private final TelegramProperties properties;
    private final ExamRepository exams;
    private final MeetingSlotRepository meetings;
    private final ParentTelegramLinkRepository links;
    private final NotificationService notifications;
    private final BotSettingService botSettings;
    private final PaymentSettingsRepository paymentSettings;
    private final PaymentService payments;
    private final Clock clock;
    private final BotI18n i18n = BotI18n.get();

    public ModuleJobs(TelegramProperties properties, ExamRepository exams, MeetingSlotRepository meetings,
                      ParentTelegramLinkRepository links, NotificationService notifications, BotSettingService botSettings,
                      PaymentSettingsRepository paymentSettings, PaymentService payments, Clock clock) {
        this.properties = properties;
        this.exams = exams;
        this.meetings = meetings;
        this.links = links;
        this.notifications = notifications;
        this.botSettings = botSettings;
        this.paymentSettings = paymentSettings;
        this.payments = payments;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${telegram.jobs-interval-ms:300000}", initialDelayString = "${telegram.jobs-initial-delay-ms:60000}")
    public void tick() {
        if (!properties.isActive()) return;
        LocalDateTime now = LocalDateTime.now(clock);
        try {
            runExamReminders(now, false);
            runMeetingReminders(now, false);
            runPaymentReminders(now, false);
        } catch (Exception e) {
            log.error("Modul eslatmalarida xato", e);
        }
    }

    /** Tomorrow's exams: one message per parent and exam. */
    @Transactional
    public int runExamReminders(LocalDateTime now, boolean force) {
        LocalDate tomorrow = now.toLocalDate().plusDays(1);
        int queued = 0;
        for (Exam x : exams.onDateToRemind(tomorrow)) {
            School school = x.getSchool();
            if (!force && !due(now, now.toLocalDate().atTime(botSettings.getOrDefault(school).getEventReminderTime()))) continue;
            String subject = x.getSubject().getName();
            String kind = x.getKind().name();
            String title = x.getTitle();
            String time = x.getStartTime() == null ? "" : x.getStartTime().toString();
            String place = x.getPlace();
            for (NotificationService.Recipient r : NotificationService.oncePerChat(links.findActiveByClassId(x.getSchoolClass().getId()))) {
                queued += notifications.enqueueDirect(school, r.student(), r.chatId(), NotificationType.EXAM_REMINDER, x.getId(),
                        x.getExamDate(), lang -> i18n.t(lang, "notif.exam_reminder",
                                "kind", i18n.t(lang, "exam.kind." + kind), "emoji", SubjectIcons.subject(subject),
                                "subject", MessageFormatter.escape(subject),
                                "title", title == null ? "" : " — " + MessageFormatter.escape(title),
                                "date", i18n.dateFull(lang, tomorrow),
                                "time", time.isEmpty() ? "" : " · 🕘 " + time,
                                "place", place == null ? "" : " · 📍 " + MessageFormatter.escape(place))
                                + MessageFormatter.footer(lang, school.getName()),
                        null);
            }
        }
        if (queued > 0) log.info("Imtihon eslatmasi: {} ta xabar navbatga qo'shildi", queued);
        return queued;
    }

    /** Tomorrow's booked meetings: the parent who booked. */
    @Transactional
    public int runMeetingReminders(LocalDateTime now, boolean force) {
        LocalDate tomorrow = now.toLocalDate().plusDays(1);
        int queued = 0;
        for (MeetingSlot m : meetings.bookedOn(tomorrow)) {
            School school = m.getSchool();
            if (!force && !due(now, now.toLocalDate().atTime(botSettings.getOrDefault(school).getEventReminderTime()))) continue;
            String teacher = (m.getEmployee().getLastName() + " " + m.getEmployee().getFirstName()).strip();
            String time = m.getStartTime() + "–" + m.getEndTime();
            String place = m.getPlace();
            queued += notifications.enqueueDirect(school, m.getStudent(), m.getChatId(), NotificationType.MEETING, m.getId(),
                    tomorrow, lang -> i18n.t(lang, "notif.meeting_reminder", "teacher", MessageFormatter.escape(teacher),
                            "date", i18n.dateFull(lang, tomorrow), "time", time,
                            "place", place == null ? "" : "\n📍 " + MessageFormatter.escape(place))
                            + MessageFormatter.footer(lang, school.getName()),
                    null);
        }
        if (queued > 0) log.info("Uchrashuv eslatmasi: {} ta xabar navbatga qo'shildi", queued);
        return queued;
    }

    /** The day after the due day, 10:00: every school with payments on and reminders on. */
    public int runPaymentReminders(LocalDateTime now, boolean force) {
        int queued = 0;
        LocalDate today = now.toLocalDate();
        for (PaymentSettings s : paymentSettings.findByEnabledTrue()) {
            if (!s.isRemindParents() || s.getMonthlyFee() <= 0) continue;
            int dueDay = Math.min(s.getDueDay(), YearMonth.from(today).lengthOfMonth());
            if (!force && (today.getDayOfMonth() != dueDay + 1 || !due(now, today.atTime(PAYMENT_TIME)))) continue;
            try {
                queued += payments.remindAsJob(s.getSchool().getId());
            } catch (RuntimeException e) {
                log.warn("To'lov eslatmasi (maktab {}): {}", s.getSchool().getId(), e.getMessage());
            }
        }
        if (queued > 0) log.info("To'lov eslatmasi: {} ta xabar navbatga qo'shildi", queued);
        return queued;
    }

    private static boolean due(LocalDateTime now, LocalDateTime at) {
        return !now.isBefore(at) && now.isBefore(at.plus(WINDOW));
    }
}
