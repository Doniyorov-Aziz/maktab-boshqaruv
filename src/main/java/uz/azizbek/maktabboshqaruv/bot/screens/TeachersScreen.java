package uz.azizbek.maktabboshqaruv.bot.screens;

import uz.azizbek.maktabboshqaruv.bot.*;
import uz.azizbek.maktabboshqaruv.service.parent.ParentDataService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentViews.TeacherView;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineButton;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineKeyboardMarkup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static uz.azizbek.maktabboshqaruv.bot.BotContext.e;

/** 👩‍🏫 O'qituvchilar — class teacher and every subject teacher; phones only if the school allows it. */
@Component
public class TeachersScreen implements Screen {

    @Autowired
    private ParentDataService data;

    @Override
    public String code() {
        return "tch";
    }

    @Override
    public BotView render(BotContext ctx, CallbackData cb) {
        List<TeacherView> list = data.teachers(ctx.student());
        String className = ParentDataService.className(ctx.student().getSchoolClass());
        StringBuilder sb = new StringBuilder(ctx.crumb(ctx.t("sec.teachers"))).append("\n")
                .append(ctx.childHeader()).append("\n")
                .append(ctx.t("tch.title", "class", e(className))).append("\n\n");
        boolean anyPhone = false;
        if (list.isEmpty()) {
            sb.append(ctx.t("tch.empty"));
        } else {
            for (TeacherView t : list) {
                String phone = t.phone() == null ? "" : ctx.t("tch.phone", "phone", e(t.phone()));
                anyPhone |= t.phone() != null;
                if (t.classTeacher()) {
                    sb.append(ctx.t("tch.class_teacher", "name", e(t.name()), "phone", phone)).append("\n\n");
                } else {
                    sb.append(ctx.t("tch.item", "emoji", SubjectIcons.subject(t.subject()), "subject", e(t.subject()),
                            "name", e(t.name()), "phone", phone)).append("\n");
                }
            }
            if (!anyPhone) sb.append("\n").append(ctx.t("tch.phone_hidden"));
        }
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .row(ctx.btn(ctx.t("kb.write"), "msg"))
                .row(ctx.switchRow("tch").toArray(new InlineButton[0]))
                .row(ctx.navRow(null).toArray(new InlineButton[0]))
                .build();
        return BotView.of(sb.toString().stripTrailing(), kb).section("teachers");
    }
}
