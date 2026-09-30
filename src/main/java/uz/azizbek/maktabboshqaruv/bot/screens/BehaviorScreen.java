package uz.azizbek.maktabboshqaruv.bot.screens;

import uz.azizbek.maktabboshqaruv.bot.*;
import uz.azizbek.maktabboshqaruv.service.parent.ParentDataService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentViews.BehaviorView;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineButton;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineKeyboardMarkup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.List;

import static uz.azizbek.maktabboshqaruv.bot.BotContext.e;

/** ⭐ Xulq — rewards first and in a cheerful tone, then warnings; month by month (m=YYYY-MM). */
@Component
public class BehaviorScreen implements Screen {

    @Autowired
    private ParentDataService data;

    @Override
    public String code() {
        return "beh";
    }

    @Override
    public BotView render(BotContext ctx, CallbackData cb) {
        YearMonth thisMonth = YearMonth.from(data.today());
        YearMonth month;
        try {
            month = cb.get("m") == null ? thisMonth : YearMonth.parse(cb.get("m"));
        } catch (Exception ex) {
            month = thisMonth;
        }
        List<BehaviorView> records = data.behavior(ctx.student(), month.atDay(1), month.atEndOfMonth());
        long rewards = records.stream().filter(b -> "REWARD".equals(b.type())).count();
        String monthLabel = ctx.i18n().monthYear(ctx.lang(), month.getMonthValue(), month.getYear());

        StringBuilder sb = new StringBuilder(ctx.crumb(ctx.t("sec.behavior"), monthLabel)).append("\n")
                .append(ctx.childHeader()).append("\n")
                .append(ctx.t("beh.summary", "month", monthLabel, "rewards", rewards, "warnings", records.size() - rewards))
                .append("\n\n");
        if (records.isEmpty()) {
            sb.append(ctx.t("beh.empty"));
        } else {
            if (rewards > 0) {
                sb.append(ctx.t("beh.rewards_title")).append("\n");
                records.stream().filter(b -> "REWARD".equals(b.type())).forEach(b ->
                        sb.append(ctx.t("beh.reward_item", "date", ctx.dateShort(b.date()), "text", e(b.description()))).append("\n"));
                sb.append("\n");
            }
            if (rewards < records.size()) {
                sb.append(ctx.t("beh.warnings_title")).append("\n");
                records.stream().filter(b -> !"REWARD".equals(b.type())).forEach(b ->
                        sb.append(ctx.t("beh.warning_item", "date", ctx.dateShort(b.date()), "text", e(b.description()))).append("\n"));
            }
        }
        YearMonth prev = month.minusMonths(1);
        YearMonth next = month.plusMonths(1);
        CallbackData base = CallbackData.of("beh");
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .row(ctx.btn("◀️ " + ctx.t("month." + prev.getMonthValue()), base.with("m", prev.toString())),
                        ctx.btn(monthLabel, CallbackData.of("noop")),
                        next.isAfter(thisMonth) ? ctx.blank()
                                : ctx.btn(ctx.t("month." + next.getMonthValue()) + " ▶️", base.with("m", next.toString())))
                .row(ctx.switchRow("beh").toArray(new InlineButton[0]))
                .row(ctx.navRow(null).toArray(new InlineButton[0]))
                .build();
        return BotView.of(sb.toString().stripTrailing(), kb).section("behavior");
    }
}
