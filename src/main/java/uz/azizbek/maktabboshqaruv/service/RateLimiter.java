package uz.azizbek.maktabboshqaruv.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Attempts per key (an IP address) in a sliding window, in memory: sign-in 10 per 5 minutes,
 * demo sign-in 30 per hour. {@code app.rate-limit.enabled=false} switches it off (tests log in
 * hundreds of times from one address).
 */
@Service
public class RateLimiter {

    public static final String MESSAGE = "Juda ko'p urinish. Birozdan so'ng qayta urinib ko'ring.";

    private final boolean enabled;
    private final Clock clock;
    private final Cache<String, Deque<Long>> hits = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(2))
            .maximumSize(100_000)
            .build();

    public RateLimiter(@Value("${app.rate-limit.enabled:true}") boolean enabled, Clock clock) {
        this.enabled = enabled;
        this.clock = clock;
    }

    /** Counts this attempt; false when the key already made {@code limit} attempts within {@code window}. */
    public boolean tryAcquire(String key, int limit, Duration window) {
        if (!enabled) return true;
        long now = clock.millis();
        Deque<Long> times = hits.get(key, k -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst() <= now - window.toMillis()) times.pollFirst();
            if (times.size() >= limit) return false;
            times.addLast(now);
            return true;
        }
    }
}
