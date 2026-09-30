package uz.azizbek.maktabboshqaruv.bot.screens;

import uz.azizbek.maktabboshqaruv.bot.*;
import uz.azizbek.maktabboshqaruv.bot.image.BotImageService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentDataService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentViews.*;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineButton;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineKeyboardMarkup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static uz.azizbek.maktabboshqaruv.bot.BotContext.e;

/** 📊 Hisobot — "Hafta" / "Oy": attendance, grades and behaviour on one page, plus a PNG report card. */
@Component
public class ReportScreen implements Screen {

    @Autowired
    private ParentDataService data;

    @Autowired
    private BotImageService images;

    @Override
    public String code() {
        return "rep";
    }

    @Override
    public BotView render(BotContext ctx, CallbackData cb) {
        String tab = cb.get("t", "week");
        boolean month = "month".equals(tab);
        ReportView report = data.report(ctx.student(), month ? "month" : "week", periodLabel(ctx, month));
        CallbackData base = CallbackData.of("rep").with("t", tab);

        if ("img".equals(cb.get("v"))) {
            ChildInfo child = data.child(ctx.student());
            BotImageService.Rendered png = images.reportCard(ctx.lang(), child, report, month);
            return BotView.photo(png, ctx.t("rep.image_caption", "name", e(child.fullName()), "period", report.period()),
                    InlineKeyboardMarkup.builder().row(ctx.navRow(base).toArray(new InlineButton[0])).build()).section("report_img");
        }

        StringBuilder sb = new StringBuilder(ctx.crumb(ctx.t("sec.report"), ctx.t("rep.tab." + tab))).append("\n")
                .append(ctx.childHeader()).append("\n")
                .append(ctx.t("rep.title", "period", report.period())).append("\n\n")
                .append(body(ctx, report));

        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .row(tab(ctx, "week", tab), tab(ctx, "month", tab))
                .row(ctx.btn(ctx.t("common.image"), base.with("v", "img")))
                .row(ctx.switchRow("rep").toArray(new InlineButton[0]))
                .row(ctx.navRow(null).toArray(new InlineButton[0]))
                .build();
        return BotView.of(sb.toString(), kb).section("report");
    }

    /** Shared with the weekly-report notification. */
    public static String body(BotContext ctx, ReportView r) {
        return body(ctx.i18n(), ctx.lang(), r);
    }

    public static String body(BotI18n i18n, String lang, ReportView r) {
        StringBuilder sb = new StringBuilder();
        AttendanceSummary a = r.attendance();
        if (a.total() == 0) {
            sb.append(i18n.t(lang, "rep.attendance_none"));
        } else {
            sb.append(i18n.t(lang, "rep.attendance", "rate", AttendanceScreen.fmt(a.rate()), "present", a.present() + a.late(),
                    "total", a.total(), "absent", a.absent(), "late", a.late()));
        }
        sb.append("\n");
        if (r.gradeCount() == 0) {
            sb.append(i18n.t(lang, "rep.grades_none"));
        } else {
            sb.append(i18n.t(lang, "rep.grades", "count", r.gradeCount(),
                    "avg", String.format(java.util.Locale.ROOT, "%.2f", r.gradeAverage())));
        }
        sb.append("\n").append(i18n.t(lang, "rep.behavior", "rewards", r.rewards(), "warnings", r.warnings())).append("\n\n");
        sb.append(i18n.t(lang, "rep.strong", "list", list(i18n, lang, r.strongSubjects()))).append("\n");
        sb.append(i18n.t(lang, "rep.attention", "list", list(i18n, lang, r.attentionSubjects())));
        return sb.toString();
    }

    private static String list(BotI18n i18n, String lang, List<String> items) {
        if (items.isEmpty()) return i18n.t(lang, "common.none");
        return items.stream().map(s -> SubjectIcons.subject(s) + " " + e(s)).collect(Collectors.joining(", "));
    }

    private String periodLabel(BotContext ctx, boolean month) {
        LocalDate today = data.today();
        if (month) return ctx.i18n().monthYear(ctx.lang(), today.getMonthValue(), today.getYear());
        return ctx.t("rep.week_period", "from", ctx.dateShort(today.with(DayOfWeek.MONDAY)), "to", ctx.dateShort(today));
    }

    private InlineButton tab(BotContext ctx, String name, String current) {
        String label = ctx.t("rep.tab." + name);
        return ctx.btn(name.equals(current) ? "• " + label + " •" : label, CallbackData.of("rep").with("t", name));
    }
}
