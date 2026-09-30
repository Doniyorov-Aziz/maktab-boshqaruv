package uz.azizbek.maktabboshqaruv;

import uz.azizbek.maktabboshqaruv.bot.BotRouter;
import uz.azizbek.maktabboshqaruv.entity.LessonSlot;
import uz.azizbek.maktabboshqaruv.entity.ParentSession;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.repository.LessonSlotRepository;
import uz.azizbek.maktabboshqaruv.repository.ParentSessionRepository;
import uz.azizbek.maktabboshqaruv.repository.ParentTelegramLinkRepository;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import uz.azizbek.maktabboshqaruv.telegram.MockTelegramClient;
import uz.azizbek.maktabboshqaruv.telegram.TelegramClient;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Renders every bot page through the real router against the seeded
 * database (telegram.mock=true) and checks what a parent would see: text is
 * present, placeholders are filled, no raw i18n key leaks, the breadcrumb is
 * there, images are real PNGs — and a foreign student id is refused.
 */
@SpringBootTest(properties = {
        "telegram.mock=true",
        "telegram.bot-username=maktab_test_bot",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000",
        "telegram.chat-rate-per-second=1000"
})
@ActiveProfiles("local")
class BotScreensSeedIntegrationTest {

    private static final Pattern UNFILLED = Pattern.compile("\\{[a-z_]+}");
    private static final Pattern RAW_KEY = Pattern.compile("\\b(common|att|gr|rep|sched|home|ann|ev|tch|msg|abs|sch|set|ch|beh|link|notif)\\.[a-z_.]+\\b");
    private static final AtomicLong UPDATE_ID = new AtomicLong(1);

    @Autowired
    private BotRouter router;
    @Autowired
    private TelegramClient client;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private LessonSlotRepository lessonSlotRepository;
    @Autowired
    private ParentTelegramLinkRepository linkRepository;
    @Autowired
    private ParentSessionRepository sessionRepository;

    private MockTelegramClient mock;
    private long chatId;
    private Student student;
    private Student stranger;
    private Long page;

    @BeforeEach
    void setUp() {
        mock = (MockTelegramClient) client;
        chatId = 8_000_000_000L + ThreadLocalRandom.current().nextInt(1_000_000);
        LessonSlot slot = lessonSlotRepository.findAll().stream()
                .filter(l -> !studentRepository.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(l.getSchoolClass().getId()).isEmpty())
                .findFirst().orElseThrow();
        List<Student> classmates = studentRepository.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(slot.getSchoolClass().getId());
        student = classmates.get(0);
        stranger = studentRepository.findAll().stream()
                .filter(s -> !s.getSchoolClass().getId().equals(student.getSchoolClass().getId()))
                .findFirst().orElseThrow();
    }

    @AfterEach
    void cleanUp() {
        linkRepository.findByChatIdAndActiveTrue(chatId).forEach(linkRepository::delete);
        sessionRepository.findByChatId(chatId).ifPresent(sessionRepository::delete);
    }

    private List<MockTelegramClient.SentMessage> send(String text) {
        int before = mock.size();
        router.handle(new TelegramModels.Update(UPDATE_ID.getAndIncrement(), new TelegramModels.Message(1L,
                new TelegramModels.User(chatId, false, "Test", "test_parent"),
                new TelegramModels.Chat(chatId, "private"), text, null)));
        return track(mock.since(before, chatId));
    }

    private List<MockTelegramClient.SentMessage> tap(String data) {
        int before = mock.size();
        TelegramModels.User from = new TelegramModels.User(chatId, false, "Test", "test_parent");
        router.handle(new TelegramModels.Update(UPDATE_ID.getAndIncrement(), null, new TelegramModels.CallbackQuery("cb",
                from, new TelegramModels.Message(page, from, new TelegramModels.Chat(chatId, "private"), null, null), data)));
        return track(mock.since(before, chatId));
    }

    private List<MockTelegramClient.SentMessage> track(List<MockTelegramClient.SentMessage> ops) {
        for (MockTelegramClient.SentMessage op : ops) {
            if (op.keyboard() != null && op.keyboard().contains("inline_keyboard")) page = op.messageId();
        }
        return ops;
    }

    private void assertPage(String what, List<MockTelegramClient.SentMessage> ops) {
        assertFalse(ops.isEmpty(), what + ": bot javob bermadi");
        MockTelegramClient.SentMessage last = ops.get(ops.size() - 1);
        String text = last.text();
        assertNotNull(text, what);
        assertFalse(text.isBlank(), what);
        assertFalse(UNFILLED.matcher(text).find(), what + ": to'ldirilmagan joy: " + text);
        assertFalse(RAW_KEY.matcher(text).find(), what + ": til kaliti ochiq qoldi: " + text);
        assertNotNull(last.keyboard(), what + ": tugmalar yo'q");
    }

    @Test
    void everyPageRendersWithSeedData() {
        List<MockTelegramClient.SentMessage> onboarding = send("/start " + student.getTelegramLinkCode());
        assertTrue(onboarding.get(0).text().contains("Tabriklaymiz"), onboarding.get(0).text());
        assertPage("bosh menyu", onboarding);

        String[][] pages = {
                {"home", "Bosh menyu"}, {"sch", "Jadval bugun"}, {"sch:t:tomorrow", "Jadval ertaga"}, {"sch:t:week", "Jadval hafta"},
                {"att", "Davomat xulosa"}, {"att:k:m:v:cal", "Davomat kalendar"}, {"att:k:m:v:det", "Davomat batafsil"},
                {"att:k:m:v:sub", "Davomat fanlar"}, {"att:k:q", "Davomat chorak"}, {"att:k:y", "Davomat o'quv yili"},
                {"gr", "Baholar so'nggi"}, {"gr:v:subj", "Baholar fanlar"}, {"gr:v:qtr", "Chorak baholari"},
                {"rep", "Hisobot hafta"}, {"rep:t:month", "Hisobot oy"}, {"beh", "Xulq"}, {"ann", "E'lonlar"},
                {"ev", "Tadbirlar"}, {"tch", "O'qituvchilar"}, {"msg", "Maktabga yozish"}, {"abs", "Sababli ariza"},
                {"info", "Maktab haqida"}, {"set", "Sozlamalar"}, {"set:v:times", "Vaqtlar"}, {"set:v:lang", "Til"},
                {"ch", "Farzandlarim"}
        };
        for (String[] p : pages) {
            List<MockTelegramClient.SentMessage> ops = tap(p[0]);
            assertPage(p[1], ops);
            if (!"home".equals(p[0])) {
                assertTrue(ops.get(ops.size() - 1).text().contains("🏠 ›"), p[1] + ": sarlavhada yo'l yo'q");
            }
        }

        // Subject detail: open the first subject from the list.
        List<MockTelegramClient.SentMessage> subjects = tap("gr:v:subj");
        String keyboard = subjects.get(subjects.size() - 1).keyboard();
        java.util.regex.Matcher m = Pattern.compile("gr:v:det:id:(\\d+)").matcher(keyboard);
        if (m.find()) assertPage("Fan tafsiloti", tap("gr:v:det:id:" + m.group(1)));

        // Picture pages are real PNG files.
        for (String data : new String[]{"att:k:m:v:img", "gr:v:chart", "rep:t:week:v:img"}) {
            List<MockTelegramClient.SentMessage> ops = tap(data);
            MockTelegramClient.SentMessage photo = ops.stream().filter(o -> "photo".equals(o.op())).findFirst().orElseThrow(() -> new AssertionError(data));
            byte[] png = mock.downloadFile(photo.photoId());
            assertTrue(png.length > 10_000 && (png[0] & 0xFF) == 0x89 && png[1] == 'P', data + ": PNG emas");
        }
    }

    @Test
    void pagesRenderInAllThreeLanguages() {
        send("/start " + student.getTelegramLinkCode());
        tap("set:a:lg:l:cy");
        List<MockTelegramClient.SentMessage> cy = tap("home");
        assertTrue(cy.get(cy.size() - 1).text().contains("Бош меню"), cy.get(cy.size() - 1).text());
        assertPage("cy davomat", tap("att"));
        tap("set:a:lg:l:ru");
        List<MockTelegramClient.SentMessage> ru = tap("att");
        assertTrue(ru.get(ru.size() - 1).text().contains("Всего уроков"), ru.get(ru.size() - 1).text());
        ParentSession s = sessionRepository.findByChatId(chatId).orElseThrow();
        assertEquals("ru", s.getLanguage());
    }

    @Test
    void foreignStudentIdInCallback_returnsNothing() {
        send("/start " + student.getTelegramLinkCode());
        for (String data : new String[]{"att:s:" + stranger.getId(), "gr:v:subj:s:" + stranger.getId(),
                "msg:a:to:to:CT:s:" + stranger.getId(), "ch:a:sel:id:" + stranger.getId()}) {
            List<MockTelegramClient.SentMessage> ops = tap(data);
            assertTrue(ops.isEmpty(), data + ": begona o'quvchi ma'lumoti qaytmasligi kerak, lekin: " + ops);
        }
        // The remembered child did not change either.
        assertEquals(student.getId(), sessionRepository.findByChatId(chatId).orElseThrow().getSelectedStudentId());
    }

    @Test
    void unlinkedChat_onlySeesTheWelcome() {
        List<MockTelegramClient.SentMessage> ops = tap("att:s:" + student.getId());
        assertEquals(1, ops.size());
        assertTrue(ops.get(0).text().contains("Assalomu alaykum"));
    }
}
