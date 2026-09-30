package uz.azizbek.maktabboshqaruv.bot.screens;

import uz.azizbek.maktabboshqaruv.bot.*;
import uz.azizbek.maktabboshqaruv.bot.image.BotImageService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentDataService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentStats;
import uz.azizbek.maktabboshqaruv.service.parent.ParentViews.*;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineButton;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineKeyboardMarkup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static uz.azizbek.maktabboshqaruv.bot.BotContext.e;

/**
 * ✅ Davomat. Period kinds: k=m (month m=YYYY-MM), k=q (quarter q=1..4), k=y (school year).
 * Views: v=sum (summary), cal (calendar), det (absences/lateness list, paged), sub (by subject), img (PNG calendar).
 */
@Component
public class AttendanceScreen implements Screen {

    private static final int PAGE = 8;
    static final Map<String, String> DAY_EMOJI = Map.of(
            "PRESENT", "🟩", "LATE", "🟨", "ABSENT", "🟥", "EXCUSED", "🟦", "NONE", "⬜");

    @Autowired
    private ParentDataService data;

    @Autowired
    private BotImageService images;

    @Override
    public String code() {
        return "att";
    }

    @Override
    public BotView render(BotContext ctx, CallbackData cb) {
        String kind = cb.get("k", "m");
        YearMonth thisMonth = YearMonth.from(data.today());
        YearMonth month = parseMonth(cb.get("m"), thisMonth);
        int quarter = cb.getInt("q", ParentStats.quarterOf(data.today()));
        String view = cb.get("v", "sum");

        String kindCode = switch (kind) {
            case "q" -> "quarter";
            case "y" -> "year";
            default -> "month";
        };
        String value = "quarter".equals(kindCode) ? String.valueOf(quarter) : month.toString();
        String label = periodLabel(ctx, kindCode, month, quarter);
        AttendanceSummary summary = data.attendance(ctx.student(), kindCode, value, label);
        CallbackData base = CallbackData.of("att").with("k", kind)
                .with("m", "m".equals(kind) ? month.toString() : null)
                .with("q", "q".equals(kind) ? quarter : null);

        return switch (view) {
            case "cal" -> calendar(ctx, summary, month, base);
            case "det" -> details(ctx, summary, base, cb.getInt("p", 0));
            case "sub" -> bySubject(ctx, summary, base);
            case "img" -> image(ctx, month, base);
            default -> summaryView(ctx, summary, kind, month, quarter, thisMonth, base);
        };
    }

    private BotView summaryView(BotContext ctx, AttendanceSummary s, String kind, YearMonth month, int quarter,
                                YearMonth thisMonth, CallbackData base) {
        StringBuilder sb = new StringBuilder(ctx.crumb(ctx.t("sec.attendance"), s.period())).append("\n")
                .append(ctx.childHeader()).append("\n");
        if (s.total() == 0) {
            sb.append("<b>").append(s.period()).append("</b>\n\n").append(ctx.t("att.empty"));
        } else {
            sb.append(ctx.t("att.summary", "period", s.period(), "total", s.total(), "present", s.present(),
                    "late", s.late(), "absent", s.absent(), "excused", s.excused(),
                    "bar", ParentStats.progressBar(s.rate(), 10), "rate", fmt(s.rate())));
            if (s.classRate() != null) {
                String cmp = s.rate() > s.classRate() + 1 ? "att.cmp.above"
                        : s.rate() < s.classRate() - 1 ? "att.cmp.below" : "att.cmp.equal";
                sb.append("\n").append(ctx.t("att.class_avg", "rate", fmt(s.classRate()), "cmp", ctx.t(cmp)));
            }
        }

        InlineKeyboardMarkup.Builder kb = InlineKeyboardMarkup.builder();
        if ("m".equals(kind)) {
            YearMonth prev = month.minusMonths(1);
            YearMonth next = month.plusMonths(1);
            kb.row(ctx.btn("◀️ " + ctx.t("month." + prev.getMonthValue()), base.with("m", prev.toString())),
                    ctx.btn(ctx.i18n().monthYear(ctx.lang(), month.getMonthValue(), month.getYear()), CallbackData.of("noop")),
                    next.isAfter(thisMonth) ? ctx.blank()
                            : ctx.btn(ctx.t("month." + next.getMonthValue()) + " ▶️", base.with("m", next.toString())));
        } else if ("q".equals(kind)) {
            kb.row(quarter > 1 ? ctx.btn("◀️ " + ctx.t("att.period.quarter", "n", quarter - 1), base.with("q", quarter - 1)) : ctx.blank(),
                    ctx.btn(ctx.t("att.period.quarter", "n", quarter), CallbackData.of("noop")),
                    quarter < 4 ? ctx.btn(ctx.t("att.period.quarter", "n", quarter + 1) + " ▶️", base.with("q", quarter + 1)) : ctx.blank());
        }
        kb.row(ctx.btn(ctx.t("att.btn.calendar"), base.with("v", "cal")), ctx.btn(ctx.t("att.btn.details"), base.with("v", "det")));
        kb.row(ctx.btn(ctx.t("att.btn.subjects"), base.with("v", "sub")),
                "m".equals(kind) ? ctx.btn(ctx.t("common.image"), base.with("v", "img")) : null);
        kb.row(kindBtn(ctx, "m", "att.btn.month", kind), kindBtn(ctx, "q", "att.btn.quarter", kind), kindBtn(ctx, "y", "att.btn.year", kind));
        kb.row(ctx.switchRow("att").toArray(new InlineButton[0]));
        kb.row(ctx.navRow(null).toArray(new InlineButton[0]));
        return BotView.of(sb.toString(), kb.build()).section("attendance");
    }

    private InlineButton kindBtn(BotContext ctx, String kind, String key, String current) {
        String label = ctx.t(key);
        return ctx.btn(kind.equals(current) ? "• " + label + " •" : label, CallbackData.of("att").with("k", kind));
    }

    /** Emoji calendar: one row per week (Monday first), "01–06" label, one square per day. */
    private BotView calendar(BotContext ctx, AttendanceSummary s, YearMonth month, CallbackData base) {
        StringBuilder sb = new StringBuilder(ctx.crumb(ctx.t("sec.attendance"), s.period(), ctx.t("att.btn.calendar"))).append("\n")
                .append(ctx.childHeader()).append("\n")
                .append(ctx.t("att.calendar.title", "period", s.period())).append("\n\n");
        // A calendar only makes sense per month; for quarter/year show the months one after another.
        List<AttendanceDay> days = s.days();
        YearMonth current = null;
        StringBuilder row = new StringBuilder();
        String rowFrom = null;
        String rowTo = null;
        for (AttendanceDay d : days) {
            YearMonth ym = YearMonth.from(d.date());
            if (!ym.equals(current)) {
                if (row.length() > 0) sb.append(line(rowFrom, rowTo, row)).append("\n");
                row.setLength(0);
                current = ym;
                if (days.size() > 31) sb.append("\n<b>").append(ctx.i18n().monthYear(ctx.lang(), ym.getMonthValue(), ym.getYear())).append("</b>\n");
                int lead = d.date().getDayOfWeek().getValue() - 1;
                row.append("▫️".repeat(lead));
                rowFrom = null;
            }
            if (rowFrom == null) rowFrom = String.format("%02d", d.date().getDayOfMonth());
            rowTo = String.format("%02d", d.date().getDayOfMonth());
            row.append(DAY_EMOJI.getOrDefault(d.status(), "⬜"));
            if (d.date().getDayOfWeek() == DayOfWeek.SUNDAY) {
                sb.append(line(rowFrom, rowTo, row)).append("\n");
                row.setLength(0);
                rowFrom = null;
            }
        }
        if (row.length() > 0) sb.append(line(rowFrom, rowTo, row)).append("\n");
        sb.append("\n").append(ctx.t("att.calendar.legend"));

        InlineKeyboardMarkup.Builder kb = InlineKeyboardMarkup.builder();
        if (days.size() <= 31) kb.row(ctx.btn(ctx.t("common.image"), base.with("v", "img")));
        kb.row(ctx.navRow(base).toArray(new InlineButton[0]));
        return BotView.of(sb.toString(), kb.build()).section("attendance");
    }

    private static String line(String from, String to, StringBuilder row) {
        String label = from == null ? "     " : from + "–" + to;
        return "<code>" + label + "</code> " + row;
    }

    private BotView details(BotContext ctx, AttendanceSummary s, CallbackData base, int page) {
        List<AttendanceIncident> all = data.incidents(ctx.student(), s.from(), s.to());
        int pages = Math.max(1, (all.size() + PAGE - 1) / PAGE);
        page = Math.max(0, Math.min(page, pages - 1));
        StringBuilder sb = new StringBuilder(ctx.crumb(ctx.t("sec.attendance"), s.period(), ctx.t("att.btn.details"))).append("\n")
                .append(ctx.childHeader()).append("\n")
                .append(ctx.t("att.details.title", "period", s.period())).append("\n\n");
        if (all.isEmpty()) {
            sb.append(ctx.t("att.details.empty"));
        } else {
            for (AttendanceIncident i : all.subList(page * PAGE, Math.min(all.size(), (page + 1) * PAGE))) {
                sb.append(ctx.t("att.details.item", "icon", "ABSENT".equals(i.status()) ? "🔴" : "🟡",
                        "date", ctx.dateFull(i.date()), "n", i.lessonNumber(), "subject", e(i.subject()))).append("\n");
            }
        }
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .row(ctx.pagerRow(base.with("v", "det"), page, pages).toArray(new InlineButton[0]))
                .row(ctx.navRow(base).toArray(new InlineButton[0]))
                .build();
        return BotView.of(sb.toString().stripTrailing(), kb).section("attendance");
    }

    private BotView bySubject(BotContext ctx, AttendanceSummary s, CallbackData base) {
        List<SubjectAbsence> list = data.absencesBySubject(ctx.student(), s.from(), s.to());
        StringBuilder sb = new StringBuilder(ctx.crumb(ctx.t("sec.attendance"), s.period(), ctx.t("att.btn.subjects"))).append("\n")
                .append(ctx.childHeader()).append("\n")
                .append(ctx.t("att.subjects.title", "period", s.period())).append("\n\n");
        if (list.isEmpty()) {
            sb.append(ctx.t("att.subjects.empty"));
        } else {
            for (SubjectAbsence a : list) {
                sb.append(ctx.t("att.subjects.item", "emoji", SubjectIcons.subject(a.subject()), "subject", e(a.subject()),
                        "absent", a.absent(), "late", a.late())).append("\n");
            }
        }
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder().row(ctx.navRow(base).toArray(new InlineButton[0])).build();
        return BotView.of(sb.toString().stripTrailing(), kb).section("attendance");
    }

    private BotView image(BotContext ctx, YearMonth month, CallbackData base) {
        String label = ctx.i18n().monthYear(ctx.lang(), month.getMonthValue(), month.getYear());
        AttendanceSummary s = data.attendance(ctx.student(), "month", month.toString(), label);
        ChildInfo child = data.child(ctx.student());
        BotImageService.Rendered png = images.attendanceCalendar(ctx.lang(), child, s, month.getYear(), month.getMonthValue(), data.today());
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder().row(ctx.navRow(base).toArray(new InlineButton[0])).build();
        return BotView.photo(png, ctx.t("att.image_caption", "name", e(child.fullName()), "period", label), kb).section("attendance_img");
    }

    String periodLabel(BotContext ctx, String kind, YearMonth month, int quarter) {
        return switch (kind) {
            case "quarter" -> ctx.t("att.period.quarter", "n", quarter);
            case "year" -> {
                int start = ParentStats.schoolYearStart(data.today());
                yield ctx.t("att.period.year", "from", start, "to", start + 1);
            }
            default -> ctx.i18n().monthYear(ctx.lang(), month.getMonthValue(), month.getYear());
        };
    }

    private static YearMonth parseMonth(String value, YearMonth fallback) {
        try {
            return value == null ? fallback : YearMonth.parse(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    static String fmt(Double v) {
        if (v == null) return "—";
        return v == Math.floor(v) ? String.valueOf(v.longValue()) : String.valueOf(v);
    }
}
