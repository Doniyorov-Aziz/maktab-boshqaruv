package uz.azizbek.maktabboshqaruv.service.library;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uz.azizbek.maktabboshqaruv.bot.BotI18n;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.NotificationLogRepository;
import uz.azizbek.maktabboshqaruv.repository.ParentSessionRepository;
import uz.azizbek.maktabboshqaruv.repository.ParentTelegramLinkRepository;
import uz.azizbek.maktabboshqaruv.service.BotSettingService;
import uz.azizbek.maktabboshqaruv.service.NotificationService;
import uz.azizbek.maktabboshqaruv.telegram.MessageFormatter;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineButton;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineKeyboardMarkup;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * "📚 Dam olish kunlari uchun 3 ta kitob" — on Fridays at 19:00 (Asia/Tashkent; app.library.weekly-*)
 * every parent gets three books picked for them (their own recommendations), one button per book.
 * Through the outbox like every digest: the school's switch (Bot sozlamalari) and the parent's
 * own switch (⚙️ Sozlamalar), quiet hours, unsubscribing and the per-chat dedup all apply, and
 * the sending is paced by the outbox — the bot never waits for it.
 */
@Service
public class LibraryWeekly {

    private static final Logger log = LoggerFactory.getLogger(LibraryWeekly.class);
    private static final Duration WINDOW = Duration.ofHours(3);

    private final LibraryCatalog catalog;
    private final LibrarySettings settings;
    private final ParentTelegramLinkRepository links;
    private final ParentSessionRepository sessions;
    private final NotificationLogRepository notificationLog;
    private final NotificationService notifications;
    private final BotSettingService botSettings;

    public LibraryWeekly(LibraryCatalog catalog, LibrarySettings settings, ParentTelegramLinkRepository links,
                         ParentSessionRepository sessions, NotificationLogRepository notificationLog,
                         NotificationService notifications, BotSettingService botSettings) {
        this.catalog = catalog;
        this.settings = settings;
        this.links = links;
        this.sessions = sessions;
        this.notificationLog = notificationLog;
        this.notifications = notifications;
        this.botSettings = botSettings;
    }

    /** @param force ignore the day and time (mock demo, tests); everything else still applies. */
    public int run(LocalDateTime now, boolean force) {
        LocalDate day = now.toLocalDate();
        LocalDateTime at = day.atTime(settings.weeklyTime());
        if (!force && (day.getDayOfWeek() != settings.weeklyDay() || now.isBefore(at) || !now.isBefore(at.plus(WINDOW)))) {
            return 0;
        }
        Map<Long, ParentSession> byChat = new HashMap<>();
        for (ParentSession s : sessions.findAll()) byChat.put(s.getChatId(), s);
        Map<Long, List<ParentTelegramLink>> bySchool = links.findAllActive().stream()
                .collect(Collectors.groupingBy(l -> NotificationService.schoolOf(l.getStudent()).getId()));

        int queued = 0;
        for (List<ParentTelegramLink> schoolLinks : bySchool.values()) {
            School school = NotificationService.schoolOf(schoolLinks.get(0).getStudent());
            if (!botSettings.getOrDefault(school).isLibraryWeeklyOn() || catalog.totalBooks(school.getId()) == 0) continue;
            Map<Long, List<Student>> children = new LinkedHashMap<>();
            for (ParentTelegramLink l : schoolLinks) children.computeIfAbsent(l.getChatId(), k -> new ArrayList<>()).add(l.getStudent());

            for (Map.Entry<Long, List<Student>> e : children.entrySet()) {
                Long chatId = e.getKey();
                ParentSession session = byChat.get(chatId);
                if (session != null && !session.wants(NotificationType.LIBRARY_WEEKLY)) continue;
                if (notificationLog.existsByChatIdAndTypeAndReferenceIdAndRecordDate(chatId, NotificationType.LIBRARY_WEEKLY,
                        school.getId(), day)) continue;
                List<Book> picks = picks(catalog.recommendations(school.getId(), chatId, e.getValue()));
                if (picks.isEmpty()) continue;
                queued += notifications.enqueueDirect(school, e.getValue().get(0), chatId, NotificationType.LIBRARY_WEEKLY,
                        school.getId(), day, lang -> text(lang, picks) + MessageFormatter.footer(lang, school.getName()),
                        lang -> buttons(lang, picks));
            }
        }
        if (queued > 0) log.info("Haftalik kitob tavsiyasi: {} ta xabar navbatga qo'shildi", queued);
        return queued;
    }

    /** Three different books: the parent's own picks first, then the week's popular, the school's, the children's. */
    static List<Book> picks(LibraryCatalog.Recommendations r) {
        List<Book> all = new ArrayList<>();
        for (List<Book> block : List.of(r.forYou(), r.popularWeek(), r.featured(), r.withChild())) {
            for (Book b : block) {
                if (all.size() < 3 && all.stream().noneMatch(x -> x.getId().equals(b.getId()))) all.add(b);
            }
        }
        return all;
    }

    static String text(String lang, List<Book> picks) {
        BotI18n i18n = BotI18n.get();
        StringBuilder list = new StringBuilder();
        int n = 0;
        for (Book b : picks) {
            if (n > 0) list.append("\n");
            list.append(i18n.t(lang, "lib.weekly.line", "n", ++n, "title", MessageFormatter.escape(b.getTitle()),
                    "author", b.getAuthor() == null ? "" : " — " + MessageFormatter.escape(b.getAuthor())));
        }
        return i18n.t(lang, "lib.weekly", "list", list.toString());
    }

    static InlineKeyboardMarkup buttons(String lang, List<Book> picks) {
        InlineKeyboardMarkup.Builder kb = InlineKeyboardMarkup.builder();
        for (Book b : picks) {
            String title = b.getTitle().length() > 40 ? b.getTitle().substring(0, 39) + "…" : b.getTitle();
            kb.row(InlineButton.callback("📕 " + title, "lib:v:b:b:" + b.getId() + ":k:w:n:1"));
        }
        kb.row(InlineButton.callback(BotI18n.get().t(lang, "lib.btn.library"), "lib:n:1"));
        return kb.build();
    }
}
