package uz.azizbek.maktabboshqaruv.bot;

import uz.azizbek.maktabboshqaruv.telegram.TelegramModels;

/**
 * A screen that takes typed input while a conversation is open
 * ({@code ParentSession.pendingAction} starts with one of its prefixes).
 */
public interface InputHandler {

    /** e.g. "APPEAL", "ABS_", "ADD_CHILD". */
    java.util.List<String> pendingPrefixes();

    /** Handles the parent's text (or photo caption) and/or photo; returns the page to show next. */
    BotView handleInput(BotContext ctx, String text, String photoFileId);

    /**
     * The whole message (video, voice, document, album part, …). Screens that only need
     * text and photos keep the default; the appeal screen takes every kind of message.
     */
    default BotView handleMessage(BotContext ctx, TelegramModels.Message message) {
        String text = message.text() != null ? message.text().trim()
                : message.caption() != null ? message.caption().trim() : "";
        return handleInput(ctx, text, message.largestPhotoId());
    }
}
