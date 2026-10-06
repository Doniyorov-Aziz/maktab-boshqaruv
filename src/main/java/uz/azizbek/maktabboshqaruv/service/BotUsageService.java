package uz.azizbek.maktabboshqaruv.service;

import jakarta.annotation.PreDestroy;
import uz.azizbek.maktabboshqaruv.entity.BotUsageEvent;
import uz.azizbek.maktabboshqaruv.entity.Student;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * "Which section did a parent open" for the bot statistics. Recording only puts
 * the event in memory; a background flush writes them in batches every few
 * seconds, so a bot update never waits for (or adds) an INSERT.
 */
@Service
public class BotUsageService {

    private static final Logger log = LoggerFactory.getLogger(BotUsageService.class);
    private static final int MAX_BUFFER = 50_000;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Autowired
    private Clock clock;

    private final ConcurrentLinkedQueue<BotUsageEvent> buffer = new ConcurrentLinkedQueue<>();

    public void record(long chatId, Student student, String section) {
        if (section == null || buffer.size() >= MAX_BUFFER) return;
        try {
            BotUsageEvent e = new BotUsageEvent();
            e.setChatId(chatId);
            if (student != null) {
                e.setStudentId(student.getId());
                e.setSchoolId(student.getSchoolClass().getAcademicYear().getSchool().getId());
            }
            e.setSection(section);
            e.setCreatedAt(LocalDateTime.now(clock));
            buffer.add(e);
        } catch (Exception ignored) {
            // statistics are best-effort; never break the parent's page
        }
    }

    /** Writes what has been collected (batched). Also called on shutdown. */
    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    @PreDestroy
    public void flush() {
        List<BotUsageEvent> batch = new ArrayList<>();
        BotUsageEvent e;
        while ((e = buffer.poll()) != null) batch.add(e);
        if (batch.isEmpty()) return;
        try {
            // a real JDBC batch (one round trip per 500 rows) — JPA saveAll would insert row by row (IDENTITY ids)
            jdbc.batchUpdate("insert into bot_usage_event (chat_id, school_id, student_id, section, created_at) " +
                            "values (?, ?, ?, ?, ?)", batch, 500, (ps, ev) -> {
                        ps.setLong(1, ev.getChatId());
                        ps.setObject(2, ev.getSchoolId());
                        ps.setObject(3, ev.getStudentId());
                        ps.setString(4, ev.getSection());
                        ps.setTimestamp(5, java.sql.Timestamp.valueOf(ev.getCreatedAt()));
                    });
        } catch (Exception ex) {
            log.debug("Bot statistikasi yozilmadi ({} ta): {}", batch.size(), ex.getMessage());
        }
    }
}
