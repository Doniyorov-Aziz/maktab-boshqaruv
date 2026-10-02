package uz.azizbek.maktabboshqaruv.bot.image;

import uz.azizbek.maktabboshqaruv.bot.BotContext;
import uz.azizbek.maktabboshqaruv.bot.BotI18n;
import uz.azizbek.maktabboshqaruv.bot.CallbackData;
import uz.azizbek.maktabboshqaruv.bot.image.BannerRenderer.*;
import uz.azizbek.maktabboshqaruv.entity.ParentSession;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.service.parent.ParentDataService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentViews.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Builds the banner on top of every bot page: each section has its own color
 * and icon and shows the child plus the numbers that matter for that page
 * (attendance calendar, grade bars, tomorrow's lessons, ...). The banner is
 * cached by its content, and BotResponder reuses Telegram's file_id, so an
 * unchanged banner is drawn and uploaded only once.
 */
@Service
public class BannerService {

    /** Section → color. The first nine follow the design brief; the rest complete the palette. */
    public static final Map<String, Color> COLORS = Map.ofEntries(
            Map.entry("schedule", new Color(0x4F46E5)), Map.entry("attendance", new Color(0x10B981)),
            Map.entry("grades", new Color(0x0EA5E9)), Map.entry("report", new Color(0x7C3AED)),
            Map.entry("announcements", new Color(0xEC4899)), Map.entry("events", new Color(0xF59E0B)),
            Map.entry("teachers", new Color(0x06B6D4)), Map.entry("absence", new Color(0xEF4444)),
            Map.entry("settings", new Color(0x64748B)),
            Map.entry("home", new Color(0x6D28D9)), Map.entry("write", new Color(0x14B8A6)),
            Map.entry("behavior", new Color(0xF97316)), Map.entry("school", new Color(0x2563EB)),
            Map.entry("children", new Color(0x8B5CF6)), Map.entry("welcome", new Color(0x4F46E5)));

    private static final Map<String, Icon> ICONS = Map.ofEntries(
            Map.entry("home", Icon.HOME), Map.entry("schedule", Icon.CALENDAR), Map.entry("attendance", Icon.CHECK),
            Map.entry("grades", Icon.BOOK), Map.entry("report", Icon.CHART), Map.entry("announcements", Icon.MEGAPHONE),
            Map.entry("events", Icon.FLAG), Map.entry("teachers", Icon.PERSON), Map.entry("write", Icon.CHAT),
            Map.entry("absence", Icon.CROSS), Map.entry("settings", Icon.GEAR), Map.entry("children", Icon.FAMILY),
            Map.entry("behavior", Icon.STAR), Map.entry("school", Icon.SCHOOL), Map.entry("welcome", Icon.WAVE));

    /** Sections that get a banner. */
    public static final Set<String> SECTIONS = ICONS.keySet();

    private static final Color GREEN = new Color(0x10B981);
    private static final Color AMBER = new Color(0xF59E0B);
    private static final Color RED = new Color(0xEF4444);
    private static final Color BLUE = new Color(0x3B82F6);

    private final BotI18n i18n = BotI18n.get();

    @Autowired
    private ParentDataService data;

    @Autowired
    private BotImageService images;

    /** Banner for a page, or null when the section has none. */
    public BotImageService.Rendered banner(BotContext ctx, String section, CallbackData cb) {
        if (section == null || !SECTIONS.contains(section)) return null;
        if (!"welcome".equals(section) && ctx.student() == null) return null;
        Banner b = build(ctx, section, cb);
        return images.banner(section, b);
    }

    public Banner build(BotContext ctx, String section, CallbackData cb) {
        String lang = ctx.lang();
        if ("welcome".equals(section)) return welcome(lang);
        Student s = ctx.student();
        ChildInfo child = data.child(s);
        LocalDate today = data.today();
        String subtitle = child.fullName() + " · " + child.className();
        String footer = child.schoolName() + " · " + i18n.dateFull(lang, today);
        Color color = COLORS.get(section);
        Icon icon = ICONS.get(section);
        String title = t(lang, "banner." + section + ".title");

        return switch (section) {
            case "home" -> home(ctx, s, child, today, color, icon, footer);
            case "schedule" -> schedule(lang, s, today, color, icon, title, subtitle, footer);
            case "attendance" -> attendance(lang, s, cb, today, color, icon, title, subtitle, footer);
            case "grades" -> grades(lang, s, color, icon, title, subtitle, footer);
            case "report" -> report(lang, s, today, color, icon, title, subtitle, footer);
            case "behavior" -> behavior(lang, s, cb, today, color, icon, title, subtitle, footer);
            case "announcements" -> announcements(lang, s, ctx.session(), color, icon, title, subtitle, footer);
            case "events" -> events(lang, s, color, icon, title, subtitle, footer);
            case "teachers" -> teachers(lang, s, child, color, icon, title, subtitle, footer);
            case "write" -> write(lang, s, ctx.chatId(), color, icon, title, subtitle, footer);
            case "absence" -> absence(lang, s, ctx.chatId(), color, icon, title, subtitle, footer);
            case "school" -> school(lang, s, color, icon, title, footer);
            case "settings" -> settings(lang, ctx.session(), color, icon, title, subtitle, footer);
            case "children" -> children(lang, ctx.students(), color, icon, title, footer);
            default -> new Banner(color, icon, title, subtitle, null, null, footer, null, null);
        };
    }

    // ------------------------------------------------------------ sections

    private Banner welcome(String lang) {
        List<Row> rows = new ArrayList<>();
        for (int i = 1; i <= 4; i++) rows.add(new Row(String.valueOf(i), t(lang, "banner.welcome.f" + i), null));
        return new Banner(COLORS.get("welcome"), Icon.WAVE, t(lang, "banner.welcome.title"),
                t(lang, "banner.welcome.subtitle"), null, null, t(lang, "app.name"),
                new Rows(t(lang, "banner.welcome.heading"), rows, null), null);
    }

    private Banner home(BotContext ctx, Student s, ChildInfo child, LocalDate today, Color color, Icon icon, String footer) {
        String lang = ctx.lang();
        TodaySummary t = data.todaySummary(s, ctx.session());
        String state = t.schedule().hasLessons() ? t.attendanceState() : "NONE";
        Color stateColor = switch (state) {
            case "PRESENT" -> GREEN;
            case "LATE" -> AMBER;
            case "ABSENT" -> RED;
            case "EXCUSED" -> BLUE;
            default -> color;
        };
        List<Stat> stats = List.of(
                new Stat(t(lang, "banner.home.lessons"), String.valueOf(t.schedule().hasLessons() ? t.schedule().lessons().size() : 0), color),
                new Stat(t(lang, "banner.home.status"), t(lang, "banner.status." + state), stateColor),
                new Stat(t(lang, "banner.home.grades"), String.valueOf(t.gradesToday().size()), COLORS.get("grades")),
                new Stat(t(lang, "banner.home.news"), String.valueOf(t.newAnnouncements()), COLORS.get("announcements")));
        return new Banner(color, icon, t(lang, "banner.home.title"), child.fullName() + " · " + child.className(),
                null, null, child.schoolName() + " · " + i18n.dateFull(lang, today),
                new Stats(i18n.dateFull(lang, today), stats), null);
    }

    private Banner schedule(String lang, Student s, LocalDate today, Color color, Icon icon, String title,
                            String subtitle, String footer) {
        LocalDate tomorrow = today.plusDays(1);
        DaySchedule day = data.day(s, tomorrow);
        List<Row> rows = new ArrayList<>();
        String empty;
        if (day.holidayTitle() != null) {
            empty = t(lang, "banner.schedule.holiday", "title", day.holidayTitle());
        } else {
            empty = t(lang, "banner.schedule.empty");
            for (LessonView l : day.lessons()) {
                rows.add(new Row(l.start().toString(), l.number() + ". " + l.subject(), l.room()));
            }
        }
        return new Banner(color, icon, title, subtitle, String.valueOf(rows.size()), t(lang, "banner.schedule.big_label"),
                footer, new Rows(t(lang, "banner.schedule.heading", "date", i18n.dateFull(lang, tomorrow)), rows, empty), null);
    }

    private Banner attendance(String lang, Student s, CallbackData cb, LocalDate today, Color color, Icon icon,
                              String title, String subtitle, String footer) {
        YearMonth month = YearMonth.from(today);
        if (cb != null && cb.get("m") != null) {
            try {
                month = YearMonth.parse(cb.get("m"));
            } catch (RuntimeException ignored) {
                // malformed month in callback data: show the current one
            }
        }
        AttendanceSummary sum = data.attendance(s, "month", month.toString(), null);
        List<String> weekdays = new ArrayList<>();
        for (DayOfWeek d : DayOfWeek.values()) weekdays.add(i18n.dowShort(lang, d));
        String monthLabel = i18n.monthYear(lang, month.getMonthValue(), month.getYear());
        Map<String, String> legend = new LinkedHashMap<>();
        legend.put("PRESENT", t(lang, "img.legend.present"));
        legend.put("LATE", t(lang, "img.legend.late"));
        legend.put("ABSENT", t(lang, "img.legend.absent"));
        legend.put("EXCUSED", t(lang, "img.legend.excused"));
        String big = sum.total() == 0 ? "—" : fmt(sum.rate()) + "%";
        return new Banner(color, icon, title, subtitle, big, t(lang, "banner.attendance.big_label", "month", monthLabel),
                null, new BannerRenderer.Calendar(monthLabel, weekdays, month.atDay(1),
                sum.days().stream().map(AttendanceDay::status).toList(), today), legend);
    }

    private Banner grades(String lang, Student s, Color color, Icon icon, String title, String subtitle, String footer) {
        List<SubjectAverage> avgs = data.subjectAverages(s);
        List<Bar> bars = avgs.stream().map(a -> new Bar(a.subject(), a.average(), a.trend())).toList();
        double total = 0;
        int count = 0;
        for (SubjectAverage a : avgs) {
            total += a.average() * a.count();
            count += a.count();
        }
        String big = count == 0 ? "—" : String.format(Locale.ROOT, "%.1f", total / count);
        return new Banner(color, icon, title, subtitle, big, t(lang, "banner.grades.big_label"), footer,
                new Bars(t(lang, "banner.grades.heading"), bars, t(lang, "banner.grades.empty")), null);
    }

    private Banner report(String lang, Student s, LocalDate today, Color color, Icon icon, String title,
                          String subtitle, String footer) {
        LocalDate from = today.with(DayOfWeek.MONDAY);
        ReportView r = data.report(s, from, today, null);
        Double rate = r.attendance().total() == 0 ? null : r.attendance().rate();
        List<Stat> stats = List.of(
                new Stat(t(lang, "img.metric.attendance"), rate == null ? "—" : fmt(rate) + "%", rateColor(rate)),
                new Stat(t(lang, "img.metric.grades"), r.gradeAverage() == null ? "—"
                        : String.format(Locale.ROOT, "%.2f", r.gradeAverage()), COLORS.get("grades")),
                new Stat(t(lang, "img.metric.rewards"), String.valueOf(r.rewards()), GREEN),
                new Stat(t(lang, "img.metric.warnings"), String.valueOf(r.warnings()), r.warnings() > 0 ? AMBER : COLORS.get("settings")));
        String period = i18n.dateShort(lang, from) + " – " + i18n.dateShort(lang, today);
        return new Banner(color, icon, title, subtitle, null, null, footer,
                new Stats(t(lang, "banner.report.heading", "period", period), stats), null);
    }

    private Banner behavior(String lang, Student s, CallbackData cb, LocalDate today, Color color, Icon icon,
                            String title, String subtitle, String footer) {
        YearMonth month = YearMonth.from(today);
        if (cb != null && cb.get("m") != null) {
            try {
                month = YearMonth.parse(cb.get("m"));
            } catch (RuntimeException ignored) {
                // keep the current month
            }
        }
        List<BehaviorView> list = data.behavior(s, month.atDay(1), month.atEndOfMonth());
        long rewards = list.stream().filter(b -> "REWARD".equals(b.type())).count();
        List<Stat> stats = List.of(
                new Stat(t(lang, "img.metric.rewards"), String.valueOf(rewards), GREEN),
                new Stat(t(lang, "img.metric.warnings"), String.valueOf(list.size() - rewards), AMBER));
        return new Banner(color, icon, title, subtitle, null, null, footer,
                new Stats(i18n.monthYear(lang, month.getMonthValue(), month.getYear()), stats), null);
    }

    private Banner announcements(String lang, Student s, ParentSession session, Color color, Icon icon, String title,
                                 String subtitle, String footer) {
        List<AnnouncementView> list = data.announcements(s, session);
        long unread = list.stream().filter(AnnouncementView::unread).count();
        List<Row> rows = list.stream().limit(5)
                .map(a -> new Row(i18n.dateShort(lang, a.date()), (a.important() ? "! " : "") + a.title(),
                        a.unread() ? t(lang, "banner.ann.new") : null))
                .toList();
        return new Banner(color, icon, title, subtitle, String.valueOf(unread), t(lang, "banner.ann.big_label"), footer,
                new Rows(t(lang, "banner.ann.heading"), rows, t(lang, "banner.ann.empty")), null);
    }

    private Banner events(String lang, Student s, Color color, Icon icon, String title, String subtitle, String footer) {
        List<EventView> list = data.upcomingEvents(s, 60);
        List<Row> rows = list.stream().limit(6)
                .map(e -> new Row(i18n.dateShort(lang, e.startDate()), e.title(), null)).toList();
        return new Banner(color, icon, title, subtitle, String.valueOf(list.size()), t(lang, "banner.events.big_label"),
                footer, new Rows(t(lang, "banner.events.heading"), rows, t(lang, "banner.events.empty")), null);
    }

    private Banner teachers(String lang, Student s, ChildInfo child, Color color, Icon icon, String title,
                            String subtitle, String footer) {
        List<TeacherView> list = data.teachers(s);
        List<Row> rows = list.stream().filter(t -> !t.classTeacher()).limit(6)
                .map(t -> new Row(null, t.name(), t.subject())).toList();
        String heading = child.classTeacher() == null ? t(lang, "banner.teachers.heading")
                : t(lang, "banner.teachers.class_teacher", "name", child.classTeacher());
        return new Banner(color, icon, title, subtitle, String.valueOf(rows.size()), t(lang, "banner.teachers.big_label"),
                footer, new Rows(heading, rows, t(lang, "banner.teachers.empty")), null);
    }

    private Banner write(String lang, Student s, long chatId, Color color, Icon icon, String title, String subtitle,
                         String footer) {
        List<MessageView> list = data.messages(s, chatId);
        long answered = list.stream().filter(m -> "ANSWERED".equals(m.status())).count();
        List<Stat> stats = List.of(
                new Stat(t(lang, "banner.write.sent"), String.valueOf(list.size()), color),
                new Stat(t(lang, "banner.write.answered"), String.valueOf(answered), GREEN));
        return new Banner(color, icon, title, subtitle, null, null, footer,
                new Stats(t(lang, "banner.write.heading"), stats), null);
    }

    private Banner absence(String lang, Student s, long chatId, Color color, Icon icon, String title, String subtitle,
                           String footer) {
        List<AbsenceView> list = data.absenceRequests(s, chatId);
        List<Stat> stats = List.of(
                new Stat(t(lang, "banner.absence.sent"), String.valueOf(list.size()), color),
                new Stat(t(lang, "banner.absence.approved"), String.valueOf(list.stream().filter(a -> "APPROVED".equals(a.status())).count()), GREEN),
                new Stat(t(lang, "banner.absence.pending"), String.valueOf(list.stream().filter(a -> "PENDING".equals(a.status())).count()), AMBER),
                new Stat(t(lang, "banner.absence.rejected"), String.valueOf(list.stream().filter(a -> "REJECTED".equals(a.status())).count()), COLORS.get("settings")));
        return new Banner(color, icon, title, subtitle, null, null, footer,
                new Stats(t(lang, "banner.absence.heading"), stats), null);
    }

    private Banner school(String lang, Student s, Color color, Icon icon, String title, String footer) {
        SchoolInfo info = data.school(s);
        List<Row> rows = new ArrayList<>();
        if (info.phone() != null && !info.phone().isBlank()) rows.add(new Row(t(lang, "banner.school.phone"), info.phone(), null));
        if (info.director() != null && !info.director().isBlank()) rows.add(new Row(t(lang, "banner.school.director"), info.director(), null));
        if (info.receptionHours() != null && !info.receptionHours().isBlank()) rows.add(new Row(t(lang, "banner.school.reception"), info.receptionHours(), null));
        if (info.address() != null && !info.address().isBlank()) rows.add(new Row(t(lang, "banner.school.address"), info.address(), null));
        if (!info.bells().isEmpty()) {
            BellView first = info.bells().get(0), last = info.bells().get(info.bells().size() - 1);
            rows.add(new Row(t(lang, "banner.school.lessons"), first.start() + " – " + last.end(), null));
        }
        return new Banner(color, icon, title, info.name(), null, null, footer,
                new Rows(t(lang, "banner.school.heading"), rows, t(lang, "banner.school.empty")), null);
    }

    private Banner settings(String lang, ParentSession session, Color color, Icon icon, String title, String subtitle,
                            String footer) {
        List<Boolean> toggles = Arrays.asList(session.getNotifyAbsence(), session.getNotifyGrades(), session.getNotifyLowGrade(),
                session.getNotifyAnnouncements(), session.getNotifyTomorrowSchedule(), session.getNotifyWeeklyReport(),
                session.getNotifyEventReminder());
        long on = toggles.stream().filter(b -> b == null || b).count();
        String time = session.getScheduleTime() != null ? session.getScheduleTime().toString() : t(lang, "banner.settings.school_time");
        List<Stat> stats = List.of(
                new Stat(t(lang, "banner.settings.notifications"), on + "/" + toggles.size(), on == toggles.size() ? GREEN : AMBER),
                new Stat(t(lang, "banner.settings.language"), t(lang, "banner.settings.lang_name"), COLORS.get("schedule")),
                new Stat(t(lang, "banner.settings.schedule_time"), time, COLORS.get("events")),
                new Stat(t(lang, "banner.settings.quiet"), Boolean.FALSE.equals(session.getQuietHoursEnabled())
                        ? t(lang, "banner.settings.off") : t(lang, "banner.settings.on"), color));
        return new Banner(color, icon, title, subtitle, null, null, footer,
                new Stats(t(lang, "banner.settings.heading"), stats), null);
    }

    private Banner children(String lang, List<Student> students, Color color, Icon icon, String title, String footer) {
        List<Row> rows = students.stream()
                .map(st -> new Row(ParentDataService.className(st.getSchoolClass()), ParentDataService.fullName(st), null))
                .toList();
        return new Banner(color, icon, title, t(lang, "banner.children.subtitle"), String.valueOf(students.size()),
                t(lang, "banner.children.big_label"), footer,
                new Rows(t(lang, "banner.children.heading"), rows, null), null);
    }

    // ------------------------------------------------------------ helpers

    private String t(String lang, String key, Object... args) {
        return i18n.t(lang, key, args);
    }

    private static String fmt(Double v) {
        if (v == null) return "—";
        return v == Math.floor(v) ? String.valueOf(v.intValue()) : String.format(Locale.ROOT, "%.1f", v);
    }

    private static Color rateColor(Double rate) {
        if (rate == null) return COLORS.get("settings");
        return rate >= 90 ? GREEN : rate >= 75 ? AMBER : RED;
    }
}
