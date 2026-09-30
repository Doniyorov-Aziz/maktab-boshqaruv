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
 * rule: a button tap edits the tapped message in place (editMessageText), so
 * the chat never fills up. Only commands, typed input and pictures create new
 * messages. If editing is impossible (the tapped message is a photo, or too
 * old), the old message is removed and the page is sent fresh.
 */
@Component
public class BotResponder {

    private static final Logger log = LoggerFactory.getLogger(BotResponder.class);
    private static final int MAX_TEXT = 4000;

    @Autowired
    private TelegramClient client;

    @Autowired
    private BotImageService images;

    public void deliver(long chatId, Long messageId, BotView view) {
        if (view.prefaceText() != null) {
            client.sendMessage(chatId, view.prefaceText(), view.prefaceKeyboard());
        }
        if (view.photo() != null) {
            client.sendChatAction(chatId, "upload_photo");
            String key = view.photo().cacheKey();
            String cached = images.cachedFileId(key);
            TelegramClient.SentPhoto sent = null;
            if (cached != null) {
                try {
                    sent = client.sendPhotoById(chatId, cached, view.text(), view.keyboard());
                } catch (TelegramApiException e) {
                    log.debug("Keshlangan rasm qayta yuborilmadi, yangidan yuklanadi: {}", TokenMasker.mask(e.getMessage()));
                }
            }
            if (sent == null) {
                sent = client.sendPhoto(chatId, view.photo().png(), view.photo().fileName(), view.text(), view.keyboard());
            }
            images.rememberFileId(key, sent.fileId());
            return;
        }
        if (view.text() == null) return;

        String text = view.text().length() > MAX_TEXT ? view.text().substring(0, MAX_TEXT) + "…" : view.text();
        if (!view.newMessage() && messageId != null) {
            try {
                client.editMessageText(chatId, messageId, text, view.keyboard());
                return;
            } catch (TelegramApiException e) {
                String msg = e.getMessage() == null ? "" : e.getMessage();
                if (msg.contains("message is not modified")) return;
                try {
                    client.deleteMessage(chatId, messageId);
                } catch (Exception ignored) {
                    // the old page just stays — the new one is sent below anyway
                }
            }
        }
        client.sendMessage(chatId, text, view.replyKeyboard() != null ? view.replyKeyboard() : view.keyboard());
    }
}
