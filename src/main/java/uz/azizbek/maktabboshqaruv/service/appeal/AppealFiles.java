package uz.azizbek.maktabboshqaruv.service.appeal;

import jakarta.annotation.PreDestroy;
import uz.azizbek.maktabboshqaruv.entity.AppealEnums;
import uz.azizbek.maktabboshqaruv.entity.AppealMessage;
import uz.azizbek.maktabboshqaruv.repository.AppealMessageRepository;
import uz.azizbek.maktabboshqaruv.telegram.TelegramClient;
import uz.azizbek.maktabboshqaruv.telegram.TokenMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

/**
 * Copies a file a parent sent from Telegram to our own storage, on a separate small
 * pool (the bot never waits for a download). The Telegram download link contains the
 * bot token, so it exists only inside the HTTP client for the moment of the download:
 * it is never stored, logged or returned — the database keeps only file_id and our
 * own storage path.
 */
@Service
public class AppealFiles {

    private static final Logger log = LoggerFactory.getLogger(AppealFiles.class);

    private final AppealMessageRepository messages;
    private final TelegramClient telegram;
    private final FileStorage storage;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(2, 4, 60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1000), r -> {
        Thread t = new Thread(r, "appeal-files");
        t.setDaemon(true);
        return t;
    }, new ThreadPoolExecutor.DiscardPolicy()); // a dropped job stays PENDING and is retried below

    public AppealFiles(AppealMessageRepository messages, TelegramClient telegram, FileStorage storage,
                       PlatformTransactionManager txManager, Clock clock) {
        this.messages = messages;
        this.telegram = telegram;
        this.storage = storage;
        this.tx = new TransactionTemplate(txManager);
        this.clock = clock;
    }

    /** Queues the download of a just-saved PENDING message. */
    public void download(Long messageId) {
        pool.execute(() -> fetch(messageId));
    }

    private record Job(String fileId, Long schoolId, AppealEnums.Kind kind, String name, String mime) {
    }

    /** Synchronous (used by the pool and by tests). */
    public void fetch(Long messageId) {
        Job job = tx.execute(s -> messages.findById(messageId)
                .filter(m -> m.getFileState() == AppealEnums.FileState.PENDING && m.getFileId() != null)
                .map(m -> new Job(m.getFileId(), m.getAppeal().getSchool().getId(), m.getKind(),
                        m.getOriginalName(), m.getMimeType()))
                .orElse(null));
        if (job == null) return;
        String path = null;
        String error = null;
        try {
            byte[] bytes = telegram.downloadFile(job.fileId());
            if (bytes.length > AttachmentPolicy.MAX_BYTES) {
                error = "juda katta";
            } else {
                path = storage.save(job.schoolId(), AttachmentPolicy.extensionOf(job.kind(), job.name(), job.mime()), bytes);
            }
        } catch (Exception e) {
            error = TokenMasker.mask(e.getMessage());
        }
        String stored = path;
        String failure = error;
        tx.executeWithoutResult(s -> messages.findById(messageId).ifPresent(m -> {
            if (stored != null) {
                m.setStoragePath(stored);
                m.setFileState(AppealEnums.FileState.STORED);
            } else {
                m.setFileState(AppealEnums.FileState.FAILED);
            }
            messages.save(m);
        }));
        if (failure != null) log.warn("Murojaat fayli #{} yuklab olinmadi: {}", messageId, failure);
    }

    /** Files still PENDING after two minutes (restart, full queue, Telegram hiccup) are tried again. */
    @Scheduled(fixedDelay = 120_000, initialDelay = 60_000)
    public void retryPending() {
        List<AppealMessage> stuck = messages.findByFileStateAndCreatedAtBefore(AppealEnums.FileState.PENDING,
                LocalDateTime.now(clock).minusMinutes(2));
        for (AppealMessage m : stuck.stream().limit(100).toList()) download(m.getId());
    }

    @PreDestroy
    void stop() {
        pool.shutdown();
    }
}
