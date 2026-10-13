package uz.azizbek.maktabboshqaruv.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uz.azizbek.maktabboshqaruv.entity.Author;
import uz.azizbek.maktabboshqaruv.entity.Book;
import uz.azizbek.maktabboshqaruv.repository.AuthorRepository;
import uz.azizbek.maktabboshqaruv.repository.BookRepository;
import uz.azizbek.maktabboshqaruv.service.library.LibraryAuthors;
import uz.azizbek.maktabboshqaruv.service.library.LibraryText;

import java.util.List;

/**
 * Every environment: the reference list of Uzbek classics (names, other spellings, years of life —
 * no books), added once by name and never overwritten (admins may correct them). Then books saved
 * before the list existed are linked to their authors by the old text "author" field, which stays.
 */
@Component
@Order(30)
public class AuthorSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AuthorSeeder.class);

    /** name, birth year (null when not sure), death year, other spellings */
    record Classic(String name, Integer born, int died, String aliases) {
    }

    static final List<Classic> CLASSICS = List.of(
            new Classic("Alisher Navoiy", 1441, 1501, "Navoiy\nNizomiddin Mir Alisher\nFoniy\nАлишер Навоий\nАлишер Навои"),
            new Classic("Zahiriddin Muhammad Bobur", 1483, 1530, "Bobur\nБобур\nБабур\nЗаҳириддин Муҳаммад Бобур"),
            new Classic("Boborahim Mashrab", null, 1711, "Mashrab\nМашраб\nБоборахим Машраб"),
            new Classic("Nodira", 1792, 1842, "Nodirabegim\nKomila\nНодира\nНодирабегим"),
            new Classic("Muhammadrizo Ogahiy", 1809, 1874, "Ogahiy\nОгаҳий\nМуҳаммадризо Огаҳий"),
            new Classic("Muqimiy", 1850, 1903, "Muhammad Aminxo'ja Muqimiy\nМуқимий"),
            new Classic("Zokirjon Furqat", 1859, 1909, "Furqat\nФурқат\nЗокиржон Фурқат"),
            new Classic("Zavqiy", 1853, 1921, "Ubaydullo Zavqiy\nЗавқий"),
            new Classic("Mahmudxo'ja Behbudiy", 1875, 1919, "Behbudiy\nБеҳбудий\nМаҳмудхўжа Беҳбудий"),
            new Classic("Hamza Hakimzoda Niyoziy", 1889, 1929, "Hamza\nҲамза\nҲамза Ҳакимзода Ниёзий"),
            new Classic("Abdulla Avloniy", 1878, 1934, "Avloniy\nАвлоний\nАбдулла Авлоний"),
            new Classic("Abdulla Qodiriy", 1894, 1938, "Qodiriy\nJulqunboy\nАбдулла Қодирий\nАбдулла Кадыри"),
            new Classic("Abdulhamid Cho'lpon", 1897, 1938, "Cho'lpon\nAbdulhamid Sulaymon o'g'li Yunusov\nЧўлпон\nЧулпан"),
            new Classic("Abdurauf Fitrat", 1886, 1938, "Fitrat\nФитрат\nАбдурауф Фитрат"),
            new Classic("Elbek", 1898, 1938, "Mashriq Yunusov\nЭлбек"),
            new Classic("Botu", 1904, 1938, "Mahmud Hodiyev\nБоту"),
            new Classic("Usmon Nosir", 1912, 1944, "Усмон Носир"),
            new Classic("Hamid Olimjon", 1909, 1944, "Ҳамид Олимжон"),
            new Classic("Sadriddin Ayniy", 1878, 1954, "Ayniy\nАйний\nСадриддин Айний"));

    private final AuthorRepository authors;
    private final BookRepository books;
    private final LibraryAuthors library;

    public AuthorSeeder(AuthorRepository authors, BookRepository books, LibraryAuthors library) {
        this.authors = authors;
        this.books = books;
        this.library = library;
    }

    @Override
    @Transactional
    public void run(String... args) {
        int added = 0;
        for (Classic c : CLASSICS) {
            if (authors.findByFullNameIgnoreCase(c.name()).isPresent()) continue;
            Author a = new Author();
            a.setFullName(c.name());
            a.setAliases(c.aliases());
            a.setBirthYear(c.born());
            a.setDeathYear(c.died());
            a.setSearchKey(LibraryText.normalize(c.name() + " " + c.aliases().replace('\n', ' ')));
            authors.save(a);
            added++;
        }
        int linked = 0;
        for (Book b : books.unlinkedAuthors()) {
            b.getAuthors().add(library.resolveOrCreate(b.getAuthor()));
            books.save(b);
            linked++;
        }
        if (added > 0 || linked > 0) {
            log.info("Mualliflar ma'lumotnomasi: {} ta muallif qo'shildi, {} ta kitob mualliflarga bog'landi", added, linked);
        }
    }
}
