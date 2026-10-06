package uz.azizbek.maktabboshqaruv.load;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import uz.azizbek.maktabboshqaruv.bot.BotRouter;
import uz.azizbek.maktabboshqaruv.bot.BotUpdateDispatcher;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 10.3 — 3000 simulated parents against the real bot code, Telegram mocked (nothing
 * leaves the machine). Each parent sends /start and opens schedule, grades and
 * attendance (12 000 updates) at a steady 200 updates/s through the same worker
 * pool as production. Measures the response time of an update (avg, p95, p99 —
 * without Telegram's network time), database statements per update, heap and CPU.
 *
 * Not part of `test`: run with `gradlew loadTest` (writes build/load-test-result.txt).
 */
@Tag("load")
@SpringBootTest(properties = {
        "telegram.mock=true",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.show-sql=false",
        "logging.level.uz.azizbek=WARN",
        "logging.level.org.hibernate=WARN"
})
@ActiveProfiles("local")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BotLoadTest {

    private static final int USERS = 3000;
    private static final long CHAT_BASE = 880_000_000L;
    private static final int RATE_PER_SECOND = 200;
    private static final String[] CALLBACKS = {"sch", "gr", "att"};

    @Autowired
    private BotUpdateDispatcher dispatcher;
    @Autowired
    private BotRouter router;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EntityManagerFactory emf;
    @Autowired
    private uz.azizbek.maktabboshqaruv.service.BotUsageService usage;

    @BeforeEach
    void link3000Parents() {
        cleanUp();
        List<Long> students = studentRepository.findAll().stream().map(s -> s.getId()).toList();
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < USERS; i++) {
            rows.add(new Object[]{students.get(i % students.size()), CHAT_BASE + i, "Ota " + i,
                    java.sql.Timestamp.valueOf(LocalDateTime.now()), true});
        }
        jdbc.batchUpdate("insert into parent_telegram_link (student_id, chat_id, first_name, linked_at, active) " +
                "values (?, ?, ?, ?, ?)", rows);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("delete from bot_usage_event where chat_id between ? and ?", CHAT_BASE, CHAT_BASE + USERS);
        jdbc.update("delete from parent_session where chat_id between ? and ?", CHAT_BASE, CHAT_BASE + USERS);
        jdbc.update("delete from parent_telegram_link where chat_id between ? and ?", CHAT_BASE, CHAT_BASE + USERS);
    }

    private static TelegramModels.User user(long chatId) {
        return new TelegramModels.User(chatId, false, "Ota", "ota" + chatId, "uz");
    }

    private static TelegramModels.Update start(long chatId, long id) {
        return new TelegramModels.Update(id, new TelegramModels.Message(id, user(chatId),
                new TelegramModels.Chat(chatId, "private"), "/start", null));
    }

    private static TelegramModels.Update tap(long chatId, long id, String data) {
        TelegramModels.Message card = new TelegramModels.Message(5000L + id, user(chatId),
                new TelegramModels.Chat(chatId, "private"), null, null);
        return new TelegramModels.Update(id, null, new TelegramModels.CallbackQuery("cb" + id, user(chatId), card, data));
    }

    /** One measured round: every parent sends /start, then opens schedule, grades and attendance. */
    private record Round(String name, int updates, double avg, double p50, double p95, double p99, double max,
                         double statementsPerUpdate, long statements, long peakHeapMb, long cpuMillis, long wallMillis,
                         long usageRows) {
    }

    private Round round(String name, long firstUpdateId) throws Exception {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        Runtime rt = Runtime.getRuntime();
        var os = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        long cpuBefore = os.getProcessCpuTime();
        AtomicLong peakHeap = new AtomicLong(rt.totalMemory() - rt.freeMemory());

        // 4 updates per parent, interleaved like real traffic: everybody's /start, then the taps
        List<TelegramModels.Update> updates = new ArrayList<>();
        long id = firstUpdateId;
        for (int i = 0; i < USERS; i++) updates.add(start(CHAT_BASE + i, id++));
        for (String cb : CALLBACKS) for (int i = 0; i < USERS; i++) updates.add(tap(CHAT_BASE + i, id++, cb));

        long[] latencyMicros = new long[updates.size()];
        List<CompletableFuture<Void>> done = new ArrayList<>();
        long t0 = System.nanoTime();
        long intervalNanos = 1_000_000_000L / RATE_PER_SECOND;
        for (int i = 0; i < updates.size(); i++) {
            long wait = t0 + i * intervalNanos - System.nanoTime();
            if (wait > 0) java.util.concurrent.locks.LockSupport.parkNanos(wait);
            final int idx = i;
            long submitted = System.nanoTime();
            done.add(dispatcher.dispatch(updates.get(i)).whenComplete((r, e) ->
                    latencyMicros[idx] = (System.nanoTime() - submitted) / 1000));
            if (i % 500 == 0) peakHeap.accumulateAndGet(rt.totalMemory() - rt.freeMemory(), Math::max);
        }
        CompletableFuture.allOf(done.toArray(new CompletableFuture[0])).get(5, TimeUnit.MINUTES);
        long wallMillis = (System.nanoTime() - t0) / 1_000_000;
        peakHeap.accumulateAndGet(rt.totalMemory() - rt.freeMemory(), Math::max);
        long[] sorted = latencyMicros.clone();
        Arrays.sort(sorted);
        // usage statistics are written in the background in JDBC batches (not on the update's path)
        long statements = stats.getPrepareStatementCount();
        long rowsBefore = usageRows();
        usage.flush();
        long usageRows = usageRows() - rowsBefore;
        return new Round(name, updates.size(), Arrays.stream(sorted).average().orElse(0) / 1000.0,
                sorted[(int) (sorted.length * 0.50)] / 1000.0, sorted[(int) (sorted.length * 0.95)] / 1000.0,
                sorted[(int) (sorted.length * 0.99)] / 1000.0, sorted[sorted.length - 1] / 1000.0,
                statements / (double) updates.size(), statements,
                peakHeap.get() / 1_048_576, (os.getProcessCpuTime() - cpuBefore) / 1_000_000, wallMillis, usageRows);
    }

    private long usageRows() {
        Long n = jdbc.queryForObject("select count(*) from bot_usage_event where chat_id between ? and ?", Long.class,
                CHAT_BASE, CHAT_BASE + USERS);
        return n == null ? 0 : n;
    }

    @Test
    @Order(1)
    void threeThousandParents_p95Under300ms() throws Exception {
        // warm-up: JIT and the connection pool (other chats, not measured)
        for (int i = 0; i < 200; i++) router.handle(start(CHAT_BASE + USERS - 1 - i, i));
        System.gc();
        long heapBefore = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1_048_576;

        // round 1: first contact (sessions created, children/timetables not cached yet)
        Round cold = round("1-raund — birinchi murojaat (sessiya yaratiladi, kesh bo'sh)", 1_000_000);
        // round 2: the same parents come back (the usual day-to-day traffic)
        Round warm = round("2-raund — qaytgan ota-onalar (odatiy kunlik trafik)", 2_000_000);
        int cores = Runtime.getRuntime().availableProcessors();

        StringBuilder report = new StringBuilder(String.format(Locale.ROOT,
                "Bot yuklama testi (10.3) — %s%nFoydalanuvchilar: %d, har raundda %d update (har biri: /start, jadval, " +
                        "baholar, davomat), tezlik: %d/s, Telegram: mock (tarmoq vaqti hisobga olinmaydi)%n" +
                        "Xotira (heap) boshida: %d MB, yadrolar: %d%n",
                LocalDateTime.now().withNano(0), USERS, USERS * 4, RATE_PER_SECOND, heapBefore, cores));
        for (Round r : List.of(cold, warm)) {
            report.append(String.format(Locale.ROOT,
                    "%n%s%n  Javob vaqti: o'rtacha %.1f ms, p50 %.1f ms, p95 %.1f ms, p99 %.1f ms, max %.1f ms%n" +
                            "  DB so'rovlari (update yo'lida): %.2f ta / update (jami %d)%n" +
                            "  Fon: statistika %d ta yozuv, ~%d ta JDBC batch'da (har 5 s)%n" +
                            "  Xotira (heap) eng ko'p: %d MB%n" +
                            "  CPU: %d ms jarayon vaqti %d ms ichida → o'rtacha %.1f%% (%d yadrodan)%n",
                    r.name(), r.avg(), r.p50(), r.p95(), r.p99(), r.max(), r.statementsPerUpdate(), r.statements(),
                    r.usageRows(), (r.usageRows() + 499) / 500,
                    r.peakHeapMb(), r.cpuMillis(), r.wallMillis(),
                    100.0 * r.cpuMillis() / Math.max(1, r.wallMillis()) / cores, cores));
        }
        System.out.println(report);
        Files.writeString(Path.of("build", "load-test-result.txt"), report.toString());

        assertTrue(cold.p95() < 300 && warm.p95() < 300, "p95: " + cold.p95() + " / " + warm.p95());
        assertTrue(warm.statementsPerUpdate() <= 4.0, "warm statements/update " + warm.statementsPerUpdate());
    }

}
