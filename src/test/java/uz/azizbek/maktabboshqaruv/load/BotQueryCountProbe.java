package uz.azizbek.maktabboshqaruv.load;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import uz.azizbek.maktabboshqaruv.bot.BotRouter;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels;

import java.time.LocalDateTime;

/** Diagnostic for 10.2: statements per update kind, and which entities are loaded one by one. */
@Tag("load")
@SpringBootTest(properties = {
        "telegram.mock=true",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.uz.azizbek=WARN"
})
@ActiveProfiles("local")
class BotQueryCountProbe {

    private static final long CHAT = 870_000_001L;

    @Autowired
    private BotRouter router;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private StudentRepository students;
    @Autowired
    private EntityManagerFactory emf;

    private static TelegramModels.User user() {
        return new TelegramModels.User(CHAT, false, "Ota", "ota", "uz");
    }

    @Test
    void probe() {
        jdbc.update("delete from parent_telegram_link where chat_id = ?", CHAT);
        jdbc.update("insert into parent_telegram_link (student_id, chat_id, first_name, linked_at, active) values (?,?,?,?,true)",
                students.findAll().get(0).getId(), CHAT, "Ota", java.sql.Timestamp.valueOf(LocalDateTime.now()));
        Statistics st = emf.unwrap(SessionFactory.class).getStatistics();
        try {
            for (int round = 0; round < 2; round++) {
                run(st, "start", new TelegramModels.Update(1, new TelegramModels.Message(1L, user(),
                        new TelegramModels.Chat(CHAT, "private"), "/start", null)));
                for (String cb : new String[]{"home", "sch", "gr", "att"}) {
                    TelegramModels.Message card = new TelegramModels.Message(99L, user(), new TelegramModels.Chat(CHAT, "private"), null, null);
                    run(st, cb, new TelegramModels.Update(2, null, new TelegramModels.CallbackQuery("x", user(), card, cb)));
                }
            }
        } finally {
            jdbc.update("delete from bot_usage_event where chat_id = ?", CHAT);
            jdbc.update("delete from parent_session where chat_id = ?", CHAT);
            jdbc.update("delete from parent_telegram_link where chat_id = ?", CHAT);
        }
    }

    private void run(Statistics st, String name, TelegramModels.Update u) {
        try {
            Thread.sleep(1100); // stay under the 3-per-second chat limit
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        st.clear();
        router.handle(u);
        StringBuilder entities = new StringBuilder();
        for (String e : st.getEntityNames()) {
            long loads = st.getEntityStatistics(e).getLoadCount() + st.getEntityStatistics(e).getFetchCount();
            if (loads > 0) entities.append(e.substring(e.lastIndexOf('.') + 1)).append('=').append(loads).append(' ');
        }
        System.out.println("PROBE " + name + ": statements=" + st.getPrepareStatementCount()
                + " queries=" + st.getQueryExecutionCount() + " | " + entities);
        for (String q : st.getQueries()) {
            long n = st.getQueryStatistics(q).getExecutionCount();
            if (n > 0) System.out.println("   q x" + n + ": " + q.substring(0, Math.min(150, q.length())));
        }
    }
}
