package uz.azizbek.maktabboshqaruv.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * The library search ({@code book.search_key like '%word%'}) is fast with a trigram index.
 * pg_trgm ships with PostgreSQL but enabling it needs the right to create an extension: when
 * that is missing the search still works (a sequential scan of one school's books), we just log it.
 */
@Component
@Order(1)
public class LibrarySearchIndex implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LibrarySearchIndex.class);

    private final JdbcTemplate jdbc;

    public LibrarySearchIndex(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        try {
            jdbc.execute("create extension if not exists pg_trgm");
            jdbc.execute("create index if not exists idx_book_search_trgm on book using gin (search_key gin_trgm_ops)");
        } catch (Exception e) {
            log.info("Kutubxona: pg_trgm indeksi yaratilmadi (qidiruv indekssiz ishlaydi): {}", e.getMessage());
        }
    }
}
