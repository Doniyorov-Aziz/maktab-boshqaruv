package uz.azizbek.maktabboshqaruv.bot;

import uz.azizbek.maktabboshqaruv.bot.image.BotImageService;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels;

/**
 * What a screen wants to show: one message (text + inline keyboard), or a
 * photo page. {@code section} feeds the usage statistics; {@code toast} is the
 * short text shown on the tapped button (answerCallbackQuery).
 */
public final class BotView {

    private final String text;
    private final TelegramModels.InlineKeyboardMarkup keyboard;
    private BotImageService.Rendered photo;
    private boolean newMessage;
    private String section;
    private String toast;
    private Object replyKeyboard;
    private String redirect;
    private String prefaceText;
    private Object prefaceKeyboard;

    /** Render another screen instead (e.g. back to where "switch child" was pressed). */
    public static BotView redirect(String screen, String toast) {
        BotView v = new BotView(null, null);
        v.redirect = screen;
        v.toast = toast;
        return v;
    }

    /**
     * A short message sent before this view — used to (re)install the bottom
     * keyboard, which Telegram only accepts on a message without inline buttons.
     */
    public BotView preface(String text, Object replyKeyboard) {
        this.prefaceText = text;
        this.prefaceKeyboard = replyKeyboard;
        this.newMessage = true;
        return this;
    }

    public String redirectScreen() {
        return redirect;
    }

    public String prefaceText() {
        return prefaceText;
    }

    public Object prefaceKeyboard() {
        return prefaceKeyboard;
    }

    private BotView(String text, TelegramModels.InlineKeyboardMarkup keyboard) {
        this.text = text;
        this.keyboard = keyboard;
    }

    public static BotView of(String text, TelegramModels.InlineKeyboardMarkup keyboard) {
        return new BotView(text, keyboard);
    }

    public static BotView photo(BotImageService.Rendered photo, String caption, TelegramModels.InlineKeyboardMarkup keyboard) {
        BotView v = new BotView(caption, keyboard);
        v.photo = photo;
        v.newMessage = true;
        return v;
    }

    /** Send as a fresh message instead of editing the tapped one (e.g. after a typed command). */
    public BotView asNewMessage() {
        this.newMessage = true;
        return this;
    }

    public BotView section(String section) {
        this.section = section;
        return this;
    }

    public BotView toast(String toast) {
        this.toast = toast;
        return this;
    }

    /** Also (re)install the persistent reply keyboard — only possible on a new message. */
    public BotView withReplyKeyboard(Object replyKeyboard) {
        this.replyKeyboard = replyKeyboard;
        this.newMessage = true;
        return this;
    }

    public String text() {
        return text;
    }

    public TelegramModels.InlineKeyboardMarkup keyboard() {
        return keyboard;
    }

    public BotImageService.Rendered photo() {
        return photo;
    }

    public boolean newMessage() {
        return newMessage;
    }

    public String section() {
        return section;
    }

    public String toast() {
        return toast;
    }

    public Object replyKeyboard() {
        return replyKeyboard;
    }
}
