package uz.azizbek.maktabboshqaruv.bot.screens;

import uz.azizbek.maktabboshqaruv.bot.*;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.service.LinkingService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentAccessService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentDataService;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineButton;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineKeyboardMarkup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static uz.azizbek.maktabboshqaruv.bot.BotContext.e;

/**
 * 👨‍👩‍👧 Farzandlarim — linked children, choosing the active one (a=sel, remembered;
 * r = screen to return to), adding another child by code (a=add) and
 * unlinking one after confirmation (a=unl → a=unly).
 */
@Component
public class ChildrenScreen implements Screen, InputHandler {

    static final String PENDING = "ADD_CHILD";

    @Autowired
    private ParentAccessService access;

    @Autowired
    private LinkingService linking;

    @Autowired
    private Onboarding onboarding;

    @Override
    public String code() {
        return "ch";
    }

    @Override
    public boolean requiresStudent() {
        return false;
    }

    @Override
    public List<String> pendingPrefixes() {
        return List.of(PENDING);
    }

    @Override
    public BotView render(BotContext ctx, CallbackData cb) {
        String action = cb.get("a");
        String returnTo = cb.get("r");
        if ("sel".equals(action)) {
            Student s = access.requireLinked(ctx.chatId(), cb.getLong("id"));
            ctx.session().setSelectedStudentId(s.getId());
            access.save(ctx.session());
            String toast = ctx.t("ch.selected_toast", "name", s.getFirstName());
            return BotView.redirect(returnTo != null ? returnTo : "home", toast);
        }
        if ("unl".equals(action)) {
            Student s = access.requireLinked(ctx.chatId(), cb.getLong("id"));
            String text = ctx.crumb(ctx.t("sec.children")) + "\n\n"
                    + ctx.t("ch.unlink.confirm", "name", e(ParentDataService.fullName(s)));
            return BotView.of(text, InlineKeyboardMarkup.builder()
                    .row(ctx.btn(ctx.t("common.yes"), CallbackData.of("ch").with("a", "unly").with("id", s.getId())),
                            ctx.btn(ctx.t("common.no"), CallbackData.of("ch")))
                    .build()).section("children");
        }
        if ("unly".equals(action)) {
            Student s = access.requireLinked(ctx.chatId(), cb.getLong("id"));
            linking.unlink(ctx.chatId(), s.getId());
            List<Student> left = access.linkedStudents(ctx.chatId());
            BotContext fresh = new BotContext(ctx.chatId(), ctx.messageId(), ctx.session(),
                    left.isEmpty() ? null : left.get(0), left, ctx.from());
            return list(fresh, returnTo).toast(ctx.t("ch.unlinked", "name", s.getFirstName()));
        }
        if ("add".equals(action)) {
            ctx.session().setPendingAction(PENDING);
            access.save(ctx.session());
            return BotView.of(ctx.crumb(ctx.t("sec.children"), ctx.t("ch.btn.add")) + "\n\n" + ctx.t("ch.add.prompt"),
                    InlineKeyboardMarkup.builder()
                            .row(ctx.btn(ctx.t("common.cancel"), CallbackData.of("ch").with("a", "x")))
                            .build())
                    .preface(ctx.t("ch.btn.add"), uz.azizbek.maktabboshqaruv.telegram.TelegramModels.shareContactKeyboard(ctx.t("start.share_button")))
                    .section("children");
        }
        if ("x".equals(action)) {
            ctx.session().setPendingAction(null);
            access.save(ctx.session());
            return list(ctx, returnTo).preface(ctx.t("msg.cancelled"), Keyboards.main(ctx.lang()));
        }
        return list(ctx, returnTo);
    }

    BotView list(BotContext ctx, String returnTo) {
        StringBuilder sb = new StringBuilder(ctx.crumb(ctx.t("sec.children"))).append("\n\n").append(ctx.t("ch.title")).append("\n\n");
        InlineKeyboardMarkup.Builder kb = InlineKeyboardMarkup.builder();
        if (ctx.students().isEmpty()) {
            sb.append(ctx.t("ch.empty"));
        } else {
            Long selected = ctx.student() == null ? null : ctx.student().getId();
            for (Student s : ctx.students()) {
                boolean current = s.getId().equals(selected);
                String cls = ParentDataService.className(s.getSchoolClass());
                sb.append(ctx.t("ch.item", "mark", current ? ctx.t("ch.selected_mark") : "", "name", e(ParentDataService.fullName(s)),
                        "class", e(cls))).append("\n");
                kb.row(ctx.btn((current ? "✅ " : "👤 ") + s.getFirstName() + " · " + cls,
                                CallbackData.of("ch").with("a", "sel").with("id", s.getId()).with("r", returnTo)),
                        ctx.btn(ctx.t("ch.btn.unlink"), CallbackData.of("ch").with("a", "unl").with("id", s.getId())));
            }
            sb.append("\n").append(ctx.t("ch.hint"));
        }
        kb.row(ctx.btn(ctx.t("ch.btn.add"), CallbackData.of("ch").with("a", "add")));
        kb.row(ctx.navRow(returnTo == null ? null : CallbackData.of(returnTo)).toArray(new InlineButton[0]));
        return BotView.of(sb.toString().stripTrailing(), kb.build()).section("children");
    }

    /** While "add child" is open, a typed code links another child. */
    @Override
    public BotView handleInput(BotContext ctx, String text, String photoFileId) {
        ctx.session().setPendingAction(null);
        access.save(ctx.session());
        LinkingService.Result result = linking.byCode(ctx.chatId(), ctx.from(), text == null ? "" : text.trim());
        if (!result.ok()) {
            return list(ctx, null).preface(ctx.t(result.errorKey()), Keyboards.main(ctx.lang()));
        }
        return onboarding.linked(ctx, result.linked());
    }
}
