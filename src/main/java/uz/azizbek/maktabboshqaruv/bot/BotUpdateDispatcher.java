package uz.azizbek.maktabboshqaruv.bot;

import jakarta.annotation.PreDestroy;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels;
import uz.azizbek.maktabboshqaruv.telegram.TokenMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs updates on a bounded worker pool so the long-polling thread never waits
 * for a handler (database, Telegram calls). Updates of one chat stay in order —
 * each chat has a small chain, the next update of that chat starts when the
 * previous one is done — while different chats run in parallel.
 *
 * The queue is bounded: when it is full the polling thread runs the update
 * itself (back-pressure: polling slows down instead of memory growing).
 */
@Component
public class BotUpdateDispatcher {

    private static final Logger log = LoggerFactory.getLogger(BotUpdateDispatcher.class);

    private final BotRouter router;
    private final ThreadPoolExecutor pool;
    private final ConcurrentHashMap<Long, CompletableFuture<Void>> tails = new ConcurrentHashMap<>();

    public BotUpdateDispatcher(BotRouter router,
                               @Value("${telegram.update-threads:8}") int threads,
                               @Value("${telegram.update-max-threads:16}") int maxThreads,
                               @Value("${telegram.update-queue:2000}") int queue) {
        this.router = router;
        AtomicInteger n = new AtomicInteger();
        this.pool = new ThreadPoolExecutor(threads, Math.max(threads, maxThreads), 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queue),
                r -> {
                    Thread t = new Thread(r, "bot-update-" + n.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /** Queues one update; returns a future that completes when it has been handled. */
    public CompletableFuture<Void> dispatch(TelegramModels.Update update) {
        long chatId = chatOf(update);
        CompletableFuture<Void> next = tails.compute(chatId, (k, tail) -> {
            CompletableFuture<Void> previous = tail == null ? CompletableFuture.completedFuture(null) : tail;
            // handle*, not then*: a failed update must not block the chat's next one
            return previous.handleAsync((r, e) -> {
                run(update);
                return null;
            }, pool);
        });
        next.whenComplete((r, e) -> tails.remove(chatId, next));
        return next;
    }

    private void run(TelegramModels.Update update) {
        try {
            router.handle(update);
        } catch (Exception e) {
            log.warn("Telegram update #{} ni qayta ishlashda xato: {}", update.updateId(), TokenMasker.mask(e.getMessage()));
        }
    }

    private static long chatOf(TelegramModels.Update u) {
        if (u.message() != null && u.message().chat() != null) return u.message().chat().id();
        if (u.callbackQuery() != null && u.callbackQuery().message() != null
                && u.callbackQuery().message().chat() != null) return u.callbackQuery().message().chat().id();
        return 0L;
    }

    public int queued() {
        return pool.getQueue().size();
    }

    public int active() {
        return pool.getActiveCount();
    }

    @PreDestroy
    void stop() {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) pool.shutdownNow();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pool.shutdownNow();
        }
    }
}
