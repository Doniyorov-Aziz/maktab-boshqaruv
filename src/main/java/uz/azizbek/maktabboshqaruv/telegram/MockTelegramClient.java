package uz.azizbek.maktabboshqaruv.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * telegram.mock=true: nothing leaves the machine. Messages are only logged
 * (the sender still marks outbox rows SENT, so the database shows the full
 * flow). Bot replies are also kept in memory so the simulate endpoint can
 * show what the parent would have seen.
 */
public class MockTelegramClient implements TelegramClient {

    private static final Logger log = LoggerFactory.getLogger(MockTelegramClient.class);
    private static final int KEEP = 200;

    public record SentMessage(long chatId, String text, boolean hasKeyboard) {
    }

    private final List<SentMessage> sent = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void sendMessage(long chatId, String html, Object replyMarkup) {
        log.info("[MOCK TELEGRAM] chat={} -> {}", chatId, html.replace("\n", " ⏎ "));
        sent.add(new SentMessage(chatId, html, replyMarkup != null));
        while (sent.size() > KEEP) {
            sent.remove(0);
        }
    }

    @Override
    public List<TelegramModels.Update> getUpdates(long offset, int timeoutSeconds) {
        return List.of();
    }

    /** Messages sent to a chat after the given index — used to return the bot's reply to a simulated update. */
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
