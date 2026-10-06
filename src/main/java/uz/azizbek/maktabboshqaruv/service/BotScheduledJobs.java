package uz.azizbek.maktabboshqaruv.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.azizbek.maktabboshqaruv.bot.BotI18n;
import uz.azizbek.maktabboshqaruv.bot.SubjectIcons;
import uz.azizbek.maktabboshqaruv.bot.screens.ReportScreen;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.CalendarEventRepository;
import uz.azizbek.maktabboshqaruv.repository.NotificationLogRepository;
import uz.azizbek.maktabboshqaruv.repository.ParentSessionRepository;
import uz.azizbek.maktabboshqaruv.repository.ParentTelegramLinkRepository;
import uz.azizbek.maktabboshqaruv.service.parent.ParentDataService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentViews.*;
import uz.azizbek.maktabboshqaruv.telegram.MessageFormatter;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineButton;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineKeyboardMarkup;
import uz.azizbek.maktabboshqaruv.telegram.TelegramProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Automatic digests, all through the outbox (so dedup, parent switches,
 * language and quiet hours apply exactly as for event-driven messages):
 *
 *  - tomorrow's timetable at the parent's chosen time (default 19:00, school setting),
 *    with a reminder of tomorrow's parent meeting / exam, or a "no lessons — holiday" note;
 *  - the weekly report on the school's report day and time (default Saturday 18:00);
 *  - a reminder one day before each calendar event (default at 18:00).
 *
 * Runs every 5 minutes; a message is due from its time until 3 hours later,
 * and the outbox dedup key makes each one go out once.
 */
@Service
public class BotScheduledJobs {

    private static final Logger log = LoggerFactory.getLogger(BotScheduledJobs.class);
    private static final Duration WINDOW = Duration.ofHours(3);

    @Autowired
    private TelegramProperties properties;
    @Autowired
    private ParentTelegramLinkRepository linkRepository;
    @Autowired
    private ParentSessionRepository sessionRepository;
    @Autowired
    private NotificationLogRepository notificationLogRepository;
    @Autowired
    private CalendarEventRepository calendarEventRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private BotSettingService botSettingService;
    @Autowired
    private ParentDataService data;
    @Autowired
    private WeeklyReportCardService weeklyCards;
    @Autowired
    private Clock clock;

    private final BotI18n i18n = BotI18n.get();

    @Scheduled(fixedDelayString = "${telegram.jobs-interval-ms:300000}", initialDelayString = "${telegram.jobs-initial-delay-ms:60000}")
    public void tick() {
        if (!properties.isActive()) return;
        LocalDateTime now = LocalDateTime.now(clock);
        try {
            runMorningDigests(now, false);
            runTomorrowSchedules(now, false);
            runWeeklyReports(now, false);
            runEventReminders(now, false);
        } catch (Exception e) {
            log.error("Rejalashtirilgan xabarnomalarda xato", e);
        }
    }

    /**
     * "☀️ Xayrli tong!" on school days at the school's digest time (07:30 by
     * default): today's lessons, the first one, and today's events and news.
     * Never on Sunday, a holiday or a day without lessons.
     * @param force ignore the time window (used by the mock demo); Sundays and holidays still count.
     */
    public int runMorningDigests(LocalDateTime now, boolean force) {
        LocalDate today = now.toLocalDate();
        if (today.getDayOfWeek() == DayOfWeek.SUNDAY) return 0;
        int queued = 0;
        Map<Long, ParentSession> sessions = sessions();
        for (ParentTelegramLink link : linkRepository.findAllActive()) {
            Student s = link.getStudent();
            ParentSession session = sessions.get(link.getChatId());
            if (session != null && !session.wants(NotificationType.MORNING_DIGEST)) continue;
            School school = NotificationService.schoolOf(s);
            LocalTime at = botSettingService.getOrDefault(school).getMorningDigestTime();
            if (!force && !due(now, today.atTime(at))) continue;
            if (notificationLogRepository.existsByChatIdAndTypeAndReferenceIdAndRecordDate(
                    link.getChatId(), NotificationType.MORNING_DIGEST, s.getId(), today)) continue;

            DaySchedule day = data.day(s, today);
            if (!day.hasLessons()) continue; // holiday, vacation or no timetable today

            List<String> news = data.announcements(s, session).stream()
                    .filter(a -> today.equals(a.date())).map(AnnouncementView::title).limit(2).toList();
            queued += notificationService.enqueueDirect(school, s, link.getChatId(), NotificationType.MORNING_DIGEST,
                    s.getId(), today, lang -> morningText(lang, s, day, news, school),
                    lang -> appButton(lang, "notif.btn.open_app", "", "home"));
        }
        if (queued > 0) log.info("Ertalabki digest: {} ta xabar navbatga qo'shildi", queued);
        return queued;
    }

    String morningText(String lang, Student s, DaySchedule day, List<String> news, School school) {
        LessonView first = day.lessons().get(0);
        String events = day.events().isEmpty() ? "" : i18n.t(lang, "notif.morning_events", "list", day.events().stream()
                .map(e -> SubjectIcons.event(e.type()) + " " + MessageFormatter.escape(e.title())).collect(Collectors.joining(", ")));
        String newsLine = news.isEmpty() ? "" : i18n.t(lang, "notif.morning_news", "list",
                news.stream().map(MessageFormatter::escape).collect(Collectors.joining(", ")));
        return i18n.t(lang, "notif.morning", "name", MessageFormatter.escape(s.getFirstName()),
                "count", day.lessons().size(), "start", first.start(),
                "emoji", SubjectIcons.subject(first.subject()), "subject", MessageFormatter.escape(first.subject()),
                "events", events, "news", newsLine) + MessageFormatter.footer(lang, school.getName());
    }

    /** "📱 …" — the Mini App page when TELEGRAM_WEBAPP_URL is set, otherwise the bot page as a new card. */
    InlineKeyboardMarkup appButton(String lang, String labelKey, String webAppPage, String botScreen) {
        String webApp = properties.webappUrlIfValid();
        InlineButton button = webApp != null
                ? InlineButton.webApp(i18n.t(lang, labelKey), webApp + webAppPage)
                : InlineButton.callback(i18n.t(lang, labelKey), botScreen + ":n:1");
        return InlineKeyboardMarkup.builder().row(button).build();
    }

    /** @param force ignore the time window (used by the mock demo). */
    public int runTomorrowSchedules(LocalDateTime now, boolean force) {
        LocalDate tomorrow = now.toLocalDate().plusDays(1);
        int queued = 0;
        Map<Long, ParentSession> sessions = sessions();
        for (ParentTelegramLink link : linkRepository.findAllActive()) {
            Student s = link.getStudent();
            ParentSession session = sessions.get(link.getChatId());
            if (session != null && !session.wants(NotificationType.TOMORROW_SCHEDULE)) continue;
            School school = NotificationService.schoolOf(s);
            LocalTime at = session != null && session.getScheduleTime() != null ? session.getScheduleTime()
                    : botSettingService.getOrDefault(school).getTomorrowScheduleTime();
            if (!force && !due(now, now.toLocalDate().atTime(at))) continue;
            if (notificationLogRepository.existsByChatIdAndTypeAndReferenceIdAndRecordDate(
                    link.getChatId(), NotificationType.TOMORROW_SCHEDULE, s.getId(), tomorrow)) continue;

            DaySchedule day = data.day(s, tomorrow);
            if (day.holidayTitle() == null && day.lessons().isEmpty()) continue; // Sunday, or nothing scheduled

            // "ertaga kerakli narsalar": announcements whose deadline is tomorrow
            List<String> needs = data.announcements(s, session).stream()
                    .filter(a -> tomorrow.equals(a.deadline())).map(AnnouncementView::title).limit(3).toList();
            queued += notificationService.enqueueDirect(school, s, link.getChatId(), NotificationType.TOMORROW_SCHEDULE,
                    s.getId(), tomorrow, lang -> tomorrowText(lang, s, day, school) + needsLine(lang, needs),
                    lang -> InlineKeyboardMarkup.builder().row(InlineButton.callback(i18n.t(lang, "notif.btn.schedule"),
                            "sch:t:week:s:" + s.getId() + ":n:1")).build());
        }
        if (queued > 0) log.info("Ertangi dars jadvali: {} ta xabar navbatga qo'shildi", queued);
        return queued;
    }

    String tomorrowText(String lang, Student s, DaySchedule day, School school) {
        String name = MessageFormatter.escape(ParentDataService.fullName(s));
        String cls = MessageFormatter.escape(ParentDataService.className(s.getSchoolClass()));
        String date = i18n.dateFull(lang, day.date());
        if (day.holidayTitle() != null) {
            return i18n.t(lang, "notif.tomorrow_holiday", "title", MessageFormatter.escape(day.holidayTitle()),
                    "name", name, "class", cls) + MessageFormatter.footer(lang, school.getName());
        }
        String lessons = day.lessons().stream()
                .map(l -> i18n.t(lang, "notif.tomorrow_line", "n", l.number(), "start", l.start(),
                        "emoji", SubjectIcons.subject(l.subject()), "subject", MessageFormatter.escape(l.subject())))
                .collect(Collectors.joining("\n"));
        List<EventView> notable = day.events().stream()
                .filter(e -> "PARENT_MEETING".equals(e.type()) || "EXAM".equals(e.type()) || "OTHER".equals(e.type())).toList();
        String events = notable.isEmpty() ? "" : i18n.t(lang, "notif.tomorrow_events", "list", notable.stream()
                .map(e -> SubjectIcons.event(e.type()) + " " + MessageFormatter.escape(e.title())).collect(Collectors.joining(", ")));
        return i18n.t(lang, "notif.tomorrow", "date", date, "name", name, "class", cls, "lessons", lessons, "events", events)
                + MessageFormatter.footer(lang, school.getName());
    }

    private String needsLine(String lang, List<String> needs) {
        if (needs.isEmpty()) return "";
        return "\n" + i18n.t(lang, "notif.tomorrow_needs", "list",
                needs.stream().map(MessageFormatter::escape).collect(Collectors.joining(", ")));
    }

    public int runWeeklyReports(LocalDateTime now, boolean force) {
        LocalDate today = now.toLocalDate();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        int queued = 0;
        Map<Long, ParentSession> sessions = sessions();
        for (ParentTelegramLink link : linkRepository.findAllActive()) {
            Student s = link.getStudent();
            ParentSession session = sessions.get(link.getChatId());
            if (session != null && !session.wants(NotificationType.WEEKLY_REPORT)) continue;
            School school = NotificationService.schoolOf(s);
            BotSetting setting = botSettingService.getOrDefault(school);
            if (!force && (today.getDayOfWeek() != setting.getWeeklyReportDay()
                    || !due(now, today.atTime(setting.getWeeklyReportTime())))) continue;
            if (notificationLogRepository.existsByChatIdAndTypeAndReferenceIdAndRecordDate(
                    link.getChatId(), NotificationType.WEEKLY_REPORT, s.getId(), weekStart)) continue;

            // A 1080×1350 picture card (drawn when sent) with a short caption and "📱 Batafsil".
            WeeklyReportCardService.WeeklyFacts facts = weeklyCards.facts(s, weekStart, today);
            queued += notificationService.enqueueDirectImage(school, s, link.getChatId(), NotificationType.WEEKLY_REPORT,
                    s.getId(), weekStart, lang -> weeklyCaption(lang, s, weekStart, today, facts, school),
                    lang -> appButton(lang, "notif.btn.app_details", "/grades", "rep:t:week:s:" + s.getId()),
                    lang -> WeeklyReportCardService.reference(s.getId(), weekStart, today, lang));
        }
        if (queued > 0) log.info("Haftalik hisobot: {} ta xabar navbatga qo'shildi", queued);
        return queued;
    }

    /** Short caption under the weekly picture (Telegram allows 1024 characters). */
    String weeklyCaption(String lang, Student s, LocalDate from, LocalDate to, WeeklyReportCardService.WeeklyFacts f,
                         School school) {
        String none = i18n.t(lang, "img.none");
        return i18n.t(lang, "notif.weekly_caption",
                "period", i18n.dateShort(lang, from) + " – " + i18n.dateShort(lang, to),
                "name", MessageFormatter.escape(ParentDataService.fullName(s)),
                "class", MessageFormatter.escape(ParentDataService.className(s.getSchoolClass())),
                "rate", f.attendanceRate() == null ? none : String.format(Locale.ROOT, "%.0f%%", f.attendanceRate()),
                "avg", f.gradeAverage() == null ? none : String.format(Locale.ROOT, "%.2f", f.gradeAverage()),
                "best", f.best() == null ? none : MessageFormatter.escape(f.best()),
                "attention", f.attention() == null ? none : MessageFormatter.escape(f.attention()))
                + MessageFormatter.footer(lang, school.getName());
    }

    public int runEventReminders(LocalDateTime now, boolean force) {
        LocalDate tomorrow = now.toLocalDate().plusDays(1);
        int queued = 0;
        Map<Long, ParentSession> sessions = sessions();
        Map<Long, List<ParentTelegramLink>> bySchool = linkRepository.findAllActive().stream()
                .collect(Collectors.groupingBy(l -> NotificationService.schoolOf(l.getStudent()).getId()));
        for (List<ParentTelegramLink> links : bySchool.values()) {
            School school = NotificationService.schoolOf(links.get(0).getStudent());
            BotSetting setting = botSettingService.getOrDefault(school);
            if (!force && !due(now, now.toLocalDate().atTime(setting.getEventReminderTime()))) continue;
            List<CalendarEvent> events = calendarEventRepository.findInRange(school.getId(), tomorrow, tomorrow).stream()
                    .filter(e -> e.getStartDate().equals(tomorrow)).toList();
            for (CalendarEvent ev : events) {
                for (NotificationService.Recipient r : NotificationService.oncePerChat(links)) {
                    ParentSession session = sessions.get(r.chatId());
                    if (session != null && !session.wants(NotificationType.EVENT_REMINDER)) continue;
                    queued += notificationService.enqueueDirect(school, r.student(), r.chatId(), NotificationType.EVENT_REMINDER,
                            ev.getId(), ev.getStartDate(), lang -> i18n.t(lang, "notif.event_reminder",
                                    "emoji", SubjectIcons.event(ev.getType().name()), "title", MessageFormatter.escape(ev.getTitle()),
                                    "date", i18n.dateFull(lang, ev.getStartDate()),
                                    "description", ev.getDescription() == null || ev.getDescription().isBlank() ? ""
                                            : "\n<blockquote>" + MessageFormatter.escape(ev.getDescription()) + "</blockquote>")
                                    + MessageFormatter.footer(lang, school.getName()),
                            null);
                }
            }
        }
        if (queued > 0) log.info("Tadbir eslatmasi: {} ta xabar navbatga qo'shildi", queued);
        return queued;
    }

    private static boolean due(LocalDateTime now, LocalDateTime at) {
        return !now.isBefore(at) && now.isBefore(at.plus(WINDOW));
    }

    private Map<Long, ParentSession> sessions() {
        Map<Long, ParentSession> map = new HashMap<>();
        for (ParentSession s : sessionRepository.findAll()) map.put(s.getChatId(), s);
        return map;
    }
}
