package uz.azizbek.maktabboshqaruv.service.library;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.dto.AttentionItemDto;
import uz.azizbek.maktabboshqaruv.dto.LibraryDtos.PermissionBookDto;
import uz.azizbek.maktabboshqaruv.dto.LibraryDtos.PermissionDto;
import uz.azizbek.maktabboshqaruv.dto.LibraryDtos.PermissionRequest;
import uz.azizbek.maktabboshqaruv.entity.Book;
import uz.azizbek.maktabboshqaruv.entity.LibraryEnums.LegalBasis;
import uz.azizbek.maktabboshqaruv.entity.LibraryPermission;
import uz.azizbek.maktabboshqaruv.entity.LibraryPermission.Grantor;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.repository.BookRepository;
import uz.azizbek.maktabboshqaruv.repository.LibraryPermissionRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import uz.azizbek.maktabboshqaruv.service.ActivityLogService;
import uz.azizbek.maktabboshqaruv.service.SchoolAccessService;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Ruxsatnomalar — the rights holders' permissions behind MUALLIF_RUXSATI / LITSENZIYA books.
 * A permission needs its signed scan. When it is revoked its books go inactive at once; when it
 * runs out, the daily job (06:15 Asia/Tashkent) makes its books inactive — they leave the bot and
 * the search — and writes it to the school's activity log; 30 days before the end it warns once.
 * Ended and ending permissions also show in the admin's attention list (the bell in the header).
 */
@Service
public class LibraryPermissions {

    private static final Logger log = LoggerFactory.getLogger(LibraryPermissions.class);
    public static final int WARN_DAYS = 30;
    private static final long MAX_DOCUMENT_BYTES = 10L * 1024 * 1024;

    private final LibraryPermissionRepository permissions;
    private final BookRepository books;
    private final SchoolRepository schools;
    private final LibraryFiles files;
    private final LibraryCatalog catalog;
    private final ActivityLogService activity;
    private final SchoolAccessService access;
    private final Clock clock;

    public LibraryPermissions(LibraryPermissionRepository permissions, BookRepository books, SchoolRepository schools,
                              LibraryFiles files, LibraryCatalog catalog, ActivityLogService activity,
                              SchoolAccessService access, Clock clock) {
        this.permissions = permissions;
        this.books = books;
        this.schools = schools;
        this.files = files;
        this.catalog = catalog;
        this.activity = activity;
        this.access = access;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ list / read

    @Transactional(readOnly = true)
    public List<PermissionDto> list(Long schoolId, String status) {
        access.requireSchool(schoolId);
        LocalDate today = LocalDate.now(clock);
        Map<Long, List<PermissionBookDto>> byPermission = new HashMap<>();
        List<LibraryPermission> all = permissions.findBySchoolIdOrderByCreatedAtDesc(schoolId);
        for (LibraryPermission p : all) {
            byPermission.put(p.getId(), books.findByPermission(p.getId()).stream()
                    .map(b -> new PermissionBookDto(b.getId(), b.getTitle(), Boolean.TRUE.equals(b.getActive()))).toList());
        }
        return all.stream().map(p -> toDto(p, byPermission.get(p.getId()), today))
                .filter(d -> status == null || status.isBlank() || "all".equals(status) || d.status().equals(status))
                .toList();
    }

    public static String statusOf(LibraryPermission p, LocalDate today) {
        if (Boolean.TRUE.equals(p.getRevoked())) return "REVOKED";
        if (p.getValidFrom().isAfter(today)) return "NOT_STARTED";
        if (p.getValidUntil() != null && p.getValidUntil().isBefore(today)) return "EXPIRED";
        if (p.getValidUntil() != null && !p.getValidUntil().isAfter(today.plusDays(WARN_DAYS))) return "ENDING";
        return "ACTIVE";
    }

    PermissionDto toDto(LibraryPermission p, List<PermissionBookDto> bookList, LocalDate today) {
        Integer daysLeft = p.getValidUntil() == null ? null : (int) ChronoUnit.DAYS.between(today, p.getValidUntil());
        return new PermissionDto(p.getId(), p.getGrantor().name(), p.getGrantorFullName(), p.getPhone(), p.getScope(),
                p.getValidFrom(), p.getValidUntil(), p.getNote(), statusOf(p, today), daysLeft, p.getDocumentType(),
                bookList == null ? List.of() : bookList, p.getCreatedBy(), p.getCreatedAt());
    }

    public LibraryPermission owned(Long id) {
        LibraryPermission p = permissions.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ruxsatnoma topilmadi"));
        access.requireSchool(p.getSchool().getId());
        return p;
    }

    public record Document(byte[] bytes, String contentType) {
    }

    @Transactional(readOnly = true)
    public Document document(Long id) {
        LibraryPermission p = owned(id);
        return new Document(files.read(p.getDocumentPath()), p.getDocumentType());
    }

    // ------------------------------------------------------------------ create / edit / revoke

    @Transactional
    public PermissionDto create(Long schoolId, PermissionRequest r, MultipartFile document) {
        access.requireSchool(schoolId);
        School school = schools.findById(schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Maktab topilmadi"));
        if (document == null || document.isEmpty()) {
            throw LibraryFiles.bad("Imzolangan ruxsat xatining skani (PDF/JPG/PNG) majburiy");
        }
        LibraryPermission p = new LibraryPermission();
        p.setSchool(school);
        p.setRevoked(false);
        p.setScope(LibraryPermission.DEFAULT_SCOPE);
        p.setCreatedBy(access.caller().username());
        p.setCreatedAt(LocalDateTime.now(clock));
        apply(p, r);
        storeDocument(p, schoolId, document);
        LibraryPermission saved = permissions.save(p);
        linkBooks(saved, r.bookIds());
        return toDto(saved, booksOf(saved), LocalDate.now(clock));
    }

    @Transactional
    public PermissionDto update(Long id, PermissionRequest r, MultipartFile document) {
        LibraryPermission p = owned(id);
        if (Boolean.TRUE.equals(p.getRevoked())) throw LibraryFiles.bad("Bekor qilingan ruxsatnomani tahrirlab bo'lmaydi");
        apply(p, r);
        p.setExpiryWarnedAt(null); // a new end date may need a new warning
        if (document != null && !document.isEmpty()) {
            String old = p.getDocumentPath();
            storeDocument(p, p.getSchool().getId(), document);
            files.delete(old);
        }
        permissions.save(p);
        if (r.bookIds() != null) linkBooks(p, r.bookIds());
        // shortened into the past: its books stop at once, like the daily job would
        if (!p.inForce(LocalDate.now(clock))) deactivateBooks(p, "muddati o'zgartirildi");
        return toDto(p, booksOf(p), LocalDate.now(clock));
    }

    /** "Bekor qilish": the rights holder withdrew the permission — its books stop at once. */
    @Transactional
    public PermissionDto revoke(Long id) {
        LibraryPermission p = owned(id);
        if (!Boolean.TRUE.equals(p.getRevoked())) {
            p.setRevoked(true);
            p.setRevokedAt(LocalDateTime.now(clock));
            permissions.save(p);
            deactivateBooks(p, "bekor qilindi");
        }
        return toDto(p, booksOf(p), LocalDate.now(clock));
    }

    private void apply(LibraryPermission p, PermissionRequest r) {
        Grantor grantor;
        try {
            grantor = Grantor.valueOf(r.grantor());
        } catch (Exception e) {
            throw LibraryFiles.bad("Ruxsat beruvchi: MUALLIF, MEROSXOR yoki NASHRIYOT");
        }
        if (r.validUntil() != null && r.validUntil().isBefore(r.validFrom())) {
            throw LibraryFiles.bad("Tugash sanasi boshlanish sanasidan oldin bo'lishi mumkin emas");
        }
        p.setGrantor(grantor);
        p.setGrantorFullName(r.grantorFullName().strip());
        p.setPhone(r.phone() == null || r.phone().isBlank() ? null : r.phone().strip());
        p.setValidFrom(r.validFrom());
        p.setValidUntil(r.validUntil());
        p.setNote(r.note() == null || r.note().isBlank() ? null : r.note().strip());
    }

    /** The scan of the signed letter: PDF, JPG or PNG by its first bytes, up to 10 MB. */
    private void storeDocument(LibraryPermission p, Long schoolId, MultipartFile document) {
        byte[] bytes;
        try {
            bytes = document.getBytes();
        } catch (IOException e) {
            throw LibraryFiles.bad("Faylni o'qib bo'lmadi");
        }
        if (bytes.length > MAX_DOCUMENT_BYTES) throw LibraryFiles.bad("Hujjat juda katta: ko'pi bilan 10 MB");
        String ext;
        String type;
        if (LibraryFiles.isPdf(bytes)) {
            ext = "pdf";
            type = "application/pdf";
        } else if (bytes.length > 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            ext = "jpg";
            type = "image/jpeg";
        } else if (bytes.length > 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            ext = "png";
            type = "image/png";
        } else {
            throw LibraryFiles.bad("Hujjat PDF, JPG yoki PNG bo'lishi kerak");
        }
        p.setDocumentPath(files.saveCover(schoolId, ext, bytes));
        p.setDocumentType(type);
    }

    /** Links the chosen books of this school to the permission (they become MUALLIF_RUXSATI if not a licence). */
    private void linkBooks(LibraryPermission p, List<Long> bookIds) {
        if (bookIds == null) return;
        for (Long id : bookIds) {
            Book b = books.findById(id).orElseThrow(() -> LibraryFiles.bad("Kitob topilmadi (#" + id + ")"));
            if (!b.getSchool().getId().equals(p.getSchool().getId())) throw LibraryFiles.bad("Kitob boshqa maktabga tegishli");
            b.setPermission(p);
            if (b.getLegalBasis() != LegalBasis.LITSENZIYA) b.setLegalBasis(LegalBasis.MUALLIF_RUXSATI);
            books.save(b);
        }
        catalog.evict(p.getSchool().getId());
    }

    private List<PermissionBookDto> booksOf(LibraryPermission p) {
        return books.findByPermission(p.getId()).stream()
                .map(b -> new PermissionBookDto(b.getId(), b.getTitle(), Boolean.TRUE.equals(b.getActive()))).toList();
    }

    /** Makes the permission's active books inactive and writes it to the school's activity log. */
    int deactivateBooks(LibraryPermission p, String why) {
        List<Book> active = books.findByPermissionIdAndActiveTrue(p.getId());
        for (Book b : active) {
            b.setActive(false);
            b.setUpdatedAt(LocalDateTime.now(clock));
            books.save(b);
        }
        if (!active.isEmpty()) {
            catalog.evict(p.getSchool().getId());
            activity.record(p.getSchool(), "gavel", "Ruxsatnoma " + why + ": " + p.getGrantorFullName() + " — "
                    + active.size() + " ta kitob botdan olindi (nofaol)");
            log.info("Ruxsatnoma #{} {}: {} ta kitob nofaol qilindi", p.getId(), why, active.size());
        }
        return active.size();
    }

    // ------------------------------------------------------------------ daily job

    @Scheduled(cron = "${app.library.permission-check-cron:0 15 6 * * *}", zone = "Asia/Tashkent")
    public void daily() {
        try {
            runDaily(LocalDate.now(clock));
        } catch (Exception e) {
            log.error("Ruxsatnomalarni tekshirishda xato", e);
        }
    }

    /** Ended permissions' books go inactive; permissions ending within 30 days are warned about once. */
    @Transactional
    public int runDaily(LocalDate today) {
        int stopped = 0;
        for (LibraryPermission p : permissions.endedWithActiveBooks(today)) {
            stopped += deactivateBooks(p, Boolean.TRUE.equals(p.getRevoked()) ? "bekor qilingan" : "muddati tugadi");
        }
        for (LibraryPermission p : permissions.endingSoonUnwarned(today, today.plusDays(WARN_DAYS))) {
            long days = ChronoUnit.DAYS.between(today, p.getValidUntil());
            activity.record(p.getSchool(), "hourglass_top", "Ruxsatnoma " + days + " kundan keyin tugaydi: "
                    + p.getGrantorFullName() + " (" + p.getValidUntil() + "). Yangilang, aks holda kitoblar botdan olinadi.");
            p.setExpiryWarnedAt(LocalDateTime.now(clock));
            permissions.save(p);
        }
        return stopped;
    }

    /** For the admin's bell: ended or ending (≤ 30 days) permissions of the school. */
    @Transactional(readOnly = true)
    public List<AttentionItemDto> attention(Long schoolId) {
        LocalDate today = LocalDate.now(clock);
        List<AttentionItemDto> items = new ArrayList<>();
        for (LibraryPermission p : permissions.endingBy(schoolId, today.plusDays(WARN_DAYS))) {
            if (p.getValidUntil().isBefore(today)) {
                if (books.countByPermissionId(p.getId()) == 0) continue;
                items.add(new AttentionItemDto("PERMISSION_EXPIRED", "Ruxsatnoma muddati tugagan: " + p.getGrantorFullName()
                        + " (" + p.getValidUntil() + ") — kitoblari botdan olindi", "library-permissions", p.getId()));
            } else {
                long days = ChronoUnit.DAYS.between(today, p.getValidUntil());
                items.add(new AttentionItemDto("PERMISSION_ENDING", "Ruxsatnoma " + days + " kunda tugaydi: "
                        + p.getGrantorFullName(), "library-permissions", p.getId()));
            }
        }
        return items;
    }
}
