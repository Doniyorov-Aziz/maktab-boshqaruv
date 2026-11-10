package uz.azizbek.maktabboshqaruv.bot.screens;

import org.springframework.stereotype.Component;
import uz.azizbek.maktabboshqaruv.bot.*;
import uz.azizbek.maktabboshqaruv.service.parent.ParentModulesService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentModulesService.FileResult;
import uz.azizbek.maktabboshqaruv.service.parent.ParentModulesService.HomeworkItem;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineButton;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineKeyboardMarkup;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static uz.azizbek.maktabboshqaruv.bot.BotContext.e;

/**
 * 📝 Uy vazifasi — the class's homework still to do, by due date (v=past: what was given in the last two weeks);
 * a 📎 button sends a homework's file (f=&lt;id&gt;).
 */
@Component
public class HomeworkScreen implements Screen {

    static final int MAX_ITEMS = 15;
    static final int PAST_DAYS = 14;

    private final ParentModulesService data;

    public HomeworkScreen(ParentModulesService data) {
        this.data = data;
    }

    @Override
    public String code() {
        return "hw";
    }

    @Override
    public BotView render(BotContext ctx, CallbackData cb) {
        boolean past = "past".equals(cb.get("v"));
        String toast = null;
        if (cb.get("f") != null) {
            FileResult r = data.sendHomeworkFile(ctx.student(), ctx.chatId(), cb.getLong("f"));
            toast = switch (r) {
                case QUEUED -> ctx.t("hw.toast.sending");
                case ALREADY -> ctx.t("hw.toast.already");
                case NONE -> ctx.t("hw.toast.none");
            };
        }
        List<HomeworkItem> items = past ? data.homeworkPast(ctx.student(), PAST_DAYS) : data.homeworkUpcoming(ctx.student());
        LocalDate today = data.today();

        StringBuilder sb = new StringBuilder(ctx.crumb(ctx.t("sec.homework"), past ? ctx.t("hw.past") : ctx.t("hw.current")))
                .append("\n").append(ctx.childHeader()).append("\n");
        List<InlineButton> fileButtons = new ArrayList<>();
        if (items.isEmpty()) {
            sb.append(past ? ctx.t("hw.empty_past") : ctx.t("hw.empty"));
        } else {
            LocalDate lastDay = null;
            int shown = 0;
            for (HomeworkItem h : items) {
                if (shown++ >= MAX_ITEMS) {
                    sb.append("\n").append(ctx.t("hw.more", "count", items.size() - MAX_ITEMS));
                    break;
                }
                LocalDate day = past ? h.assigned() : h.due();
                if (!day.equals(lastDay)) {
                    sb.append("\n").append(past ? ctx.t("hw.given_on", "date", ctx.dateFull(day)) : dueLabel(ctx, day, today)).append("\n");
                    lastDay = day;
                }
                sb.append(SubjectIcons.subject(h.subject())).append(" <b>").append(e(h.subject())).append("</b>: ")
                        .append(e(h.title()));
                if (h.fileName() != null) sb.append(" 📎");
                sb.append("\n");
                if (h.description() != null) {
                    String d = h.description().length() > 300 ? h.description().substring(0, 300) + "…" : h.description();
                    sb.append("<blockquote>").append(e(d)).append("</blockquote>\n");
                }
                if (past) sb.append("   ").append(ctx.t("hw.due_was", "date", ctx.dateShort(h.due()))).append("\n");
                if (h.fileName() != null && fileButtons.size() < 8) {
                    String label = "📎 " + h.subject() + " — " + h.title();
                    if (label.length() > 40) label = label.substring(0, 39) + "…";
                    fileButtons.add(ctx.btn(label, CallbackData.of("hw").with("f", h.id()).with("v", past ? "past" : "now")));
                }
            }
        }

        InlineKeyboardMarkup.Builder kb = InlineKeyboardMarkup.builder();
        for (InlineButton b : fileButtons) kb.row(b);
        kb.row(past ? ctx.btn(ctx.t("hw.btn.current"), "hw") : ctx.btn(ctx.t("hw.btn.past"), CallbackData.of("hw").with("v", "past")));
        kb.row(ctx.switchRow("hw").toArray(new InlineButton[0]));
        kb.row(ctx.navRow(null).toArray(new InlineButton[0]));
        BotView view = BotView.of(sb.toString().stripTrailing(), kb.build()).section("homework");
        return toast == null ? view : view.toast(toast);
    }

    private static String dueLabel(BotContext ctx, LocalDate day, LocalDate today) {
        if (day.equals(today)) return ctx.t("hw.due_today", "date", ctx.dateShort(day));
        if (day.equals(today.plusDays(1))) return ctx.t("hw.due_tomorrow", "date", ctx.dateShort(day));
        return ctx.t("hw.due_on", "date", ctx.dateFull(day));
    }
}
