package uz.azizbek.maktabboshqaruv.bot;

/**
 * A screen that also takes typed input while a conversation is open
 * (ParentSession.pendingAction starts with one of {@link #pendingPrefixes()}).
 */
public interface InputHandler {

    /** e.g. "MSG", "ABS_", "ADD_CHILD". */
    java.util.List<String> pendingPrefixes();

    /** Handles the parent's text (or photo caption) and/or photo; returns the page to show next. */
    BotView handleInput(BotContext ctx, String text, String photoFileId);
}
