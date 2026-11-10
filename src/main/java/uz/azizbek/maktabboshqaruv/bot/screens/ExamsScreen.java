package uz.azizbek.maktabboshqaruv.bot.screens;

import org.springframework.stereotype.Component;
import uz.azizbek.maktabboshqaruv.bot.*;
import uz.azizbek.maktabboshqaruv.service.parent.ParentModulesService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentModulesService.ExamItem;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineButton;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineKeyboardMarkup;

import java.time.LocalDate;
import java.util.List;

import static uz.azizbek.maktabboshqaruv.bot.BotContext.e;

/** 📝 Nazorat ishlari — the class's tests and exams of the next month, and last week's with the child's result. */
@Component
public class ExamsScreen implements Screen {

    private final ParentModulesService data;

    public ExamsScreen(ParentModulesService data) {
        this.data = data;
    }

    @Override
    public String code() {
        return "exm";
    }

    @Override
    public BotView render(BotContext ctx, CallbackData cb) {
        List<ExamItem> exams = data.exams(ctx.student());
        LocalDate today = data.today();
        StringBuilder sb = new StringBuilder(ctx.crumb(ctx.t("sec.more"), ctx.t("sec.exams"))).append("\n")
                .append(ctx.childHeader()).append("\n");
        List<ExamItem> ahead = exams.stream().filter(x -> !x.date().isBefore(today)).toList();
        List<ExamItem> done = exams.stream().filter(x -> x.date().isBefore(today)).toList();
        if (exams.isEmpty()) {
            sb.append(ctx.t("exm.empty"));
        }
        if (!ahead.isEmpty()) {
            sb.append(ctx.t("exm.ahead")).append("\n");
            for (ExamItem x : ahead) sb.append(line(ctx, x, today)).append("\n");
        }
        if (!done.isEmpty()) {
            sb.append("\n").append(ctx.t("exm.done")).append("\n");
            for (ExamItem x : done) {
                sb.append(line(ctx, x, today));
                sb.append(x.score() == null ? " — " + ctx.t("exm.no_result")
                        : " — " + SubjectIcons.grade(x.score()) + " <b>" + x.score() + "</b>");
                sb.append("\n");
            }
        }
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .row(ctx.switchRow("exm").toArray(new InlineButton[0]))
                .row(ctx.navRow(CallbackData.of("more")).toArray(new InlineButton[0]))
                .build();
        return BotView.of(sb.toString().stripTrailing(), kb).section("exams");
    }

    private static String line(BotContext ctx, ExamItem x, LocalDate today) {
        String when = x.date().equals(today) ? ctx.t("exm.today")
                : x.date().equals(today.plusDays(1)) ? ctx.t("exm.tomorrow") : ctx.dateShort(x.date());
        StringBuilder sb = new StringBuilder("• <b>").append(when).append("</b>");
        if (x.time() != null) sb.append(" ").append(x.time());
        sb.append(" — ").append(SubjectIcons.subject(x.subject())).append(" ").append(e(x.subject()))
                .append(" · ").append(ctx.t("exam.kind." + x.kind().name()));
        if (x.title() != null) sb.append(" (").append(e(x.title())).append(")");
        if (x.place() != null) sb.append(" · 📍 ").append(e(x.place()));
        return sb.toString();
    }
}
