package uz.azizbek.maktabboshqaruv.bot.screens;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import uz.azizbek.maktabboshqaruv.bot.*;
import uz.azizbek.maktabboshqaruv.entity.Book;
import uz.azizbek.maktabboshqaruv.entity.LibraryEnums.DownloadSource;
import uz.azizbek.maktabboshqaruv.entity.ParentSession;
import uz.azizbek.maktabboshqaruv.service.library.LibraryCatalog;
import uz.azizbek.maktabboshqaruv.service.library.LibraryCatalog.Recommendations;
import uz.azizbek.maktabboshqaruv.service.library.LibraryCatalog.Shelf;
import uz.azizbek.maktabboshqaruv.service.library.LibraryFiles;
import uz.azizbek.maktabboshqaruv.service.library.LibrarySettings;
import uz.azizbek.maktabboshqaruv.service.library.LibraryText;
import uz.azizbek.maktabboshqaruv.service.parent.ParentAccessService;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineButton;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineKeyboardMarkup;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import static uz.azizbek.maktabboshqaruv.bot.BotContext.e;

/**
 * "📚 Kitob mutolaasi" — the school library in the bot. One page message, edited in place:
 * the library (search, recommendations, shelves, most read, new, saved, downloaded), a shelf
 * or a list five books per page, and a book's card (its cover as the card picture when the
 * school has one) with "📥 PDF yuklab olish", "❤️ Saqlash" and 👍/👎.
 *
 * While the parent is in the library, plain text is a search ({@code pendingAction = "LIB"}):
 * the mode ends after 10 idle minutes or when another menu button is pressed (BotRouter), so it
 * never mixes with writing to the school.
 *
 * callback_data carries ids only — {@code lib:v:b:b:<book>:k:<back>}; the book is looked up
 * again within the parent's school and must be active ({@link LibraryCatalog#book}).
 * {@code k} says where "⬅️ Orqaga" leads: m (library), s.P (search page), cID.P (shelf page),
 * t.P / n.P / f.P / h.P (most read / new / saved / downloaded), r (recommendations), w (weekly message).
 */
@Component
public class LibraryScreen implements Screen, InputHandler {

    public static final String PENDING = "LIB";
    private static final int DESCRIPTION_MAX = 600;

    private final LibraryCatalog catalog;
    private final LibraryFiles files;
    private final LibrarySettings settings;
    private final ParentAccessService access;
    private final Clock clock;

    public LibraryScreen(LibraryCatalog catalog, LibraryFiles files, LibrarySettings settings, ParentAccessService access,
                         Clock clock) {
        this.catalog = catalog;
        this.files = files;
        this.settings = settings;
        this.access = access;
        this.clock = clock;
    }

    @Override
    public String code() {
        return "lib";
    }

    @Override
    public List<String> pendingPrefixes() {
        return List.of(PENDING);
    }

    /** The search mode is over: no library page was opened for {@code idleMinutes}. */
    public static boolean isIdle(ParentSession session, long nowMillis, int idleMinutes) {
        Long at = PendingData.parse(session.getPendingData()).getLong("t");
        return at == null || nowMillis - at > idleMinutes * 60_000L;
    }

    // ------------------------------------------------------------------ routing

    @Override
    public BotView render(BotContext ctx, CallbackData cb) {
        Long schoolId = LibraryCatalog.schoolId(ctx.student());
        String action = cb.get("a");
        if (action != null) return act(ctx, schoolId, cb, action);
        return switch (cb.get("v", "m")) {
            case "q" -> searchPrompt(ctx);
            case "s" -> searchResults(ctx, schoolId, PendingData.parse(ctx.session().getPendingData()).get("q"), cb.getInt("p", 0));
            case "c" -> shelf(ctx, schoolId, cb.getLong("c"), cb.getInt("p", 0));
            case "t" -> list(ctx, ctx.t("lib.top.title"), catalog.mostRead(schoolId, cb.getInt("p", 0)), "t",
                    ctx.t("lib.top.empty"));
            case "n" -> list(ctx, ctx.t("lib.new.title"), catalog.newest(schoolId, cb.getInt("p", 0)), "n",
                    ctx.t("lib.empty_library"));
            case "f" -> {
                Page<Book> page = catalog.favorites(schoolId, ctx.chatId(), cb.getInt("p", 0));
                yield list(ctx, ctx.t("lib.saved.title", "count", page.getTotalElements()), page, "f",
                        ctx.t("lib.saved.empty"));
            }
            case "h" -> {
                Page<Book> page = catalog.history(schoolId, ctx.chatId(), cb.getInt("p", 0));
                yield list(ctx, ctx.t("lib.history.title", "count", page.getTotalElements()), page, "h",
                        ctx.t("lib.history.empty"));
            }
            case "r" -> recommendations(ctx, schoolId);
            case "b" -> card(ctx, schoolId, cb.getLong("b"), cb.get("k", "m"));
            default -> main(ctx, schoolId);
        };
    }

    /** Plain text while in the library = a search. */
    @Override
    public BotView handleInput(BotContext ctx, String text, String photoFileId) {
        Long schoolId = LibraryCatalog.schoolId(ctx.student());
        if (text == null || text.isBlank()) return main(ctx, schoolId);
        String query = text.length() > 100 ? text.substring(0, 100) : text;
        return searchResults(ctx, schoolId, query, 0);
    }

    // ------------------------------------------------------------------ pages

    BotView main(BotContext ctx, Long schoolId) {
        enterMode(ctx, null);
        long total = catalog.totalBooks(schoolId);
        StringBuilder sb = new StringBuilder(ctx.crumb(ctx.t("sec.library"))).append("\n\n")
                .append(ctx.t("lib.title", "count", total)).append("\n")
                .append(total == 0 ? ctx.t("lib.empty_library") : ctx.t("lib.intro"));
        InlineKeyboardMarkup.Builder kb = InlineKeyboardMarkup.builder()
                .row(ctx.btn(ctx.t("lib.btn.search"), CallbackData.of("lib").with("v", "q")),
                        ctx.btn(ctx.t("lib.btn.recs"), CallbackData.of("lib").with("v", "r")));
        List<InlineButton> shelves = new ArrayList<>();
        for (Shelf s : catalog.shelves(schoolId)) {
            shelves.add(ctx.btn(s.label(ctx.lang()) + " · " + s.count(), CallbackData.of("lib").with("v", "c").with("c", s.id())));
        }
        kb.grid(shelves, 2)
                .row(ctx.btn(ctx.t("lib.btn.top"), CallbackData.of("lib").with("v", "t")),
                        ctx.btn(ctx.t("lib.btn.new"), CallbackData.of("lib").with("v", "n")))
                .row(ctx.btn(ctx.t("lib.btn.saved"), CallbackData.of("lib").with("v", "f")),
                        ctx.btn(ctx.t("lib.btn.history"), CallbackData.of("lib").with("v", "h")))
                .row(ctx.navRow(null).toArray(new InlineButton[0]));
        return BotView.of(sb.toString(), kb.build()).section("library");
    }

    private BotView searchPrompt(BotContext ctx) {
        enterMode(ctx, null);
        String text = ctx.crumb(ctx.t("sec.library"), ctx.t("lib.btn.search")) + "\n\n" + ctx.t("lib.search.prompt");
        return BotView.of(text, InlineKeyboardMarkup.builder().row(libraryNav(ctx)).build()).section("library");
    }

    private BotView searchResults(BotContext ctx, Long schoolId, String query, int page) {
        if (query == null || LibraryText.normalize(query).isEmpty()) return searchPrompt(ctx);
        enterMode(ctx, query);
        Page<Book> result = catalog.search(schoolId, query, page);
        String crumb = ctx.crumb(ctx.t("sec.library"), ctx.t("lib.btn.search"));
        if (result.getTotalElements() == 0) {
            InlineKeyboardMarkup.Builder kb = InlineKeyboardMarkup.builder();
            List<InlineButton> similar = new ArrayList<>();
            for (Shelf s : similarShelves(schoolId, query)) {
                similar.add(ctx.btn(s.label(ctx.lang()) + " · " + s.count(), CallbackData.of("lib").with("v", "c").with("c", s.id())));
            }
            kb.grid(similar, 2).row(libraryNav(ctx));
            return BotView.of(crumb + "\n\n" + ctx.t("lib.search.none", "query", e(query)), kb.build()).section("library");
        }
        String header = ctx.t("lib.search.title", "query", e(query), "count", result.getTotalElements());
        return books(ctx, crumb, header, result, "s", null);
    }

    private BotView shelf(BotContext ctx, Long schoolId, Long categoryId, int page) {
        Shelf shelf = catalog.shelf(schoolId, categoryId);
        if (shelf == null) return main(ctx, schoolId);
        enterMode(ctx, null);
        Page<Book> result = catalog.byCategory(schoolId, categoryId, page);
        String crumb = ctx.crumb(ctx.t("sec.library"), shelf.label(ctx.lang()));
        String header = ctx.t("lib.category.title", "category", "<b>" + e(shelf.label(ctx.lang())) + "</b>",
                "count", result.getTotalElements());
        return books(ctx, crumb, header, result, "c" + categoryId, ctx.t("lib.category.empty"));
    }

    private BotView list(BotContext ctx, String title, Page<Book> page, String code, String empty) {
        enterMode(ctx, null);
        String crumb = ctx.crumb(ctx.t("sec.library"), stripTags(title));
        return books(ctx, crumb, title, page, code, empty);
    }

    /** A page of five books: numbered lines in the text, one button per book, ◀ 1/3 ▶ and the way back. */
    private BotView books(BotContext ctx, String crumb, String header, Page<Book> page, String code, String empty) {
        StringBuilder sb = new StringBuilder(crumb).append("\n\n").append(header);
        InlineKeyboardMarkup.Builder kb = InlineKeyboardMarkup.builder();
        if (page.isEmpty()) {
            if (empty != null) sb.append("\n\n").append(empty);
        } else {
            sb.append("\n");
            int n = page.getNumber() * LibraryCatalog.PAGE_SIZE;
            String back = code + "." + page.getNumber();
            for (Book b : page.getContent()) {
                n++;
                sb.append("\n").append(n).append(". 📕 <b>").append(e(b.getTitle())).append("</b>")
                        .append(b.getAuthor() == null ? "" : " — " + e(b.getAuthor()))
                        .append(b.getPages() == null ? "" : " · " + ctx.t("lib.pages_short", "pages", b.getPages()));
                kb.row(ctx.btn(shorten("📕 " + b.getTitle() + (b.getAuthor() == null ? "" : " — " + b.getAuthor())
                                + (b.getPages() == null ? "" : " · " + ctx.t("lib.pages_short", "pages", b.getPages())), 60),
                        CallbackData.of("lib").with("v", "b").with("b", b.getId()).with("k", back)));
            }
            sb.append("\n\n").append(ctx.t("lib.list.hint"));
        }
        CallbackData base = listData(code);
        kb.row(ctx.pagerRow(base, page.getNumber(), page.getTotalPages()).toArray(new InlineButton[0]));
        kb.row(libraryNav(ctx));
        return BotView.of(sb.toString(), kb.build()).section("library");
    }

    private BotView recommendations(BotContext ctx, Long schoolId) {
        enterMode(ctx, null);
        Recommendations r = catalog.recommendations(schoolId, ctx.chatId(), ctx.students());
        StringBuilder sb = new StringBuilder(ctx.crumb(ctx.t("sec.library"), ctx.t("lib.btn.recs"))).append("\n\n")
                .append(ctx.t("lib.recs.title"));
        List<Book> buttons = new ArrayList<>();
        block(ctx, sb, r.personal() ? ctx.t("lib.recs.for_you") : ctx.t("lib.recs.for_new"), r.forYou(), buttons);
        block(ctx, sb, ctx.t("lib.recs.popular"), r.popularWeek(), buttons);
        block(ctx, sb, ctx.t("lib.recs.featured"), r.featured(), buttons);
        if (!ctx.students().isEmpty()) block(ctx, sb, ctx.t("lib.recs.with_child"), r.withChild(), buttons);
        InlineKeyboardMarkup.Builder kb = InlineKeyboardMarkup.builder();
        List<InlineButton> row = new ArrayList<>();
        for (Book b : buttons) {
            row.add(ctx.btn(shorten("📕 " + b.getTitle(), 32), CallbackData.of("lib").with("v", "b").with("b", b.getId()).with("k", "r")));
        }
        kb.grid(row, 2).row(libraryNav(ctx));
        return BotView.of(sb.toString(), kb.build()).section("library");
    }

    private void block(BotContext ctx, StringBuilder sb, String title, List<Book> list, List<Book> buttons) {
        sb.append("\n\n").append(title);
        if (list.isEmpty()) {
            sb.append("\n").append(ctx.t("lib.recs.none"));
            return;
        }
        int n = 0;
        for (Book b : list) {
            sb.append("\n").append(ctx.t("lib.recs.line", "n", ++n, "title", e(b.getTitle()),
                    "author", b.getAuthor() == null ? "" : " — " + e(b.getAuthor())));
            if (buttons.stream().noneMatch(x -> x.getId().equals(b.getId())) && buttons.size() < 12) buttons.add(b);
        }
    }

    /** A book's card: its cover as the card picture (sent by file_id after the first time), or text. */
    BotView card(BotContext ctx, Long schoolId, Long bookId, String back) {
        Book b = catalog.book(schoolId, bookId);
        if (b == null) return backPage(ctx, schoolId, back).toast(ctx.t("lib.toast.not_found"));
        enterMode(ctx, null);
        StringBuilder sb = new StringBuilder(ctx.crumb(ctx.t("sec.library"))).append("\n\n")
                .append("📕 <b>").append(e(b.getTitle())).append("</b>");
        // the author's name is always shown (the law keeps it with the work even in the public domain)
        sb.append("\n").append(b.getAuthor() != null && !b.getAuthor().isBlank()
                ? ctx.t("lib.card.author", "author", e(b.getAuthor())) : ctx.t("lib.card.author_unknown"));
        sb.append("\n");
        if (b.getCategory() != null) {
            sb.append("\n").append(ctx.t("lib.card.category", "category", e(b.getCategory().label(ctx.lang()))));
        }
        sb.append("\n").append(ctx.t("lib.card.language", "language", ctx.t("lib.lang." + b.getLanguage())));
        if (b.getPages() != null) sb.append("\n").append(ctx.t("lib.card.pages", "pages", b.getPages()));
        sb.append("\n").append(ctx.t("lib.card.age", "age", ctx.t("lib.age." + b.getAgeGroup().name())));
        if (b.getYear() != null) sb.append("\n").append(ctx.t("lib.card.year", "year", b.getYear()));
        if (b.getDescription() != null && !b.getDescription().isBlank()) {
            sb.append("\n\n<i>").append(e(shorten(b.getDescription().strip(), DESCRIPTION_MAX))).append("</i>");
        }
        sb.append("\n\n").append(ctx.t("lib.card.stats", "count", b.getDownloadCount(), "likes", b.getLikes(),
                "dislikes", b.getDislikes()));

        boolean saved = catalog.isFavorite(b.getId(), ctx.chatId());
        int mine = catalog.myRating(b.getId(), ctx.chatId());
        CallbackData self = CallbackData.of("lib").with("b", b.getId()).with("k", back);
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .row(ctx.btn(ctx.t("lib.btn.download"), self.with("a", "dl")).styled("primary"),
                        ctx.btn(saved ? ctx.t("lib.btn.unsave") : ctx.t("lib.btn.save"), self.with("a", "fv")))
                .row(ctx.btn((mine > 0 ? "✅ " : "") + "👍 " + b.getLikes(), self.with("a", "up")),
                        ctx.btn((mine < 0 ? "✅ " : "") + "👎 " + b.getDislikes(), self.with("a", "dn")))
                .row(ctx.btn(ctx.t("common.back"), backData(back)), ctx.btn(ctx.t("lib.btn.library"), "lib"))
                .build();
        BotView view = BotView.of(sb.toString(), kb).section("library");
        if (b.getCoverPath() != null) {
            String path = b.getCoverPath();
            view.banner(BotBanners.Banner.cover(b.getId(), path, () -> files.read(path)));
        }
        return view;
    }

    // ------------------------------------------------------------------ actions

    private BotView act(BotContext ctx, Long schoolId, CallbackData cb, String action) {
        Long bookId = cb.getLong("b");
        String back = cb.get("k", "m");
        String toast = switch (action) {
            case "dl" -> switch (catalog.download(ctx.session(), ctx.student(), bookId, sourceOf(back))) {
                case QUEUED -> ctx.t("lib.toast.sending");
                case ALREADY_QUEUED -> ctx.t("lib.toast.already");
                case LIMIT -> ctx.t("lib.toast.limit", "limit", settings.dailyDownloadLimit());
                case NOT_FOUND -> ctx.t("lib.toast.not_found");
            };
            case "fv" -> catalog.toggleFavorite(ctx.session(), schoolId, bookId)
                    ? ctx.t("lib.toast.saved") : ctx.t("lib.toast.unsaved");
            case "up", "dn" -> {
                int result = catalog.rate(ctx.session(), schoolId, bookId, "up".equals(action) ? 1 : -1);
                yield result > 0 ? ctx.t("lib.toast.liked") : result < 0 ? ctx.t("lib.toast.disliked") : ctx.t("lib.toast.vote_removed");
            }
            default -> null;
        };
        return card(ctx, schoolId, bookId, back).toast(toast);
    }

    // ------------------------------------------------------------------ helpers

    /** Keeps the library's search mode open (and refreshes its 10-minute clock). */
    private void enterMode(BotContext ctx, String query) {
        ParentSession s = ctx.session();
        PendingData data = PENDING.equals(s.getPendingAction()) ? PendingData.parse(s.getPendingData()) : new PendingData();
        long now = clock.millis();
        Long at = data.getLong("t");
        boolean changed = !PENDING.equals(s.getPendingAction()) || at == null || now - at > 60_000
                || (query != null && !query.equals(data.get("q")));
        if (!changed) return;
        if (query != null) data.put("q", query);
        data.put("t", now);
        s.setPendingAction(PENDING);
        s.setPendingData(data.toString());
        access.save(s);
    }

    private BotView backPage(BotContext ctx, Long schoolId, String back) {
        CallbackData data = backData(back);
        return render(ctx, data);
    }

    /** Where "⬅️ Orqaga" on a card leads (see the class comment for the codes). */
    static CallbackData backData(String back) {
        CallbackData lib = CallbackData.of("lib");
        if (back == null || back.isBlank() || "m".equals(back) || "w".equals(back)) return lib;
        if ("r".equals(back)) return lib.with("v", "r");
        String[] parts = back.split("\\.");
        int page = parts.length > 1 ? parseInt(parts[1]) : 0;
        String code = parts[0];
        if (code.startsWith("c")) return lib.with("v", "c").with("c", parseInt(code.substring(1))).with("p", page);
        return switch (code) {
            case "s", "t", "n", "f", "h" -> lib.with("v", code).with("p", page);
            default -> lib;
        };
    }

    private static CallbackData listData(String code) {
        if (code.startsWith("c")) return CallbackData.of("lib").with("v", "c").with("c", code.substring(1));
        return CallbackData.of("lib").with("v", code);
    }

    static DownloadSource sourceOf(String back) {
        if (back == null) return DownloadSource.CATEGORY;
        if (back.startsWith("c")) return DownloadSource.CATEGORY;
        if (back.startsWith("s")) return DownloadSource.SEARCH;
        if (back.startsWith("t")) return DownloadSource.TOP;
        if (back.startsWith("n")) return DownloadSource.NEW;
        if (back.startsWith("f")) return DownloadSource.SAVED;
        if (back.startsWith("h")) return DownloadSource.SAVED;
        if (back.startsWith("r") || back.startsWith("w")) return DownloadSource.RECOMMENDATION;
        return DownloadSource.CATEGORY;
    }

    /** Shelves whose name shares a word with the query; otherwise the fullest shelves. */
    private List<Shelf> similarShelves(Long schoolId, String query) {
        List<Shelf> all = catalog.shelves(schoolId);
        List<String> words = List.of(LibraryText.normalize(query).split(" "));
        List<Shelf> similar = new ArrayList<>();
        for (Shelf s : all) {
            String name = LibraryText.normalize(s.nameUz() + " " + (s.nameRu() == null ? "" : s.nameRu()));
            if (s.count() > 0 && words.stream().anyMatch(w -> w.length() >= 3 && name.contains(w))) similar.add(s);
        }
        if (similar.isEmpty()) {
            all.stream().filter(s -> s.count() > 0)
                    .sorted((a, b) -> Long.compare(b.count(), a.count())).limit(4).forEach(similar::add);
        }
        return similar;
    }

    private InlineButton[] libraryNav(BotContext ctx) {
        return new InlineButton[]{ctx.btn(ctx.t("lib.btn.library"), "lib"), ctx.btn(ctx.t("common.home"), "home")};
    }

    private static String stripTags(String html) {
        return html.replaceAll("<[^>]+>", "");
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static String shorten(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1).stripTrailing() + "…" : s;
    }
}
