package uz.azizbek.maktabboshqaruv.service.library;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.azizbek.maktabboshqaruv.bot.BotI18n;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.entity.LibraryEnums.AgeGroup;
import uz.azizbek.maktabboshqaruv.entity.LibraryEnums.DownloadSource;
import uz.azizbek.maktabboshqaruv.repository.*;
import uz.azizbek.maktabboshqaruv.service.NotificationService;
import uz.azizbek.maktabboshqaruv.telegram.MessageFormatter;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * What the bot's "📚 Kitob mutolaasi" reads and does. Every list is one select plus its count
 * (the shelf is fetched with the book); the shelves with their book counts and the week's most
 * read books are cached per school for 10 minutes and dropped when the school edits its library.
 *
 * Books are always looked up within the parent's school and only when parents may see them
 * (active, not a draft) — a book id from callback_data is never trusted on its own.
 */
@Service
public class LibraryCatalog {

    public static final int PAGE_SIZE = 5;
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    /** A shelf as the bot shows it, detached from the session (safe to cache). */
    public record Shelf(Long id, String nameUz, String nameRu, String emoji, long count) {
        public String label(String lang) {
            String name = "ru".equals(lang) && nameRu != null && !nameRu.isBlank() ? nameRu : nameUz;
            return emoji == null || emoji.isBlank() ? name : emoji + " " + name;
        }
    }

    /** The four blocks of "⭐ Tavsiyalar". */
    public record Recommendations(List<Book> forYou, boolean personal, List<Book> popularWeek, List<Book> featured,
                                  List<Book> withChild) {
    }

    public enum DownloadResult { QUEUED, ALREADY_QUEUED, LIMIT, NOT_FOUND }

    private final BookRepository books;
    private final BookCategoryRepository categories;
    private final BookDownloadRepository downloads;
    private final BookFavoriteRepository favorites;
    private final BookRatingRepository ratings;
    private final NotificationService notifications;
    private final LibrarySettings settings;
    private final LibraryDefaults defaults;
    private final Clock clock;

    private final Cache<Long, List<Shelf>> shelves = Caffeine.newBuilder().expireAfterWrite(CACHE_TTL).maximumSize(1000).build();
    private final Cache<Long, List<Long>> popular = Caffeine.newBuilder().expireAfterWrite(CACHE_TTL).maximumSize(1000).build();

    public LibraryCatalog(BookRepository books, BookCategoryRepository categories, BookDownloadRepository downloads,
                          BookFavoriteRepository favorites, BookRatingRepository ratings, NotificationService notifications,
                          LibrarySettings settings, LibraryDefaults defaults, Clock clock) {
        this.books = books;
        this.categories = categories;
        this.downloads = downloads;
        this.favorites = favorites;
        this.ratings = ratings;
        this.notifications = notifications;
        this.settings = settings;
        this.defaults = defaults;
        this.clock = clock;
    }

    public static Long schoolId(Student student) {
        return NotificationService.schoolOf(student).getId();
    }

    // ------------------------------------------------------------------ reading

    /** Active shelves with the number of visible books on each (cached). */
    public List<Shelf> shelves(Long schoolId) {
        return shelves.get(schoolId, id -> {
            defaults.ensureCategories(id);
            List<Shelf> list = new ArrayList<>();
            for (Object[] row : categories.activeWithCounts(id)) {
                BookCategory c = (BookCategory) row[0];
                list.add(new Shelf(c.getId(), c.getNameUz(), c.getNameRu(), c.getEmoji(), (Long) row[1]));
            }
            return List.copyOf(list);
        });
    }

    public Shelf shelf(Long schoolId, Long categoryId) {
        if (categoryId == null) return null;
        return shelves(schoolId).stream().filter(s -> s.id().equals(categoryId)).findFirst().orElse(null);
    }

    public long totalBooks(Long schoolId) {
        return books.countBySchoolIdAndActiveTrueAndDraftFalse(schoolId);
    }

    @Transactional(readOnly = true)
    public Page<Book> search(Long schoolId, String query, int page) {
        List<String> w = LibraryText.likePatterns(query);
        return books.search(schoolId, w.get(0), w.get(1), w.get(2), PageRequest.of(Math.max(0, page), PAGE_SIZE));
    }

    @Transactional(readOnly = true)
    public Page<Book> byCategory(Long schoolId, Long categoryId, int page) {
        return books.byCategory(schoolId, categoryId, PageRequest.of(Math.max(0, page), PAGE_SIZE));
    }

    @Transactional(readOnly = true)
    public Page<Book> newest(Long schoolId, int page) {
        return books.newest(schoolId, PageRequest.of(Math.max(0, page), PAGE_SIZE));
    }

    @Transactional(readOnly = true)
    public Page<Book> mostRead(Long schoolId, int page) {
        return books.mostRead(schoolId, PageRequest.of(Math.max(0, page), PAGE_SIZE));
    }

    @Transactional(readOnly = true)
    public Page<Book> favorites(Long schoolId, long chatId, int page) {
        return books.favorites(schoolId, chatId, PageRequest.of(Math.max(0, page), PAGE_SIZE));
    }

    /** "📥 Yuklab olganlarim": the latest first; a page of ids, then the books (two selects). */
    @Transactional(readOnly = true)
    public Page<Book> history(Long schoolId, long chatId, int page) {
        PageRequest request = PageRequest.of(Math.max(0, page), PAGE_SIZE);
        List<Long> ids = downloads.historyOf(chatId, schoolId, request).stream().map(r -> (Long) r[0]).toList();
        long total = ids.isEmpty() && page == 0 ? 0 : downloads.historyCount(chatId, schoolId);
        return new org.springframework.data.domain.PageImpl<>(inOrder(books.visibleByIds(schoolId, ids), ids), request, total);
    }

    /** A book parents of this school may see, with its shelf; null for a foreign, inactive or unknown id. */
    @Transactional(readOnly = true)
    public Book book(Long schoolId, Long bookId) {
        if (bookId == null) return null;
        return books.findWithCategory(bookId)
                .filter(b -> b.getSchool().getId().equals(schoolId) && b.visibleToParents())
                .orElse(null);
    }

    public boolean isFavorite(Long bookId, long chatId) {
        return favorites.findByBookIdAndChatId(bookId, chatId).isPresent();
    }

    /** +1, -1 or 0 (not rated). */
    public int myRating(Long bookId, long chatId) {
        return ratings.findByBookIdAndChatId(bookId, chatId).map(BookRating::getValue).orElse(0);
    }

    @Transactional(readOnly = true)
    public Recommendations recommendations(Long schoolId, long chatId, List<Student> children) {
        PageRequest three = PageRequest.of(0, 3);
        List<Book> featured = books.featured(schoolId, three);
        List<Book> popularWeek = popularWeek(schoolId, 3);

        List<Long> favouriteShelves = downloads.categoriesOf(chatId, schoolId, PageRequest.of(0, 2)).stream()
                .map(r -> (Long) r[0]).toList();
        List<Book> forYou;
        boolean personal = !favouriteShelves.isEmpty();
        if (personal) {
            forYou = new ArrayList<>(books.unreadInCategories(schoolId, favouriteShelves, chatId, three));
        } else {
            // a new reader: what the school recommends and what other parents read
            forYou = new ArrayList<>();
            addDistinct(forYou, featured, 3);
            addDistinct(forYou, popularWeek, 3);
        }
        if (forYou.size() < 3) addDistinct(forYou, books.mostRead(schoolId, PageRequest.of(0, 6)).getContent(), 3);

        Set<AgeGroup> groups = EnumSet.noneOf(AgeGroup.class);
        for (Student s : children) {
            Integer grade = s.getSchoolClass() == null ? null : s.getSchoolClass().getGradeNumber();
            if (grade != null) groups.add(grade <= 4 ? AgeGroup.BOLALAR : AgeGroup.OSMIRLAR);
        }
        List<Book> withChild = groups.isEmpty() ? List.of() : books.forAgeGroups(schoolId, groups, three);
        return new Recommendations(forYou, personal, popularWeek, featured, withChild);
    }

    /** The week's most downloaded books (ids cached 10 minutes), most first; falls back to all-time. */
    public List<Book> popularWeek(Long schoolId, int limit) {
        List<Long> ids = popular.get(schoolId, id -> downloads
                .popularSince(id, LocalDateTime.now(clock).minusDays(7), PageRequest.of(0, 10))
                .stream().map(r -> (Long) r[0]).toList());
        List<Long> top = ids.stream().limit(limit).toList();
        if (top.isEmpty()) return books.mostRead(schoolId, PageRequest.of(0, limit)).getContent();
        return inOrder(books.visibleByIds(schoolId, top), top);
    }

    // ------------------------------------------------------------------ actions

    /**
     * The parent asked for the PDF: checks the book, the daily limit and a double tap, records
     * the download and queues the file in the outbox (sent at once, by Telegram file_id when the
     * book was sent before, with the outbox's rate limit and 429 handling).
     */
    @Transactional
    public DownloadResult download(ParentSession parent, Student student, Long bookId, DownloadSource source) {
        School school = NotificationService.schoolOf(student);
        Book book = book(school.getId(), bookId);
        if (book == null) return DownloadResult.NOT_FOUND;
        long chatId = parent.getChatId();
        LocalDateTime now = LocalDateTime.now(clock);
        if (downloads.existsByChatIdAndBookIdAndDownloadedAtGreaterThanEqual(chatId, bookId, now.minusSeconds(15))) {
            return DownloadResult.ALREADY_QUEUED;
        }
        if (downloads.countByChatIdAndDownloadedAtGreaterThanEqual(chatId, now.toLocalDate().atStartOfDay())
                >= settings.dailyDownloadLimit()) {
            return DownloadResult.LIMIT;
        }
        BookDownload d = new BookDownload();
        d.setSchool(school);
        d.setBook(book);
        d.setParent(parent.getId() == null ? null : parent);
        d.setChatId(chatId);
        d.setDownloadedAt(now);
        d.setSource(source);
        downloads.save(d);
        books.incrementDownloads(book.getId());
        String title = book.getTitle();
        String author = book.getAuthor();
        // keyed by the download (not the book): the outbox's dedup key must allow the same book twice a day
        notifications.createRows(school, NotificationType.BOOK_DOWNLOAD, d.getId(), now.toLocalDate(),
                List.of(new NotificationService.Recipient(student, chatId)),
                lang -> caption(lang, title, author), null, BookMedia.ref(book.getId()));
        return DownloadResult.QUEUED;
    }

    static String caption(String lang, String title, String author) {
        return BotI18n.get().t(lang, "lib.caption", "title", MessageFormatter.escape(title),
                "author", author == null || author.isBlank() ? "" : "\n" + MessageFormatter.escape(author));
    }

    /** ❤️ on / off; returns the new state. */
    @Transactional
    public boolean toggleFavorite(ParentSession parent, Long schoolId, Long bookId) {
        Book book = book(schoolId, bookId);
        if (book == null) return false;
        Optional<BookFavorite> existing = favorites.findByBookIdAndChatId(bookId, parent.getChatId());
        if (existing.isPresent()) {
            favorites.delete(existing.get());
            return false;
        }
        BookFavorite f = new BookFavorite();
        f.setBook(book);
        f.setParent(parent.getId() == null ? null : parent);
        f.setChatId(parent.getChatId());
        f.setCreatedAt(LocalDateTime.now(clock));
        favorites.save(f);
        return true;
    }

    /** 👍 (+1) / 👎 (-1); the same vote again takes it back. One vote per parent and book. */
    @Transactional
    public int rate(ParentSession parent, Long schoolId, Long bookId, int value) {
        Book book = book(schoolId, bookId);
        if (book == null || (value != 1 && value != -1)) return 0;
        BookRating r = ratings.findByBookIdAndChatId(bookId, parent.getChatId()).orElse(null);
        int likes = 0;
        int dislikes = 0;
        int result;
        if (r == null) {
            r = new BookRating();
            r.setBook(book);
            r.setParent(parent.getId() == null ? null : parent);
            r.setChatId(parent.getChatId());
            r.setValue(value);
            r.setUpdatedAt(LocalDateTime.now(clock));
            ratings.save(r);
            if (value > 0) likes++;
            else dislikes++;
            result = value;
        } else if (r.getValue() == value) {
            ratings.delete(r);
            if (value > 0) likes--;
            else dislikes--;
            result = 0;
        } else {
            r.setValue(value);
            r.setUpdatedAt(LocalDateTime.now(clock));
            ratings.save(r);
            likes += value > 0 ? 1 : -1;
            dislikes += value > 0 ? -1 : 1;
            result = value;
        }
        books.addRatings(bookId, likes, dislikes);
        return result;
    }

    /** The school changed its library: shelves, counts and "popular" are read again. */
    public void evict(Long schoolId) {
        shelves.invalidate(schoolId);
        popular.invalidate(schoolId);
    }

    // ------------------------------------------------------------------ helpers

    private static List<Book> inOrder(List<Book> found, List<Long> ids) {
        Map<Long, Book> byId = new HashMap<>();
        for (Book b : found) byId.put(b.getId(), b);
        List<Book> ordered = new ArrayList<>();
        for (Long id : ids) if (byId.containsKey(id)) ordered.add(byId.get(id));
        return ordered;
    }

    private static void addDistinct(List<Book> target, List<Book> more, int limit) {
        for (Book b : more) {
            if (target.size() >= limit) return;
            if (target.stream().noneMatch(x -> x.getId().equals(b.getId()))) target.add(b);
        }
    }
}
