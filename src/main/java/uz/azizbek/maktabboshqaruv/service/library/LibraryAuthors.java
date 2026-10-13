package uz.azizbek.maktabboshqaruv.service.library;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.dto.LibraryDtos.AuthorDto;
import uz.azizbek.maktabboshqaruv.dto.LibraryDtos.AuthorRequest;
import uz.azizbek.maktabboshqaruv.entity.Author;
import uz.azizbek.maktabboshqaruv.repository.AuthorRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;

/**
 * The authors reference list: who wrote a book and when they died, which decides "Jamoat mulki"
 * (CopyrightRules). Shared by all schools; staff read it, admins edit it.
 */
@Service
public class LibraryAuthors {

    private final AuthorRepository authors;
    private final Clock clock;

    public LibraryAuthors(AuthorRepository authors, Clock clock) {
        this.authors = authors;
        this.clock = clock;
    }

    /** This year in Tashkent (the application clock's zone). */
    public int currentYear() {
        return LocalDate.now(clock).getYear();
    }

    public CopyrightRules.Status status(Author a) {
        return CopyrightRules.status(a.getDeathYear(), currentYear());
    }

    @Transactional(readOnly = true)
    public List<AuthorDto> list() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : authors.bookCounts()) counts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        return authors.findAllByOrderByFullNameAsc().stream().map(a -> toDto(a, counts.getOrDefault(a.getId(), 0L))).toList();
    }

    public Author require(Long id) {
        return authors.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Muallif topilmadi"));
    }

    @Transactional
    public AuthorDto create(AuthorRequest r) {
        if (authors.findByFullNameIgnoreCase(r.fullName().strip()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu muallif ro'yxatda bor: " + r.fullName().strip());
        }
        Author a = new Author();
        apply(a, r);
        return toDto(authors.save(a), 0);
    }

    @Transactional
    public AuthorDto update(Long id, AuthorRequest r) {
        Author a = require(id);
        authors.findByFullNameIgnoreCase(r.fullName().strip())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu nom boshqa muallifda bor");
                });
        apply(a, r);
        return toDto(authors.save(a), authors.bookCount(id));
    }

    @Transactional
    public void delete(Long id) {
        Author a = require(id);
        if (authors.bookCount(id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu muallifning kitoblari bor — o'chirib bo'lmaydi");
        }
        authors.delete(a);
    }

    /**
     * The author a typed name means: by full name or any alias (case, apostrophes and script do not
     * matter); a new entry without years when nobody matches.
     */
    @Transactional
    public Author resolveOrCreate(String name) {
        Author found = match(name);
        if (found != null) return found;
        Author a = new Author();
        a.setFullName(name.strip().length() > 200 ? name.strip().substring(0, 200) : name.strip());
        a.setSearchKey(keyOf(a.getFullName(), null));
        return authors.save(a);
    }

    /** The author whose name or alias equals {@code name} after normalisation, or null. */
    @Transactional(readOnly = true)
    public Author match(String name) {
        String n = LibraryText.normalize(name);
        if (n.isEmpty()) return null;
        for (Author a : authors.findAll()) {
            for (String variant : variants(a)) {
                if (LibraryText.normalize(variant).equals(n)) return a;
            }
        }
        return null;
    }

    public static List<String> variants(Author a) {
        List<String> list = new ArrayList<>();
        list.add(a.getFullName());
        if (a.getAliases() != null) {
            for (String s : a.getAliases().split("[\\n;,]")) if (!s.isBlank()) list.add(s.strip());
        }
        return list;
    }

    private void apply(Author a, AuthorRequest r) {
        if (r.birthYear() != null && r.deathYear() != null && r.deathYear() < r.birthYear()) {
            throw LibraryFiles.bad("Vafot yili tug'ilgan yildan oldin bo'lishi mumkin emas");
        }
        if (r.deathYear() != null && r.deathYear() > currentYear()) {
            throw LibraryFiles.bad("Vafot yili kelajakda bo'lishi mumkin emas");
        }
        a.setFullName(r.fullName().strip());
        a.setAliases(r.aliases() == null || r.aliases().isBlank() ? null : r.aliases().strip());
        a.setBirthYear(r.birthYear());
        a.setDeathYear(r.deathYear());
        a.setNote(r.note() == null || r.note().isBlank() ? null : r.note().strip());
        a.setSearchKey(keyOf(a.getFullName(), a.getAliases()));
    }

    static String keyOf(String fullName, String aliases) {
        return LibraryText.normalize(fullName + " " + (aliases == null ? "" : aliases.replace('\n', ' ')));
    }

    public AuthorDto toDto(Author a, long books) {
        CopyrightRules.Status s = status(a);
        return new AuthorDto(a.getId(), a.getFullName(), a.getAliases(), a.getBirthYear(), a.getDeathYear(), a.getNote(),
                s.state(), s.year(), books);
    }
}
