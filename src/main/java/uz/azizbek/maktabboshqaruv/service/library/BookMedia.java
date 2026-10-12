package uz.azizbek.maktabboshqaruv.service.library;

import org.springframework.stereotype.Component;
import uz.azizbek.maktabboshqaruv.repository.BookRepository;
import uz.azizbek.maktabboshqaruv.service.OutboxMedia;
import uz.azizbek.maktabboshqaruv.telegram.TelegramClient.MediaKind;
import uz.azizbek.maktabboshqaruv.telegram.TelegramProperties;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Outbox media "book:&lt;id&gt;": the book's PDF as a document named "Muallif - Nomi.pdf".
 * The first parent's copy is uploaded from our storage; Telegram's file_id is remembered on
 * the book and every later parent gets the file by that id — the server never uploads it again.
 * In mock mode the ids live in memory only (they mean nothing to the real Telegram), as with banners.
 */
@Component
public class BookMedia implements OutboxMedia.Resolver {

    static final String PREFIX = "book:";

    private final BookRepository books;
    private final LibraryFiles files;
    private final boolean persist;
    private final Map<Long, String> mockFileIds = new ConcurrentHashMap<>();

    public BookMedia(BookRepository books, LibraryFiles files, TelegramProperties properties) {
        this.books = books;
        this.files = files;
        this.persist = !properties.isMock();
    }

    public static String ref(Long bookId) {
        return PREFIX + bookId;
    }

    @Override
    public boolean supports(String media) {
        return media.startsWith(PREFIX);
    }

    @Override
    public List<OutboxMedia.File> files(String media) {
        Long id = Long.valueOf(media.substring(PREFIX.length()));
        return books.findById(id).map(b -> {
            String path = b.getPdfPath();
            String fileId = persist ? b.getTelegramFileId() : mockFileIds.get(id);
            return List.of(new OutboxMedia.File(MediaKind.DOCUMENT, fileId, () -> files.read(path),
                    LibraryFiles.downloadName(b.getAuthor(), b.getTitle()),
                    newId -> {
                        if (newId.equals(fileId)) return;
                        if (persist) books.rememberFileId(id, newId);
                        else mockFileIds.put(id, newId);
                    }));
        }).orElse(List.of());
    }

    /** A new PDF was uploaded for the book: the old file_id points at the old file. */
    public void forget(Long bookId) {
        mockFileIds.remove(bookId);
    }
}
