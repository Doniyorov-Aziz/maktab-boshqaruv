package uz.azizbek.maktabboshqaruv.bot.screens;

import uz.azizbek.maktabboshqaruv.bot.*;
import uz.azizbek.maktabboshqaruv.service.parent.ParentDataService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentViews.BellView;
import uz.azizbek.maktabboshqaruv.service.parent.ParentViews.SchoolInfo;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineButton;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineKeyboardMarkup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static uz.azizbek.maktabboshqaruv.bot.BotContext.e;

/** 🏫 Maktab haqida — name, address, phone, bell schedule, director's reception days. */
@Component
public class SchoolInfoScreen implements Screen {

    @Autowired
    private ParentDataService data;

    @Override
    public String code() {
        return "info";
    }

    @Override
    public BotView render(BotContext ctx, CallbackData cb) {
        SchoolInfo s = data.school(ctx.student());
        StringBuilder sb = new StringBuilder(ctx.crumb(ctx.t("sec.school"))).append("\n\n")
                .append(ctx.t("sch.title", "name", e(s.name()))).append("\n");
        if (s.address() != null) sb.append(ctx.t("sch.address", "address", e(s.address()))).append("\n");
        if (s.phone() != null) sb.append(ctx.t("sch.phone", "phone", e(s.phone()))).append("\n");
        if (s.director() != null) sb.append(ctx.t("sch.director", "name", e(s.director()))).append("\n");
        if (s.receptionHours() != null) sb.append(ctx.t("sch.reception", "text", e(s.receptionHours()))).append("\n");
        sb.append("\n").append(ctx.t("sch.bells")).append("\n");
        if (s.bellNote() != null) {
            sb.append(e(s.bellNote()));
        } else if (s.bells().isEmpty()) {
            sb.append(ctx.t("sch.bells_empty"));
        } else {
            for (BellView b : s.bells()) {
                sb.append(ctx.t("sch.bell_item", "n", b.number(), "start", b.start(), "end", b.end())).append("\n");
            }
        }
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .row(ctx.btn(ctx.t("kb.write"), "msg"))
                .row(ctx.navRow(null).toArray(new InlineButton[0]))
                .build();
        return BotView.of(sb.toString().stripTrailing(), kb).section("school");
    }
}
