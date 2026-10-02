package uz.azizbek.maktabboshqaruv.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * telegram.mock=true: nothing leaves the machine. Every outgoing operation
 * (send, edit, photo, delete, callback answer) is logged and kept in memory,
 * so the simulate endpoint and the demo script can show exactly what a parent
 * would see — text, buttons and rendered images.
 */
public class MockTelegramClient implements TelegramClient {

    private static final Logger log = LoggerFactory.getLogger(MockTelegramClient.class);
    private static final int KEEP = 500;

    /** One recorded outgoing operation. {@code keyboard} is the JSON of the reply markup, if any. */
    public record SentMessage(long chatId, String text, boolean hasKeyboard, String op, Long messageId,
                              String keyboard, String photoId) {
        public SentMessage(long chatId, String text, boolean hasKeyboard) {
            this(chatId, text, hasKeyboard, "send", null, null, null);
        }
    }

    private final List<SentMessage> sent = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, byte[]> files = new ConcurrentHashMap<>();
    private final AtomicLong messageIds = new AtomicLong(1000);
    private final AtomicLong fileIds = new AtomicLong(1);

    @Override
    public Long sendMessage(long chatId, String html, Object replyMarkup) {
        long id = messageIds.incrementAndGet();
        log.info("[MOCK TELEGRAM] chat={} -> {}", chatId, html.replace("\n", " ⏎ "));
        record(new SentMessage(chatId, html, replyMarkup != null, "send", id, TelegramJson.write(replyMarkup), null));
        return id;
    }

    @Override
    public void editMessageText(long chatId, long messageId, String html, Object replyMarkup) {
        log.info("[MOCK TELEGRAM] chat={} edit #{} -> {}", chatId, messageId, html.replace("\n", " ⏎ "));
        record(new SentMessage(chatId, html, replyMarkup != null, "edit", messageId, TelegramJson.write(replyMarkup), null));
    }

    @Override
    public SentPhoto sendPhoto(long chatId, byte[] png, String fileName, String captionHtml, Object replyMarkup) {
        String fileId = "mock-photo-" + fileIds.getAndIncrement();
        files.put(fileId, png);
        long id = messageIds.incrementAndGet();
        log.info("[MOCK TELEGRAM] chat={} photo {} ({} bayt) {}", chatId, fileName, png.length, captionHtml);
        record(new SentMessage(chatId, captionHtml, replyMarkup != null, "photo", id, TelegramJson.write(replyMarkup), fileId));
        return new SentPhoto(id, fileId);
    }

    @Override
    public SentPhoto sendPhotoById(long chatId, String fileId, String captionHtml, Object replyMarkup) {
        long id = messageIds.incrementAndGet();
        record(new SentMessage(chatId, captionHtml, replyMarkup != null, "photo", id, TelegramJson.write(replyMarkup), fileId));
        return new SentPhoto(id, fileId);
    }

    @Override
    public SentPhoto editMessageMedia(long chatId, long messageId, String fileId, byte[] png, String fileName,
                                      String captionHtml, Object replyMarkup) {
        String id = fileId;
        if (id == null) {
            id = "mock-photo-" + fileIds.getAndIncrement();
            files.put(id, png);
        }
        log.info("[MOCK TELEGRAM] chat={} media #{} -> {} {}", chatId, messageId, id, captionHtml);
        record(new SentMessage(chatId, captionHtml, replyMarkup != null, "media", messageId, TelegramJson.write(replyMarkup), id));
        return new SentPhoto(messageId, id);
    }

    @Override
    public void editMessageCaption(long chatId, long messageId, String captionHtml, Object replyMarkup) {
        record(new SentMessage(chatId, captionHtml, replyMarkup != null, "caption", messageId, TelegramJson.write(replyMarkup), null));
    }

    @Override
    public void deleteMessage(long chatId, long messageId) {
        record(new SentMessage(chatId, null, false, "delete", messageId, null, null));
    }

    @Override
    public void answerCallbackQuery(String callbackQueryId, String text, boolean showAlert) {
        if (text != null) log.info("[MOCK TELEGRAM] callback {} -> {}", callbackQueryId, text);
    }

    @Override
    public List<TelegramModels.Update> getUpdates(long offset, int timeoutSeconds) {
        return List.of();
    }

    @Override
    public byte[] downloadFile(String fileId) {
        byte[] bytes = files.get(fileId);
        if (bytes == null) throw new TelegramApiException(404, "Fayl topilmadi", null);
        return bytes;
    }

    /** Simulates a parent uploading a photo; returns the file_id the bot will see. */
    public String storeIncomingFile(byte[] bytes) {
        String fileId = "mock-upload-" + fileIds.getAndIncrement();
        files.put(fileId, bytes);
        return fileId;
    }

    private void record(SentMessage m) {
        sent.add(m);
        synchronized (sent) {
            while (sent.size() > KEEP) sent.remove(0);
        }
    }

    /** Operations for a chat after the given index — the bot's reaction to one simulated update. */
    public List<SentMessage> since(int index, long chatId) {
        synchronized (sent) {
            List<SentMessage> result = new ArrayList<>();
            for (int i = Math.max(0, index); i < sent.size(); i++) {
                if (sent.get(i).chatId() == chatId) result.add(sent.get(i));
            }
            return result;
        }
    }

    public int size() {
        return sent.size();
    }
}
