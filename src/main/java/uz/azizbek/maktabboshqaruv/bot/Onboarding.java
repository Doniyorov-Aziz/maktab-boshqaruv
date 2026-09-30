package uz.azizbek.maktabboshqaruv.bot;

import uz.azizbek.maktabboshqaruv.bot.screens.HomeScreen;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.service.parent.ParentAccessService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentDataService;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static uz.azizbek.maktabboshqaruv.bot.BotContext.e;

/**
 * First contact: the warm welcome for a parent who is not linked yet, and —
 * right after a successful link — "✅ Tabriklaymiz!", a three-step intro and
 * the main menu, with the bottom keyboard installed.
 */
@Component
public class Onboarding {

    @Autowired
    private HomeScreen homeScreen;

    @Autowired
    private ParentAccessService access;

    public BotView welcome(BotContext ctx) {
        return BotView.of(ctx.t("start.welcome", "app", ctx.t("app.name")), null)
                .withReplyKeyboard(TelegramModels.shareContactKeyboard(ctx.t("start.share_button")))
                .section("welcome");
    }

    public BotView linked(BotContext ctx, List<Student> linked) {
        ctx.session().setSelectedStudentId(linked.get(0).getId());
        ctx.session().setPendingAction(null);
        access.save(ctx.session());
        List<Student> all = access.linkedStudents(ctx.chatId());
        BotContext fresh = new BotContext(ctx.chatId(), null, ctx.session(), linked.get(0), all, ctx.from());

        String success;
        if (linked.size() == 1) {
            Student s = linked.get(0);
            success = fresh.t("link.success", "name", e(ParentDataService.fullName(s)),
                    "class", e(ParentDataService.className(s.getSchoolClass())));
        } else {
            String list = linked.stream()
                    .map(s -> fresh.t("link.list_item", "name", e(ParentDataService.fullName(s)),
                            "class", e(ParentDataService.className(s.getSchoolClass()))))
                    .collect(Collectors.joining("\n"));
            success = fresh.t("link.success_many", "count", linked.size(), "list", list);
        }
        success += "\n\n" + fresh.t("link.intro");
        return homeScreen.render(fresh, CallbackData.of("home"))
                .preface(success, Keyboards.main(fresh.lang()))
                .section("onboarding");
    }
}
