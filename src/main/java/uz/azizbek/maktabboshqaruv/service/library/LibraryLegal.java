package uz.azizbek.maktabboshqaruv.service.library;

import org.springframework.stereotype.Component;
import uz.azizbek.maktabboshqaruv.dto.LibraryDtos.BookRequest;
import uz.azizbek.maktabboshqaruv.entity.Author;
import uz.azizbek.maktabboshqaruv.entity.Book;
import uz.azizbek.maktabboshqaruv.entity.LibraryEnums.LegalBasis;
import uz.azizbek.maktabboshqaruv.entity.LibraryPermission;
import uz.azizbek.maktabboshqaruv.repository.AuthorRepository;
import uz.azizbek.maktabboshqaruv.repository.LibraryPermissionRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The legal checks every saved book passes (site form, bulk upload, Wikisource import):
 * <ul>
 *   <li><b>JAMOAT_MULKI</b> — every author has a death year; the latest one + 70 is before this year
 *       (CopyrightRules, qonun 35-modda); the work was published while the author lived, or its first
 *       edition year + 70 is before this year; and the file has no modern foreword or commentary
 *       (37-modda: the work must not be distorted).</li>
 *   <li><b>MUALLIF_RUXSATI / LITSENZIYA</b> — linked to this school's permission that is in force today.</li>
 * </ul>
 * A failed check is a 400 with the reason in Uzbek; nothing is saved.
 */
@Component
public class LibraryLegal {

    private final AuthorRepository authorRepository;
    private final LibraryPermissionRepository permissions;
    private final LibraryAuthors authors;
    private final Clock clock;

    public LibraryLegal(AuthorRepository authorRepository, LibraryPermissionRepository permissions, LibraryAuthors authors,
                        Clock clock) {
        this.authorRepository = authorRepository;
        this.permissions = permissions;
        this.authors = authors;
        this.clock = clock;
    }

    /** Sets the authors, the permission and the confirmations of the request on the book — after checking them. */
    public void apply(Book b, BookRequest r, Long schoolId) {
        List<Author> list = resolveAuthors(r);
        LegalBasis basis = r.legalBasis();
        LibraryPermission permission = null;
        if (basis == LegalBasis.JAMOAT_MULKI) {
            checkPublicDomain(list, r.publishedInLifetime(), r.firstPublishedYear(), r.noModernCommentary());
        } else if (basis == LegalBasis.MUALLIF_RUXSATI || basis == LegalBasis.LITSENZIYA) {
            permission = permissionInForce(r.permissionId(), schoolId);
        }
        b.getAuthors().clear();
        b.getAuthors().addAll(list);
        b.setAuthor(list.isEmpty() ? null : list.stream().map(Author::getFullName).collect(Collectors.joining(", ")));
        b.setPermission(permission);
        b.setPublishedInLifetime(basis == LegalBasis.JAMOAT_MULKI ? Boolean.TRUE.equals(r.publishedInLifetime()) : null);
        b.setFirstPublishedYear(basis == LegalBasis.JAMOAT_MULKI && !Boolean.TRUE.equals(r.publishedInLifetime())
                ? r.firstPublishedYear() : null);
        b.setNoModernCommentary(basis == LegalBasis.JAMOAT_MULKI ? Boolean.TRUE : null);
    }

    List<Author> resolveAuthors(BookRequest r) {
        List<Author> list = new ArrayList<>();
        if (r.authorIds() != null && !r.authorIds().isEmpty()) {
            for (Long id : r.authorIds()) {
                Author a = authorRepository.findById(id).orElseThrow(() -> LibraryFiles.bad("Muallif topilmadi (#" + id + ")"));
                if (!list.contains(a)) list.add(a);
            }
        } else if (r.author() != null && !r.author().isBlank()) {
            for (String name : r.author().split("[,;]")) {
                if (!name.isBlank()) {
                    Author a = authors.resolveOrCreate(name);
                    if (!list.contains(a)) list.add(a);
                }
            }
        }
        return list;
    }

    /** The public-domain rules; throws 400 with the reason. */
    public void checkPublicDomain(List<Author> list, Boolean publishedInLifetime, Integer firstPublishedYear,
                                  Boolean noModernCommentary) {
        int year = LocalDate.now(clock).getYear();
        if (list.isEmpty()) throw LibraryFiles.bad("Jamoat mulki uchun muallif tanlanishi shart");
        for (Author a : list) {
            if (a.getDeathYear() == null) {
                throw LibraryFiles.bad("«" + a.getFullName() + "» muallifining vafot yili kiritilmagan — asar jamoat "
                        + "mulki ekanini aniqlab bo'lmaydi. «Mualliflar» bo'limida vafot yilini kiriting.");
            }
        }
        int deciding = CopyrightRules.decidingDeathYear(list.stream().map(Author::getDeathYear).toList());
        if (!CopyrightRules.isFree(deciding, year)) {
            throw LibraryFiles.bad("Bu muallif asarlari " + CopyrightRules.protectedUntil(deciding)
                    + "-yilgacha mualliflik huquqi bilan himoyalangan. Muallif yoki merosxo'rlar ruxsati kerak.");
        }
        if (!Boolean.TRUE.equals(publishedInLifetime)) {
            if (firstPublishedYear == null) {
                throw LibraryFiles.bad("«Asar muallif tirikligida chop etilgan» belgilanmagan — birinchi nashr yilini kiriting");
            }
            if (!CopyrightRules.isFree(firstPublishedYear, year)) {
                throw LibraryFiles.bad("Asar muallif vafotidan keyin " + firstPublishedYear + "-yilda birinchi marta "
                        + "chop etilgan — " + CopyrightRules.protectedUntil(firstPublishedYear)
                        + "-yilgacha mualliflik huquqi bilan himoyalangan. Huquq egasining ruxsati kerak.");
            }
        }
        if (!Boolean.TRUE.equals(noModernCommentary)) {
            throw LibraryFiles.bad("«Faylda zamonaviy so'zboshi, izoh yoki sharh yo'q» tasdig'i majburiy");
        }
    }

    LibraryPermission permissionInForce(Long permissionId, Long schoolId) {
        if (permissionId == null) {
            throw LibraryFiles.bad("Muallif ruxsati yoki litsenziya uchun amaldagi ruxsatnoma tanlanishi shart "
                    + "(«Ruxsatnomalar» bo'limi)");
        }
        LibraryPermission p = permissions.findById(permissionId)
                .orElseThrow(() -> LibraryFiles.bad("Ruxsatnoma topilmadi"));
        if (!p.getSchool().getId().equals(schoolId)) throw LibraryFiles.bad("Ruxsatnoma boshqa maktabga tegishli");
        if (!p.inForce(LocalDate.now(clock))) {
            throw LibraryFiles.bad("Ruxsatnoma amalda emas (muddati o'tgan, hali boshlanmagan yoki bekor qilingan)");
        }
        return p;
    }
}
