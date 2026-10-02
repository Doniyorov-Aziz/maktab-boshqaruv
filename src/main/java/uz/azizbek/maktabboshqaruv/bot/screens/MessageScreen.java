package uz.azizbek.maktabboshqaruv.bot.screens;

import uz.azizbek.maktabboshqaruv.bot.*;
import uz.azizbek.maktabboshqaruv.entity.ParentMessageRecipient;
import uz.azizbek.maktabboshqaruv.service.ParentMessageService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentAccessService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentDataService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentViews.MessageView;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineButton;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineKeyboardMarkup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static uz.azizbek.maktabboshqaruv.bot.BotContext.e;

/**
 * 💬 Maktabga yozish. Pick a recipient (a=to, to=CT|AD) → the next text or
 * photo the parent sends becomes the message. Shows the latest messages and
 * the school's replies.
 */
@Component
public class MessageScreen implements Screen, InputHandler {

    static final String PENDING = "MSG";

    @Autowired
    private ParentDataService data;

    @Autowired
    private ParentAccessService access;

    @Autowired
    private ParentMessageService messages;

    private static String shorten(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1).stripTrailing() + "…" : s;
    }

    @Override
    public String code() {
        return "msg";
    }

    @Override
    public List<String> pendingPrefixes() {
        return List.of(PENDING);
    }

    @Override
    public BotView render(BotContext ctx, CallbackData cb) {
        String action = cb.get("a");
        if ("to".equals(action)) {
            ParentMessageRecipient to = "AD".equals(cb.get("to")) ? ParentMessageRecipient.ADMINISTRATION : ParentMessageRecipient.CLASS_TEACHER;
            ctx.session().setPendingAction(PENDING);
            ctx.session().setPendingData(new PendingData().put("to", to.name()).put("sid", ctx.student().getId()).toString());
            access.save(ctx.session());
            String text = ctx.crumb(ctx.t("sec.write"), ctx.t("msg.to." + to.name())) + "\n" + ctx.childHeader() + "\n"
                    + ctx.t("msg.prompt", "to", ctx.t("msg.to." + to.name()));
            return BotView.of(text, InlineKeyboardMarkup.builder()
                    .row(ctx.btn(ctx.t("common.cancel"), CallbackData.of("msg").with("a", "x"))).build()).section("write");
        }
        if ("x".equals(action)) {
            clear(ctx);
            return main(ctx).toast(ctx.t("msg.cancelled"));
        }
        return main(ctx);
    }

    BotView main(BotContext ctx) {
        StringBuilder sb = new StringBuilder(ctx.crumb(ctx.t("sec.write"))).append("\n").append(ctx.childHeader()).append("\n")
                .append(ctx.t("msg.title"));
        List<MessageView> history = data.messages(ctx.student(), ctx.chatId());
        if (!history.isEmpty()) {
            sb.append("\n\n").append(ctx.t("msg.history_title")).append("\n");
            // The last three, shortened, so the page still fits a card caption (1024 characters).
            for (MessageView m : history.stream().limit(3).toList()) {
                String reply = m.replyText() == null ? null : shorten(m.replyText(), 110);
                sb.append(ctx.t("msg.history_item", "status", ctx.t("msg.status." + m.status()),
                        "date", ctx.dateShort(m.createdAt().toLocalDate()), "to", ctx.t("msg.to." + m.recipient()),
                        "text", e(shorten(m.text(), 60)),
                        "reply", reply == null ? "" : ctx.t("msg.history_reply", "text", e(reply))))
                        .append("\n");
            }
        }
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .row(ctx.btn(ctx.t("msg.to.CLASS_TEACHER"), CallbackData.of("msg").with("a", "to").with("to", "CT")),
                        ctx.btn(ctx.t("msg.to.ADMINISTRATION"), CallbackData.of("msg").with("a", "to").with("to", "AD")))
                .row(ctx.switchRow("msg").toArray(new InlineButton[0]))
                .row(ctx.navRow(null).toArray(new InlineButton[0]))
                .build();
        return BotView.of(sb.toString().stripTrailing(), kb).section("write");
    }

    @Override
    public BotView handleInput(BotContext ctx, String text, String photoFileId) {
        if ((text == null || text.isBlank()) && photoFileId == null) {
            return BotView.of(ctx.t("msg.empty_text"), InlineKeyboardMarkup.builder()
                    .row(ctx.btn(ctx.t("common.cancel"), CallbackData.of("msg").with("a", "x"))).build()).asNewMessage();
        }
        PendingData pending = PendingData.parse(ctx.session().getPendingData());
        ParentMessageRecipient to = ParentMessageRecipient.valueOf(pending.get("to"));
        // The student was checked when the conversation started; check again — links can change meanwhile.
        messages.createFromParent(access.requireLinked(ctx.chatId(), pending.getLong("sid")), ctx.chatId(), ctx.from(),
                to, text, photoFileId);
        clear(ctx);
        BotView page = main(ctx);
        return BotView.of(ctx.t("msg.sent") + "\n\n" + page.text(), page.keyboard()).asNewMessage().section("write");
    }

    private void clear(BotContext ctx) {
        ctx.session().setPendingAction(null);
        ctx.session().setPendingData(null);
        access.save(ctx.session());
    }
}
