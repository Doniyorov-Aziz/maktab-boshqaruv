package uz.azizbek.maktabboshqaruv;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import uz.azizbek.maktabboshqaruv.bot.BotRouter;
import uz.azizbek.maktabboshqaruv.config.LibraryDemoSeeder;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.entity.LibraryEnums.AgeGroup;
import uz.azizbek.maktabboshqaruv.entity.LibraryEnums.DownloadSource;
import uz.azizbek.maktabboshqaruv.entity.LibraryEnums.LegalBasis;
import uz.azizbek.maktabboshqaruv.repository.*;
import uz.azizbek.maktabboshqaruv.service.NotificationSender;
import uz.azizbek.maktabboshqaruv.service.appeal.FileStorage;
import uz.azizbek.maktabboshqaruv.service.library.LibraryCatalog;
import uz.azizbek.maktabboshqaruv.service.library.LibraryFiles;
import uz.azizbek.maktabboshqaruv.service.library.LibraryText;
import uz.azizbek.maktabboshqaruv.service.library.LibraryWeekly;
import uz.azizbek.maktabboshqaruv.telegram.MockTelegramClient;
import uz.azizbek.maktabboshqaruv.telegram.TelegramClient;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels;

import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The whole "📚 Kitob mutolaasi" flow through the real router with the Telegram API mocked:
 * menu → shelf → book → PDF (a BookDownload row, the document in the outbox), a search, a
 * search that finds nothing, the recommendations with their blocks, leaving the search mode
 * with another menu button, and Friday's weekly message.
 *
 * With the environment variable LIBRARY_DEMO_OUT set, everything the bot sent is written
 * there as a Markdown transcript (docs/library-bot-demo.md).
 */
@SpringBootTest(properties = {
        "telegram.mock=true",
        "telegram.bot-username=maktab_test_bot",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000",
        "telegram.chat-rate-per-second=1000"
})
@ActiveProfiles("local")
class LibraryBotFlowIntegrationTest {

    private static final AtomicLong UPDATE_ID = new AtomicLong(1);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String TAG = "D" + ThreadLocalRandom.current().nextInt(1000, 9999);

    @Autowired private BotRouter router;
    @Autowired private TelegramClient client;
    @Autowired private StudentRepository students;
    @Autowired private ParentTelegramLinkRepository links;
    @Autowired private ParentSessionRepository sessions;
    @Autowired private BookRepository books;
    @Autowired private BookCategoryRepository categories;
    @Autowired private BookDownloadRepository downloads;
    @Autowired private NotificationLogRepository notificationLog;
    @Autowired private LibraryFiles files;
    @Autowired private FileStorage storage;
    @Autowired private LibraryCatalog catalog;
    @Autowired private LibraryWeekly weekly;
    @Autowired private NotificationSender sender;
    @Autowired private Clock clock;

    private MockTelegramClient mock;
    private long chatId;
    private Student student;
    private Long page;
    private BookCategory shelf;
    private final List<Book> created = new ArrayList<>();
    private final StringBuilder transcript = new StringBuilder();

    @BeforeEach
    void setUp() throws Exception {
        mock = (MockTelegramClient) client;
        chatId = 7_100_000_000L + ThreadLocalRandom.current().nextInt(1_000_000);
        student = students.findAll().stream()
                .filter(s -> s.getTelegramLinkCode() != null && s.getSchoolClass().getAcademicYear().getSchool().getId() == 1L)
                .findFirst().orElseThrow();
        School school = student.getSchoolClass().getAcademicYear().getSchool();

        shelf = new BookCategory();
        shelf.setSchool(school);
        shelf.setEmoji("🕵️");
        shelf.setNameUz("Detektiv " + TAG);
        shelf.setNameRu("Детектив " + TAG);
        shelf.setPosition(0);
        shelf.setActive(true);
        shelf = categories.save(shelf);

        String[][] data = {
                {"Namuna detektiv 1", "Namuna muallif A", "KATTALAR", "1"},
                {"Namuna detektiv 2", "Namuna muallif B", "OSMIRLAR", "0"},
                {"Namuna detektiv 3", "Namuna muallif A", "KATTALAR", "0"},
                {"Namuna ertaklar", "Namuna muallif C", "BOLALAR", "1"},
                {"O‘tkan kunlar (namuna)", "Namuna muallif D", "KATTALAR", "0"},
                {"Namuna detektiv 4", "Namuna muallif B", "OSMIRLAR", "0"},
                {"Namuna detektiv 5", "Namuna muallif C", "KATTALAR", "0"}};
        int n = 0;
        for (String[] d : data) {
            created.add(book(school, d[0] + " " + TAG, d[1], AgeGroup.valueOf(d[2]), "1".equals(d[3]), n++));
        }
        catalog.evict(school.getId());
    }

    @AfterEach
    void cleanUp() throws Exception {
        List<Long> ids = created.stream().map(Book::getId).toList();
        downloads.deleteAll(downloads.findAll().stream().filter(d -> ids.contains(d.getBook().getId())).toList());
        notificationLog.deleteAll(notificationLog.findAll().stream().filter(x -> x.getChatId() == chatId).toList());
        for (Book b : created) {
            storage.delete(b.getPdfPath());
            storage.delete(b.getCoverPath());
        }
        books.deleteAll(created);
        categories.delete(shelf);
        links.findAll().stream().filter(l -> l.getChatId() == chatId).forEach(links::delete);
        sessions.findByChatId(chatId).ifPresent(sessions::delete);
        catalog.evict(1L);
        String out = System.getenv("LIBRARY_DEMO_OUT");
        if (out != null && !out.isBlank()) {
            // the child's link code would let anyone follow that child — never in a document
            String text = transcript.toString().replaceAll("/start [A-Za-z0-9_-]+","/start ••••••••");
            Files.writeString(Path.of(out), header() + text, StandardCharsets.UTF_8);
        }
    }

    private Book book(School school, String title, String author, AgeGroup age, boolean featured, int n) throws Exception {
        byte[] pdf = LibraryDemoSeeder.samplePdf(title, author, new Color(0x4F46E5 + n * 0x101010), 2);
        LibraryFiles.PdfInfo info = files.checkPdf(pdf);
        Book b = new Book();
        b.setSchool(school);
        b.setTitle(title);
        b.setAuthor(author);
        b.setCategory(age == AgeGroup.BOLALAR ? null : shelf);
        b.setDescription("Generatsiya qilingan namunaviy PDF — haqiqiy kitob emas.");
        b.setLanguage("uz");
        b.setAgeGroup(age);
        b.setPages(info.pages());
        b.setYear(2024);
        b.setLegalBasis(LegalBasis.MAKTAB_MATERIALI);
        b.setLegalNote("namuna");
        b.setActive(true);
        b.setFeatured(featured);
        b.setDraft(false);
        b.setSearchKey(LibraryText.key(title, author));
        b.setDownloadCount((long) (10 - n));
        b.setLikes(3 - n % 3);
        b.setDislikes(n % 2);
        b.setCreatedBy("test");
        b.setCreatedAt(LocalDateTime.now(clock).minusDays(n));
        b.setPdfPath(files.savePdf(school.getId(), pdf));
        b.setPdfSize(info.size());
        b.setPdfSha256(info.sha256());
        // only the first book has a cover — its card is a photo with a caption
        if (n == 0) b.setCoverPath(files.saveCover(school.getId(), "jpg", files.coverFromPdf(pdf)));
        return books.save(b);
    }

    // ------------------------------------------------------------------ driving the bot

    private List<MockTelegramClient.SentMessage> send(String step, String text) {
        int before = mock.size();
        router.handle(new TelegramModels.Update(UPDATE_ID.getAndIncrement(), new TelegramModels.Message(1L,
                new TelegramModels.User(chatId, false, "Malika", "malika_ota_ona"),
                new TelegramModels.Chat(chatId, "private"), text, null)));
        return record(step, "✍️ Ota-ona yozadi: «" + text + "»", mock.since(before, chatId));
    }

    private List<MockTelegramClient.SentMessage> tap(String step, String button, String data) {
        int before = mock.size();
        TelegramModels.User from = new TelegramModels.User(chatId, false, "Malika", "malika_ota_ona");
        router.handle(new TelegramModels.Update(UPDATE_ID.getAndIncrement(), null, new TelegramModels.CallbackQuery("cb",
                from, new TelegramModels.Message(page, from, new TelegramModels.Chat(chatId, "private"), null, null), data)));
        return record(step, "👆 Ota-ona bosadi: [" + button + "]  <sub>callback_data=" + data + "</sub>", mock.since(before, chatId));
    }

    private List<MockTelegramClient.SentMessage> outbox(String step) {
        int before = mock.size();
        for (int i = 0; i < 3; i++) {
            sender.sendDue();
            try {
                Thread.sleep(1100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return record(step, "📤 Outbox yuboradi (NotificationSender)", mock.since(before, chatId));
    }

    private List<MockTelegramClient.SentMessage> record(String step, String action, List<MockTelegramClient.SentMessage> ops) {
        transcript.append("\n## ").append(step).append("\n\n").append(action).append("\n");
        for (MockTelegramClient.SentMessage op : ops) {
            if (op.keyboard() != null && op.keyboard().contains("inline_keyboard")) page = op.messageId();
            if ("delete".equals(op.op())) continue;
            String kind = switch (op.op()) {
                case "edit" -> "✏️ editMessageText";
                case "send" -> "💬 sendMessage";
                case "photo", "media" -> "🖼 " + op.op() + " (muqova / banner, caption bilan)";
                case "document" -> "📎 sendDocument (PDF, file_id=" + op.photoId() + ")";
                default -> op.op();
            };
            transcript.append("\n**🤖 Bot — ").append(kind).append("**\n\n");
            for (String line : html(op.text()).split("\n")) transcript.append("> ").append(line).append("\n");
            String buttons = buttons(op.keyboard());
            if (!buttons.isEmpty()) transcript.append("\n").append(buttons);
        }
        return ops;
    }

    private static String html(String s) {
        if (s == null) return "";
        return s.replaceAll("</?b>", "**").replaceAll("</?i>", "_").replaceAll("<[^>]+>", "")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&amp;", "&");
    }

    private static String buttons(String json) {
        if (json == null || !json.contains("inline_keyboard")) return "";
        StringBuilder sb = new StringBuilder();
        for (JsonNode row : JSON.readTree(json).get("inline_keyboard")) {
            sb.append("`");
            List<String> cells = new ArrayList<>();
            for (JsonNode b : row) cells.add("[ " + b.get("text").asString() + " ]");
            sb.append(String.join(" ", cells)).append("`  \n");
        }
        return sb.toString();
    }

    private static String header() {
        return "# 📚 Kitob mutolaasi — bot simulyatsiyasi (Telegram API mock)\n\n"
                + "Avtomatik yozilgan: `LibraryBotFlowIntegrationTest` (`LIBRARY_DEMO_OUT=docs/library-bot-demo.md`).\n"
                + "Bot haqiqiy router orqali ishladi, Telegram API o'rnida `MockTelegramClient` — har bir chiqish shu yerda.\n"
                + "Kitoblar test uchun generatsiya qilingan namunaviy PDF lar (haqiqiy kitob emas). `**qalin**`, `_kursiv_` —\n"
                + "Telegram HTML formatlash; `[ tugma ]` — inline tugmalar qatorlari; `<sub>` — callback_data (faqat ID lar).\n";
    }

    private static String last(List<MockTelegramClient.SentMessage> ops) {
        for (int i = ops.size() - 1; i >= 0; i--) if (ops.get(i).text() != null) return ops.get(i).text();
        return "";
    }

    // ------------------------------------------------------------------ the flow

    @Test
    void menu_shelf_book_download_search_recommendations_weekly() {
        send("0. Ota-ona botga ulanadi", "/start " + student.getTelegramLinkCode());

        // 1. menu button → the library page
        String main = last(send("1. Menyudagi «📚 Kitob mutolaasi» tugmasi", "📚 Kitob mutolaasi"));
        assertTrue(main.contains("Maktab kutubxonasi"), main);
        assertTrue(main.contains("Kitob nomini yoki muallifni yozing"), main);

        // 2. a shelf
        String list = last(tap("2. Bo'lim", "🕵️ Detektiv " + TAG, "lib:v:c:c:" + shelf.getId()));
        assertTrue(list.contains("Namuna detektiv 1"), list);
        assertTrue(list.contains("bet"), list);

        // 3. the book's card (it has a cover → photo with a caption)
        Book first = created.get(0);
        List<MockTelegramClient.SentMessage> card = tap("3. Kitob kartochkasi", "📕 " + first.getTitle(),
                "lib:v:b:b:" + first.getId() + ":k:c" + shelf.getId() + ".0");
        assertTrue(card.stream().anyMatch(o -> "photo".equals(o.op())), "card with a cover is a photo: " + card);
        assertTrue(last(card).contains("marta yuklangan"), last(card));

        // 4. download → BookDownload + the PDF from the outbox
        long before = downloads.findAll().stream().filter(d -> d.getChatId() == chatId).count();
        tap("4. «📥 PDF yuklab olish» (toast: 📥 Kitob yuborilmoqda…)", "📥 PDF yuklab olish",
                "lib:a:dl:b:" + first.getId() + ":k:c" + shelf.getId() + ".0");
        List<BookDownload> mine = downloads.findAll().stream().filter(d -> d.getChatId() == chatId).toList();
        assertEquals(before + 1, mine.size());
        assertEquals(DownloadSource.CATEGORY, mine.get(mine.size() - 1).getSource());
        List<MockTelegramClient.SentMessage> doc = outbox("5. PDF ota-onaga yuboriladi");
        assertTrue(doc.stream().anyMatch(o -> "document".equals(o.op()) && o.text().contains("Yoqimli mutolaa")), doc.toString());

        // 6. search by typing (library mode is open), Cyrillic and another apostrophe
        String found = last(send("6. Qidiruv (kirillcha, boshqa apostrof)", "ўткан кунлар"));
        assertTrue(found.contains("O‘tkan kunlar (namuna)"), found);
        String none = last(send("7. Topilmagan qidiruv", "mavjud bo'lmagan kitob xyz"));
        assertTrue(none.contains("Topilmadi") || none.contains("topilmadi"), none);

        // 8. recommendations: personal / popular / school / with the child
        String recs = last(tap("8. «⭐ Tavsiyalar»", "⭐ Tavsiyalar", "lib:v:r"));
        assertTrue(recs.contains("Siz uchun"), recs);
        assertTrue(recs.contains("Bu hafta mashhur"), recs);
        assertTrue(recs.contains("Maktab tavsiya qiladi"), recs);
        assertTrue(recs.contains("Farzandingiz bilan o'qing"), recs);

        // 9. another menu button closes the search mode — text after it is not a search
        send("9. Boshqa menyu tugmasi qidiruv rejimini yopadi", "📅 Dars jadvali");
        String after = last(send("10. Endi oddiy matn — qidiruv emas", "detektiv"));
        assertFalse(after.contains("kitob topildi"), after);

        // 11. Friday's weekly picks (forced: any day/time), delivered by the outbox
        int queued = weekly.run(LocalDateTime.now(clock), true, chatId);
        assertTrue(queued >= 1, "weekly message queued");
        List<MockTelegramClient.SentMessage> week = outbox("11. Juma 19:00 — haftalik tavsiya xabari");
        assertTrue(week.stream().anyMatch(o -> o.text() != null && o.text().contains("Dam olish kunlari uchun 3 ta kitob")), week.toString());
        assertTrue(notificationLog.findAll().stream().anyMatch(x -> x.getChatId() == chatId
                && x.getType() == NotificationType.LIBRARY_WEEKLY && x.getStatus() == NotificationStatus.SENT));
    }
}
