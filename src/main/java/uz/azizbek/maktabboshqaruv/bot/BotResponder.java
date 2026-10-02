package uz.azizbek.maktabboshqaruv.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.azizbek.maktabboshqaruv.bot.image.BotImageService;
import uz.azizbek.maktabboshqaruv.telegram.TelegramApiException;
import uz.azizbek.maktabboshqaruv.telegram.TelegramClient;
import uz.azizbek.maktabboshqaruv.telegram.TokenMasker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Puts a {@link BotView} on screen following the "one page = one message"
 * rule. A page with a section banner is a card: the banner photo with the page
 * text as its caption; moving between cards replaces the photo in place
 * (editMessageMedia), so the chat never fills up. Pages whose text is longer
 * than a caption allows (1024 characters) are plain text messages, edited
 * with editMessageText. When the tapped message cannot be edited into the new
 * kind (text → card or card → text), it is removed and the page sent fresh.
 */
@Component
public class BotResponder {

    private static final Logger log = LoggerFactory.getLogger(BotResponder.class);
    private static final int MAX_TEXT = 4000;
    /** Telegram's caption limit, counted after HTML entities are parsed. */
    static final int MAX_CAPTION = 1024;

    @Autowired
    private TelegramClient client;

    @Autowired
    private BotImageService images;

    public void deliver(long chatId, Long messageId, BotView view) {
        deliver(chatId, messageId, view, false);
    }

    /** @param currentIsPhoto the tapped message is a photo (a card), so it can only be edited as media */
    public void deliver(long chatId, Long messageId, BotView view, boolean currentIsPhoto) {
        if (view.prefaceText() != null) {
            client.sendMessage(chatId, view.prefaceText(), view.prefaceKeyboard());
        }
        if (view.photo() != null) {
            client.sendChatAction(chatId, "upload_photo");
            sendPhoto(chatId, view.photo(), view.text(), view.keyboard());
            return;
        }
        if (view.text() == null) return;

        boolean edit = !view.newMessage() && messageId != null;
        if (view.banner() != null && fitsCaption(view.text())) {
            Object markup = view.keyboard() != null ? view.keyboard() : view.replyKeyboard();
            if (edit && currentIsPhoto && editCard(chatId, messageId, view)) return;
            if (edit) safeDelete(chatId, messageId);
            sendPhoto(chatId, view.banner(), view.text(), markup);
            return;
        }

        String text = view.text().length() > MAX_TEXT ? view.text().substring(0, MAX_TEXT) + "…" : view.text();
        if (edit && !currentIsPhoto) {
            try {
                client.editMessageText(chatId, messageId, text, view.keyboard());
                return;
            } catch (TelegramApiException e) {
                if (notModified(e)) return;
                safeDelete(chatId, messageId);
            }
        } else if (edit) {
            safeDelete(chatId, messageId);
        }
        client.sendMessage(chatId, text, view.replyKeyboard() != null ? view.replyKeyboard() : view.keyboard());
    }

    /** Replaces the banner and caption of the tapped card. Returns false if a fresh message is needed. */
    private boolean editCard(long chatId, long messageId, BotView view) {
        BotImageService.Rendered banner = view.banner();
        String cached = images.cachedFileId(banner.cacheKey());
        try {
            TelegramClient.SentPhoto sent = client.editMessageMedia(chatId, messageId, cached,
                    cached == null ? banner.png() : null, banner.fileName(), view.text(), view.keyboard());
            images.rememberFileId(banner.cacheKey(), sent.fileId());
            return true;
        } catch (TelegramApiException e) {
            if (notModified(e)) return true;
            log.debug("Kartochkani tahrirlab bo'lmadi, yangisi yuboriladi: {}", TokenMasker.mask(e.getMessage()));
            return false;
        }
    }

    private void sendPhoto(long chatId, BotImageService.Rendered photo, String caption, Object markup) {
        String key = photo.cacheKey();
        String cached = images.cachedFileId(key);
        TelegramClient.SentPhoto sent = null;
        if (cached != null) {
            try {
                sent = client.sendPhotoById(chatId, cached, caption, markup);
            } catch (TelegramApiException e) {
                log.debug("Keshlangan rasm qayta yuborilmadi, yangidan yuklanadi: {}", TokenMasker.mask(e.getMessage()));
            }
        }
        if (sent == null) {
            sent = client.sendPhoto(chatId, photo.png(), photo.fileName(), caption, markup);
        }
        images.rememberFileId(key, sent.fileId());
    }

    private void safeDelete(long chatId, long messageId) {
        try {
            client.deleteMessage(chatId, messageId);
        } catch (Exception ignored) {
            // the old page just stays — the new one is sent anyway
        }
    }

    private static boolean notModified(TelegramApiException e) {
        return e.getMessage() != null && e.getMessage().contains("message is not modified");
    }

    /** Visible length of an HTML caption: tags removed, entities counted as one character. */
    static boolean fitsCaption(String html) {
        String visible = html.replaceAll("<[^>]+>", "")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&amp;", "&");
        return visible.length() <= MAX_CAPTION;
    }
}
