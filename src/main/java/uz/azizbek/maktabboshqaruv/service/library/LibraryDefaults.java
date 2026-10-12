package uz.azizbek.maktabboshqaruv.service.library;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.azizbek.maktabboshqaruv.entity.BookCategory;
import uz.azizbek.maktabboshqaruv.repository.BookCategoryRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;

import java.util.List;

/**
 * The shelves a school's library starts with. They are created once, the first time the
 * library of a school is opened (in the bot or on the site); after that the school renames,
 * reorders, hides or adds shelves as it likes.
 */
@Service
public class LibraryDefaults {

    /** emoji, Uzbek name, Russian name */
    static final List<String[]> CATEGORIES = List.of(
            new String[]{"🕵️", "Detektiv", "Детектив"},
            new String[]{"📖", "Badiiy adabiyot", "Художественная литература"},
            new String[]{"🏛", "O'zbek klassikasi", "Узбекская классика"},
            new String[]{"👨‍👩‍👧", "Ota-onalar uchun", "Для родителей"},
            new String[]{"🧒", "Bolalar uchun", "Для детей"},
            new String[]{"🧠", "Ilmiy-ommabop", "Научно-популярное"},
            new String[]{"📜", "Tarix", "История"},
            new String[]{"💼", "Shaxsiy rivojlanish", "Саморазвитие"},
            new String[]{"🇬🇧", "Ingliz tilida", "На английском"});

    private final BookCategoryRepository categories;
    private final SchoolRepository schools;

    public LibraryDefaults(BookCategoryRepository categories, SchoolRepository schools) {
        this.categories = categories;
        this.schools = schools;
    }

    /** Creates the default shelves for a school that has none yet. */
    @Transactional
    public synchronized void ensureCategories(Long schoolId) {
        if (categories.countBySchoolId(schoolId) > 0 || !schools.existsById(schoolId)) return;
        int position = 0;
        for (String[] c : CATEGORIES) {
            BookCategory category = new BookCategory();
            category.setSchool(schools.getReferenceById(schoolId));
            category.setEmoji(c[0]);
            category.setNameUz(c[1]);
            category.setNameRu(c[2]);
            category.setPosition(++position);
            category.setActive(true);
            categories.save(category);
        }
    }
}
