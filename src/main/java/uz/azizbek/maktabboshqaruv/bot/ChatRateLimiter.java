package uz.azizbek.maktabboshqaruv.bot;

import uz.azizbek.maktabboshqaruv.telegram.TelegramProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * At most telegram.chat-rate-per-second (default 2) updates per chat per
 * second. Extra taps get a friendly "slow down" instead of hammering the
 * database — and one chat cannot starve the others.
 */
@Component
public class ChatRateLimiter {

    @Autowired
    private TelegramProperties properties;

    @Autowired
    private Clock clock;

    private final Map<Long, Deque<Long>> hits = new ConcurrentHashMap<>();

    public boolean allow(long chatId) {
        long now = clock.millis();
        Deque<Long> q = hits.computeIfAbsent(chatId, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() >= 1000) q.pollFirst();
            if (q.size() >= Math.max(1, properties.getChatRatePerSecond())) return false;
            q.addLast(now);
            return true;
        }
    }
}
