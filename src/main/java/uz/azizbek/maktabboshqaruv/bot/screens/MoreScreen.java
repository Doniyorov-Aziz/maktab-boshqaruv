package uz.azizbek.maktabboshqaruv.bot.screens;

import org.springframework.stereotype.Component;
import uz.azizbek.maktabboshqaruv.bot.*;
import uz.azizbek.maktabboshqaruv.service.parent.ParentModulesService;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineButton;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineKeyboardMarkup;

import java.util.ArrayList;
import java.util.List;

/** ➕ Yana — the school modules that do not fit on the main menu: exams, meetings, the tabel, achievements, payments. */
@Component
public class MoreScreen implements Screen {

    private final ParentModulesService data;

    public MoreScreen(ParentModulesService data) {
        this.data = data;
    }

    @Override
    public String code() {
        return "more";
    }

    @Override
    public BotView render(BotContext ctx, CallbackData cb) {
        String text = ctx.crumb(ctx.t("sec.more")) + "\n" + ctx.childHeader() + "\n" + ctx.t("more.pick");
        List<InlineButton> buttons = new ArrayList<>(List.of(
                ctx.btn(ctx.t("kb.exams"), "exm").styled("primary"),
                ctx.btn(ctx.t("kb.meetings"), "mt").styled("primary"),
                ctx.btn(ctx.t("kb.tabel"), "tab").styled("primary"),
                ctx.btn(ctx.t("kb.achievements"), "ach").styled("primary")));
        if (data.paymentsEnabled(ctx.student())) buttons.add(ctx.btn(ctx.t("kb.payments"), "pay").styled("primary"));
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .grid(buttons, 2)
                .row(ctx.switchRow("more").toArray(new InlineButton[0]))
                .row(ctx.navRow(null).toArray(new InlineButton[0]))
                .build();
        return BotView.of(text, kb).section("more");
    }
}
