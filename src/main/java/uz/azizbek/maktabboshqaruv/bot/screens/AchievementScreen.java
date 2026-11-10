package uz.azizbek.maktabboshqaruv.bot.screens;

import org.springframework.stereotype.Component;
import uz.azizbek.maktabboshqaruv.bot.*;
import uz.azizbek.maktabboshqaruv.service.parent.ParentModulesService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentModulesService.AchievementItem;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineButton;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineKeyboardMarkup;

import java.util.List;

import static uz.azizbek.maktabboshqaruv.bot.BotContext.e;

/** 🏆 Yutuqlar — the child's olympiads, competitions and certificates, newest first. */
@Component
public class AchievementScreen implements Screen {

    static final int MAX_ITEMS = 20;

    private final ParentModulesService data;

    public AchievementScreen(ParentModulesService data) {
        this.data = data;
    }

    @Override
    public String code() {
        return "ach";
    }

    @Override
    public BotView render(BotContext ctx, CallbackData cb) {
        List<AchievementItem> items = data.achievements(ctx.student());
        StringBuilder sb = new StringBuilder(ctx.crumb(ctx.t("sec.more"), ctx.t("sec.achievements"))).append("\n")
                .append(ctx.childHeader()).append("\n");
        if (items.isEmpty()) {
            sb.append(ctx.t("ach.empty"));
        } else {
            long prizes = items.stream().filter(a -> a.place() != null).count();
            sb.append(ctx.t("ach.summary", "count", items.size(), "prizes", prizes)).append("\n\n");
            int shown = 0;
            for (AchievementItem a : items) {
                if (shown++ >= MAX_ITEMS) break;
                String medal = a.place() == null ? "🎖" : switch (a.place()) {
                    case 1 -> "🥇";
                    case 2 -> "🥈";
                    default -> "🥉";
                };
                sb.append(medal).append(" <b>").append(e(a.title())).append("</b>\n   ")
                        .append(ctx.t("ach.level." + a.level().name()))
                        .append(" · ").append(ctx.t("ach.cat." + a.category().name()));
                if (a.place() != null) sb.append(" · ").append(ctx.t("ach.place", "n", a.place()));
                if (a.subject() != null) sb.append(" · ").append(e(a.subject()));
                sb.append(" · ").append(ctx.dateShort(a.date())).append("\n");
            }
        }
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .row(ctx.switchRow("ach").toArray(new InlineButton[0]))
                .row(ctx.navRow(CallbackData.of("more")).toArray(new InlineButton[0]))
                .build();
        return BotView.of(sb.toString().stripTrailing(), kb).section("achievements");
    }
}
