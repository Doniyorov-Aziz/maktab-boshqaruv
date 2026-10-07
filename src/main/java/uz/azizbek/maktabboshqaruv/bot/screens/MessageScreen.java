package uz.azizbek.maktabboshqaruv.bot.screens;

import uz.azizbek.maktabboshqaruv.bot.*;
import uz.azizbek.maktabboshqaruv.entity.Appeal;
import uz.azizbek.maktabboshqaruv.entity.AppealEnums.Target;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.service.appeal.AppealService;
import uz.azizbek.maktabboshqaruv.service.appeal.AppealService.Incoming;
import uz.azizbek.maktabboshqaruv.service.appeal.AppealService.Refused;
import uz.azizbek.maktabboshqaruv.service.parent.ParentAccessService;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineButton;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineKeyboardMarkup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static uz.azizbek.maktabboshqaruv.bot.BotContext.e;

/**
 * "✉️ Ma'muriyatga xat" — a parent writes to the administration or the class teacher.
 *
 *  1. who to write to (🏫 Ma'muriyat / 👩‍🏫 Sinf rahbari); with several children —
 *     which child it is about;
 *  2. the parent sends anything: text, photos, videos, voice, audio, files, round
 *     videos, albums — each gets a short ✓ (an album gets one);
 *  3. "✅ Yuborish" → the school gets one appeal with a number ("#1234"), replies come
 *     here; "↩️ Javob yozish" under a reply continues the same appeal.
 *
 * Callback data keeps the old "msg" screen code, so "💬 Sinf rahbariga yozish" buttons
 * under earlier notifications still work.
 */
@Component
public class MessageScreen implements Screen, InputHandler {

    public static final String PENDING = "APPEAL";

    @Autowired
    private AppealService appeals;

    @Autowired
    private ParentAccessService access;

    @Override
    public String code() {
        return "msg";
    }

    @Override
    public List<String> pendingPrefixes() {
        return List.of(PENDING, "MSG");
    }

    @Override
    public BotView render(BotContext ctx, CallbackData cb) {
        String action = cb.get("a");
        try {
            if ("to".equals(action)) return chooseOrStart(ctx, cb);
            if ("re".equals(action)) return startReply(ctx, cb.getLong("ap"));
            if ("ok".equals(action)) return send(ctx);
            if ("x".equals(action)) {
                Long appealId = composingAppeal(ctx);
                if (appealId != null) appeals.cancel(ctx.chatId(), appealId);
                clear(ctx);
                return main(ctx).toast(ctx.t("appeal.cancelled"));
            }
        } catch (Refused r) {
            clear(ctx);
            return BotView.of(ctx.t(r.key, r.args) + "\n\n" + main(ctx).text(), main(ctx).keyboard()).section("write");
        }
        return main(ctx);
    }

    BotView main(BotContext ctx) {
        StringBuilder sb = new StringBuilder(ctx.crumb(ctx.t("sec.write"))).append("\n").append(ctx.childHeader())
                .append(ctx.t("appeal.title"));
        List<Appeal> history = appeals.recent(ctx.chatId(), ctx.student().getId(), 3);
        if (!history.isEmpty()) {
            sb.append("\n\n").append(ctx.t("appeal.history_title"));
            for (Appeal a : history) {
                sb.append("\n").append(ctx.t("appeal.history_item", "id", a.getId(),
                        "status", ctx.t("appeal.status." + a.getStatus().name()),
                        "date", ctx.dateShort(a.getLastMessageAt().toLocalDate()),
                        "to", ctx.t("appeal.to." + a.getTarget().name()),
                        "text", e(a.getLastPreview() == null ? "" : shorten(a.getLastPreview(), 70))));
            }
        }
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .row(ctx.btn(ctx.t("appeal.to.ADMINISTRATION"), CallbackData.of("msg").with("a", "to").with("to", "AD")),
                        ctx.btn(ctx.t("appeal.to.CLASS_TEACHER"), CallbackData.of("msg").with("a", "to").with("to", "CT")))
                .row(ctx.switchRow("msg").toArray(new InlineButton[0]))
                .row(ctx.navRow(null).toArray(new InlineButton[0]))
                .build();
        return BotView.of(sb.toString(), kb).section("write");
    }

    /** With several children (and no child chosen in the button) — first ask which child. */
    private BotView chooseOrStart(BotContext ctx, CallbackData cb) {
        Target target = "AD".equals(cb.get("to")) ? Target.ADMINISTRATION : Target.CLASS_TEACHER;
        if (ctx.students().size() > 1 && cb.get("s") == null) {
            InlineKeyboardMarkup.Builder kb = InlineKeyboardMarkup.builder();
            for (Student s : ctx.students()) {
                kb.row(ctx.btn("👦 " + s.getFirstName() + " (" + s.getSchoolClass().getGradeNumber() + "-"
                                + s.getSchoolClass().getSectionLetter() + ")",
                        CallbackData.of("msg").with("a", "to").with("to", cb.get("to")).with("s", s.getId())));
            }
            kb.row(ctx.navRow(CallbackData.of("msg")).toArray(new InlineButton[0]));
            return BotView.of(ctx.crumb(ctx.t("sec.write"), ctx.t("appeal.to." + target.name())) + "\n"
                    + ctx.t("appeal.pick_child"), kb.build()).section("write");
        }
        Appeal draft = appeals.startDraft(ctx.chatId(), ctx.student(), target, ctx.from());
        return compose(ctx, draft.getId(), ctx.t("appeal.to." + target.name()));
    }

    private BotView startReply(BotContext ctx, Long appealId) {
        Appeal a = appeals.ownAppeal(ctx.chatId(), appealId);
        return compose(ctx, a.getId(), "#" + a.getId());
    }

    private BotView compose(BotContext ctx, Long appealId, String to) {
        ctx.session().setPendingAction(PENDING);
        ctx.session().setPendingData(new PendingData().put("ap", appealId).toString());
        access.save(ctx.session());
        return BotView.of(ctx.crumb(ctx.t("sec.write"), to) + "\n" + ctx.childHeader()
                + ctx.t("appeal.prompt", "limit", AppealService.MESSAGES_PER_SENDING), composeKeyboard(ctx))
                .section("write");
    }

    private InlineKeyboardMarkup composeKeyboard(BotContext ctx) {
        return InlineKeyboardMarkup.builder()
                .row(ctx.btn(ctx.t("appeal.btn.send"), CallbackData.of("msg").with("a", "ok")),
                        ctx.btn(ctx.t("appeal.btn.cancel"), CallbackData.of("msg").with("a", "x")))
                .build();
    }

    private BotView send(BotContext ctx) {
        Long appealId = composingAppeal(ctx);
        if (appealId == null) return main(ctx).toast(ctx.t("appeal.empty"));
        Appeal sent;
        try {
            sent = appeals.submit(ctx.chatId(), appealId);
        } catch (Refused r) {
            // nothing was sent yet: stay in the composing mode
            return BotView.of(ctx.t(r.key, r.args), composeKeyboard(ctx)).asNewMessage().section("write");
        }
        clear(ctx);
        BotView page = main(ctx);
        return BotView.of(ctx.t("appeal.sent", "id", sent.getId()) + "\n\n" + page.text(), page.keyboard())
                .asNewMessage().section("write");
    }

    @Override
    public BotView handleInput(BotContext ctx, String text, String photoFileId) {
        // only reached through handleMessage below
        return main(ctx);
    }

    @Override
    public BotView handleMessage(BotContext ctx, TelegramModels.Message message) {
        Long appealId = composingAppeal(ctx);
        if (appealId == null) {
            clear(ctx);
            return main(ctx).asNewMessage();
        }
        Incoming in = Incoming.of(message);
        if (in == null) {
            return BotView.of(ctx.t("appeal.unsupported"), composeKeyboard(ctx)).asNewMessage().section("write");
        }
        int count;
        try {
            count = appeals.accept(ctx.chatId(), appealId, in);
        } catch (Refused r) {
            return BotView.of(ctx.t(r.key, r.args), composeKeyboard(ctx)).asNewMessage().section("write");
        }
        // an album arrives as several messages with one media_group_id — one ✓ for all of them
        PendingData pending = PendingData.parse(ctx.session().getPendingData());
        if (in.mediaGroupId() != null) {
            if (in.mediaGroupId().equals(pending.get("g"))) return null;
            ctx.session().setPendingData(pending.put("g", in.mediaGroupId()).toString());
            access.save(ctx.session());
            return BotView.of(ctx.t("appeal.ack_album", "count", count, "limit", AppealService.MESSAGES_PER_SENDING),
                    composeKeyboard(ctx)).asNewMessage().section(null);
        }
        return BotView.of(ctx.t("appeal.ack", "count", count, "limit", AppealService.MESSAGES_PER_SENDING),
                composeKeyboard(ctx)).asNewMessage().section(null);
    }

    /** The appeal being composed in this chat, or null. */
    private Long composingAppeal(BotContext ctx) {
        String action = ctx.session().getPendingAction();
        if (action == null || !action.startsWith(PENDING)) return null;
        return PendingData.parse(ctx.session().getPendingData()).getLong("ap");
    }

    private void clear(BotContext ctx) {
        ctx.session().setPendingAction(null);
        ctx.session().setPendingData(null);
        access.save(ctx.session());
    }

    private static String shorten(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1).stripTrailing() + "…" : s;
    }
}
