package uz.azizbek.maktabboshqaruv.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Hibernate creates a CHECK (type in (...)) constraint for every
 * {@code @Enumerated(STRING)} column when it first creates a table, but
 * ddl-auto=update never widens it later. Databases created by the first bot
 * version would therefore reject the new notification types (TOMORROW_SCHEDULE,
 * WEEKLY_REPORT, ...). These enum columns are written only by our own code,
 * so the stale constraints are simply dropped, once, at startup.
 */
@Component
@Order(0)
public class TelegramSchemaMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TelegramSchemaMigration.class);

    private static final List<String[]> STALE_CHECKS = List.of(
            new String[]{"notification_log", "notification_log_type_check"},
            new String[]{"notification_log", "notification_log_status_check"},
            // v4: "tanlangan ota-onalar" (PARENTS) audience
            new String[]{"broadcast", "broadcast_audience_check"}
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        for (String[] check : STALE_CHECKS) {
            try {
                Integer exists = jdbcTemplate.queryForObject(
                        "select count(*) from pg_constraint where conname = ? and conrelid = to_regclass(?)",
                        Integer.class, check[1], check[0]);
                if (exists != null && exists > 0) {
                    jdbcTemplate.execute("alter table " + check[0] + " drop constraint " + check[1]);
                    log.info("Migratsiya: {}.{} cheklovi olib tashlandi (yangi xabarnoma turlari uchun)", check[0], check[1]);
                }
            } catch (Exception e) {
                log.warn("Migratsiya: {} cheklovini tekshirib bo'lmadi: {}", check[1], e.getMessage());
            }
        }
    }
}
