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

import java.util.ArrayList;
import java.util.List;

import static uz.azizbek.maktabboshqaruv.bot.BotContext.e;

/**
 * 📘 Baholar. v=recent (latest grades, paged), subj (averages per subject with
 * bars and trend), det (one subject: teacher + all grades, paged, id=subjectId),
 * qtr (quarter grades), chart (PNG).
 */
@Component
public class GradesScreen implements Screen {

    private static final int PAGE = 8;

    @Autowired
    private ParentDataService data;

    @Autowired
    private BotImageService images;

    @Override
    public String code() {
        return "gr";
    }

    @Override
    public BotView render(BotContext ctx, CallbackData cb) {
        String view = cb.get("v", "recent");
        return switch (view) {
            case "subj" -> subjects(ctx);
            case "det" -> detail(ctx, cb.getLong("id"), cb.getInt("p", 0));
            case "qtr" -> quarter(ctx);
            case "chart" -> chart(ctx);
            default -> recent(ctx, cb.getInt("p", 0));
        };
    }

    private InlineButton[] tabs(BotContext ctx, String current) {
        return new InlineButton[]{tab(ctx, "recent", "gr.tab.recent", current), tab(ctx, "subj", "gr.tab.subjects", current),
                tab(ctx, "qtr", "gr.tab.quarter", current)};
    }

    private InlineButton tab(BotContext ctx, String v, String key, String current) {
        String label = ctx.t(key);
        return ctx.btn(v.equals(current) ? "• " + label + " •" : label, CallbackData.of("gr").with("v", v));
    }

    private BotView recent(BotContext ctx, int page) {
        List<GradeView> all = data.recentGrades(ctx.student(), 48);
        int pages = Math.max(1, (all.size() + PAGE - 1) / PAGE);
        page = Math.max(0, Math.min(page, pages - 1));
        StringBuilder sb = header(ctx, ctx.t("gr.tab.recent")).append(ctx.t("gr.recent.title")).append("\n\n");
        if (all.isEmpty()) {
            sb.append(ctx.t("gr.empty"));
        } else {
            for (GradeView g : all.subList(page * PAGE, Math.min(all.size(), (page + 1) * PAGE))) {
                sb.append(ctx.t("gr.recent.item", "score_emoji", SubjectIcons.grade(g.score()), "score", g.score(),
                        "subject", e(g.subject()), "date", ctx.dateShort(g.date()), "type", ctx.t("gr.type." + g.type()),
                        "comment", comment(ctx, g.comment()))).append("\n");
            }
        }
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .row(tabs(ctx, "recent"))
                .row(ctx.pagerRow(CallbackData.of("gr").with("v", "recent"), page, pages).toArray(new InlineButton[0]))
                .row(ctx.switchRow("gr").toArray(new InlineButton[0]))
                .row(ctx.navRow(null).toArray(new InlineButton[0]))
                .build();
        return BotView.of(sb.toString().stripTrailing(), kb).section("grades");
    }

    private BotView subjects(BotContext ctx) {
        List<SubjectAverage> list = data.subjectAverages(ctx.student());
        StringBuilder sb = header(ctx, ctx.t("gr.tab.subjects")).append(ctx.t("gr.subjects.title")).append("\n\n");
        List<InlineButton> buttons = new ArrayList<>();
        if (list.isEmpty()) {
            sb.append(ctx.t("gr.empty"));
        } else {
            for (SubjectAverage a : list) {
                sb.append(ctx.t("gr.subjects.item", "book", SubjectIcons.book(a.average()), "subject", e(a.subject()),
                        "avg", String.format(java.util.Locale.ROOT, "%.1f", a.average()),
                        "bar", ParentStats.gradeBar(a.average(), 5), "trend", trend(a.trend()))).append("\n");
                buttons.add(ctx.btn(SubjectIcons.subject(a.subject()) + " " + shortName(a.subject()),
                        CallbackData.of("gr").with("v", "det").with("id", a.subjectId())));
            }
            sb.append("\n").append(ctx.t("gr.trend.note"));
        }
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .row(tabs(ctx, "subj"))
                .grid(buttons, 2)
                .row(list.isEmpty() ? null : ctx.btn(ctx.t("gr.btn.chart"), CallbackData.of("gr").with("v", "chart")))
                .row(ctx.switchRow("gr").toArray(new InlineButton[0]))
                .row(ctx.navRow(null).toArray(new InlineButton[0]))
                .build();
        return BotView.of(sb.toString().stripTrailing(), kb).section("grades");
    }

    private BotView detail(BotContext ctx, Long subjectId, int page) {
        SubjectDetail d = data.subjectDetail(ctx.student(), subjectId);
        CallbackData back = CallbackData.of("gr").with("v", "subj");
        if (d.subject() == null) {
            return BotView.of(ctx.t("gr.empty"), InlineKeyboardMarkup.builder().row(ctx.navRow(back).toArray(new InlineButton[0])).build());
        }
        int pages = Math.max(1, (d.grades().size() + PAGE - 1) / PAGE);
        page = Math.max(0, Math.min(page, pages - 1));
        StringBuilder sb = header(ctx, e(d.subject()))
                .append(ctx.t("gr.subject.title", "emoji", SubjectIcons.subject(d.subject()), "subject", e(d.subject()),
                        "teacher", e(d.teacher() == null ? ctx.t("common.none") : d.teacher()),
                        "avg", String.format(java.util.Locale.ROOT, "%.2f", d.average()), "count", d.grades().size()))
                .append("\n\n");
        if (d.grades().isEmpty()) {
            sb.append(ctx.t("gr.empty"));
        } else {
            for (GradeView g : d.grades().subList(page * PAGE, Math.min(d.grades().size(), (page + 1) * PAGE))) {
                sb.append(ctx.t("gr.subject.item", "score_emoji", SubjectIcons.grade(g.score()), "score", g.score(),
                        "date", ctx.dateFull(g.date()), "type", ctx.t("gr.type." + g.type()),
                        "comment", comment(ctx, g.comment()))).append("\n");
            }
        }
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .row(ctx.pagerRow(CallbackData.of("gr").with("v", "det").with("id", subjectId), page, pages).toArray(new InlineButton[0]))
                .row(ctx.btn(ctx.t("kb.write"), CallbackData.of("msg").with("a", "to").with("to", "CT")))
                .row(ctx.navRow(back).toArray(new InlineButton[0]))
                .build();
        return BotView.of(sb.toString().stripTrailing(), kb).section("grades");
    }

    private BotView quarter(BotContext ctx) {
        List<GradeView> list = data.quarterGrades(ctx.student());
        StringBuilder sb = header(ctx, ctx.t("gr.tab.quarter")).append(ctx.t("gr.quarter.title")).append("\n\n");
        if (list.isEmpty()) {
            sb.append(ctx.t("gr.quarter.empty"));
        } else {
            for (GradeView g : list) {
                sb.append(ctx.t("gr.quarter.item", "emoji", SubjectIcons.grade(g.score()), "subject", e(g.subject()),
                        "score", g.score(), "date", ctx.dateShort(g.date()))).append("\n");
            }
        }
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .row(tabs(ctx, "qtr"))
                .row(ctx.switchRow("gr").toArray(new InlineButton[0]))
                .row(ctx.navRow(null).toArray(new InlineButton[0]))
                .build();
        return BotView.of(sb.toString().stripTrailing(), kb).section("grades");
    }

    private BotView chart(BotContext ctx) {
        ChildInfo child = data.child(ctx.student());
        BotImageService.Rendered png = images.subjectChart(ctx.lang(), child, data.subjectAverages(ctx.student()));
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .row(ctx.navRow(CallbackData.of("gr").with("v", "subj")).toArray(new InlineButton[0])).build();
        return BotView.photo(png, ctx.t("gr.chart_caption", "name", e(child.fullName())), kb).section("grades_img");
    }

    private StringBuilder header(BotContext ctx, String page) {
        return new StringBuilder(ctx.crumb(ctx.t("sec.grades"), page)).append("\n").append(ctx.childHeader()).append("\n");
    }

    private String comment(BotContext ctx, String text) {
        return text == null || text.isBlank() ? "" : ctx.t("gr.comment", "text", e(text));
    }

    private static String trend(String t) {
        return switch (t) {
            case "UP" -> "↑";
            case "DOWN" -> "↓";
            case "FLAT" -> "→";
            default -> "";
        };
    }

    private static String shortName(String s) {
        return s.length() <= 18 ? s : s.substring(0, 17) + "…";
    }
}
