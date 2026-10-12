package uz.azizbek.maktabboshqaruv.service.library;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.azizbek.maktabboshqaruv.dto.LibraryDtos.PageDto;
import uz.azizbek.maktabboshqaruv.service.SchoolAccessService;
import uz.azizbek.maktabboshqaruv.util.XlsxWriter;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * "Mutolaa statistikasi" — who reads what. Admin only (the controller enforces the role), always
 * within one school. The child and the class of a download are not stored with it: they come
 * from the parent's links (chat → children → class) at report time, so a parent with two
 * children shows both ("Aziz Karimov (5-A), Madina Karimova (2-B)"). Each list is one SQL
 * statement (plus its count) — no query per row.
 */
@Service
public class LibraryStatsService {

    private static final int READER_DAYS = 30;
    private static final int EXPORT_LIMIT = 20_000;
    private static final DateTimeFormatter EXPORT_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final NamedParameterJdbcTemplate jdbc;
    private final SchoolAccessService access;
    private final Clock clock;

    public LibraryStatsService(NamedParameterJdbcTemplate jdbc, SchoolAccessService access, Clock clock) {
        this.jdbc = jdbc;
        this.access = access;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ overview

    public record Kpi(long totalBooks, long monthDownloads, long readingParents, long linkedParents, int readingPercent,
                      String topClass, long topClassReaders) {
    }

    public record DayCount(LocalDate date, long count) {
    }

    public record CategoryCount(Long id, String label, long count) {
    }

    public record ClassReport(Long classId, String className, int grade, long students, long linkedParents,
                              long readingParents, int percent) {
    }

    public record TopBook(Long id, String title, String author, boolean hasCover, long downloads, int likes, int dislikes,
                          Integer likePercent) {
    }

    public record Overview(Kpi kpi, List<DayCount> daily, List<CategoryCount> byCategory, List<ClassReport> byClass,
                           List<TopBook> top) {
    }

    @Transactional(readOnly = true)
    public Overview overview(Long schoolId) {
        access.requireSchool(schoolId);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime since = now.toLocalDate().minusDays(READER_DAYS - 1L).atStartOfDay();
        MapSqlParameterSource p = new MapSqlParameterSource("s", schoolId)
                .addValue("since", Timestamp.valueOf(since))
                .addValue("month", Timestamp.valueOf(now.toLocalDate().withDayOfMonth(1).atStartOfDay()));

        Map<String, Object> k = jdbc.queryForMap("""
                select (select count(*) from book where school_id = :s and active = true and draft = false) books,
                       (select count(*) from book_download where school_id = :s and downloaded_at >= :month) month_downloads,
                       (select count(distinct chat_id) from book_download where school_id = :s and downloaded_at >= :since) readers,
                       (select count(distinct l.chat_id) from parent_telegram_link l join students st on st.id = l.student_id
                          join school_class sc on sc.id = st.class_id join academic_year ay on ay.id = sc.academic_year_id
                         where l.active = true and ay.school_id = :s) linked
                """, p);
        long readers = num(k.get("readers"));
        long linked = num(k.get("linked"));

        List<ClassReport> byClass = jdbc.query("""
                select * from (
                  select sc.id, sc.grade_number, sc.section_letter,
                         (select count(*) from students st where st.class_id = sc.id) students,
                         (select count(distinct l.chat_id) from parent_telegram_link l join students st on st.id = l.student_id
                           where st.class_id = sc.id and l.active = true) linked,
                         (select count(distinct l.chat_id) from parent_telegram_link l join students st on st.id = l.student_id
                           where st.class_id = sc.id and l.active = true and exists (select 1 from book_download d
                             where d.chat_id = l.chat_id and d.school_id = :s and d.downloaded_at >= :since)) readers
                    from school_class sc join academic_year ay on ay.id = sc.academic_year_id
                   where ay.school_id = :s) x
                 where students > 0
                 order by grade_number, section_letter
                """, p, (rs, i) -> {
            long l = rs.getLong("linked");
            long r = rs.getLong("readers");
            return new ClassReport(rs.getLong("id"), rs.getInt("grade_number") + "-" + rs.getString("section_letter"),
                    rs.getInt("grade_number"), rs.getLong("students"), l, r, l == 0 ? 0 : (int) Math.round(r * 100.0 / l));
        });
        ClassReport best = byClass.stream().filter(c -> c.readingParents() > 0)
                .max(Comparator.comparingLong(ClassReport::readingParents).thenComparingInt(ClassReport::percent))
                .orElse(null);

        Map<LocalDate, Long> perDay = new HashMap<>();
        jdbc.query("""
                select cast(downloaded_at as date) d, count(*) n from book_download
                 where school_id = :s and downloaded_at >= :since group by 1
                """, p, rs -> {
            perDay.put(rs.getDate("d").toLocalDate(), rs.getLong("n"));
        });
        List<DayCount> daily = new ArrayList<>();
        for (LocalDate d = since.toLocalDate(); !d.isAfter(now.toLocalDate()); d = d.plusDays(1)) {
            daily.add(new DayCount(d, perDay.getOrDefault(d, 0L)));
        }

        List<CategoryCount> byCategory = jdbc.query("""
                select c.id, c.emoji, c.name_uz, count(*) n from book_download d join book b on b.id = d.book_id
                  left join book_category c on c.id = b.category_id
                 where d.school_id = :s and d.downloaded_at >= :since
                 group by c.id, c.emoji, c.name_uz order by n desc
                """, p, (rs, i) -> new CategoryCount(rs.getObject("id") == null ? null : rs.getLong("id"),
                rs.getString("name_uz") == null ? "Bo'limsiz"
                        : ((rs.getString("emoji") == null ? "" : rs.getString("emoji") + " ") + rs.getString("name_uz")),
                rs.getLong("n")));

        List<TopBook> top = jdbc.query("""
                select id, title, author, cover_path is not null has_cover, download_count, likes, dislikes from book
                 where school_id = :s and draft = false and download_count > 0
                 order by download_count desc, id limit 10
                """, p, (rs, i) -> {
            int likes = rs.getInt("likes");
            int dislikes = rs.getInt("dislikes");
            return new TopBook(rs.getLong("id"), rs.getString("title"), rs.getString("author"), rs.getBoolean("has_cover"),
                    rs.getLong("download_count"), likes, dislikes,
                    likes + dislikes == 0 ? null : (int) Math.round(likes * 100.0 / (likes + dislikes)));
        });

        Kpi kpi = new Kpi(num(k.get("books")), num(k.get("month_downloads")), readers, linked,
                linked == 0 ? 0 : (int) Math.min(100, Math.round(readers * 100.0 / linked)),
                best == null ? null : best.className(), best == null ? 0 : best.readingParents());
        return new Overview(kpi, daily, byCategory, byClass, top);
    }

    // ------------------------------------------------------------------ journal

    public record JournalFilter(LocalDate from, LocalDate to, Long classId, Long categoryId, Long bookId, String q) {
    }

    public record JournalRow(Long id, LocalDateTime downloadedAt, Long chatId, String parentName, String parentPhone,
                             String telegramName, String children, String classes, Long bookId, String bookTitle,
                             String bookAuthor, String category, String source) {
    }

    private static final String JOURNAL_FROM = """
             from book_download d
             join book b on b.id = d.book_id
             left join book_category c on c.id = b.category_id
             left join parent_session ps on ps.chat_id = d.chat_id
            """;

    @Transactional(readOnly = true)
    public PageDto<JournalRow> journal(Long schoolId, JournalFilter f, int page, int size) {
        access.requireSchool(schoolId);
        MapSqlParameterSource p = new MapSqlParameterSource();
        String where = where(schoolId, f, p);
        long total = num(jdbc.queryForObject("select count(*)" + JOURNAL_FROM + where, p, Long.class));
        int pageSize = Math.min(Math.max(size, 1), 200);
        p.addValue("limit", pageSize).addValue("offset", (long) Math.max(page, 0) * pageSize);
        List<JournalRow> rows = jdbc.query(journalSql(where) + " limit :limit offset :offset", p, (rs, i) -> row(rs));
        return new PageDto<>(rows, total, Math.max(page, 0), pageSize);
    }

    @Transactional(readOnly = true)
    public byte[] export(Long schoolId, JournalFilter f) {
        access.requireSchool(schoolId);
        MapSqlParameterSource p = new MapSqlParameterSource();
        String where = where(schoolId, f, p);
        p.addValue("limit", EXPORT_LIMIT).addValue("offset", 0);
        List<JournalRow> rows = jdbc.query(journalSql(where) + " limit :limit offset :offset", p, (rs, i) -> row(rs));
        List<List<Object>> cells = new ArrayList<>();
        for (JournalRow r : rows) {
            cells.add(Arrays.asList(r.downloadedAt().format(EXPORT_TIME), r.parentName(), r.parentPhone(), r.children(),
                    r.classes(), r.bookTitle(), r.bookAuthor(), r.category(), sourceLabel(r.source())));
        }
        return XlsxWriter.write("Yuklashlar", List.of("Sana-vaqt", "Ota-ona", "Telefon", "Farzandi", "Sinfi", "Kitob",
                "Muallif", "Bo'lim", "Manba"), List.of(17, 26, 16, 40, 12, 36, 24, 22, 14), cells);
    }

    private static String journalSql(String where) {
        return """
                select d.id, d.downloaded_at, d.chat_id, d.source, b.id book_id, b.title, b.author,
                       c.emoji, c.name_uz, ps.first_name tg_first, ps.username tg_user,
                       g.guardian_name, g.guardian_phone, ch.children, ch.classes
                """ + JOURNAL_FROM + """
                 left join lateral (select st.guardian_name, st.guardian_phone from parent_telegram_link l
                     join students st on st.id = l.student_id where l.chat_id = d.chat_id
                     order by l.active desc, l.id limit 1) g on true
                 left join lateral (select string_agg(st.first_name || ' ' || st.last_name || ' (' || sc.grade_number || '-'
                            || sc.section_letter || ')', ', ' order by sc.grade_number, st.first_name) children,
                          string_agg(distinct sc.grade_number || '-' || sc.section_letter, ', ') classes
                     from parent_telegram_link l join students st on st.id = l.student_id
                     join school_class sc on sc.id = st.class_id join academic_year ay on ay.id = sc.academic_year_id
                    where l.chat_id = d.chat_id and l.active = true and ay.school_id = d.school_id) ch on true
                """ + where + " order by d.downloaded_at desc, d.id desc";
    }

    private String where(Long schoolId, JournalFilter f, MapSqlParameterSource p) {
        StringBuilder w = new StringBuilder(" where d.school_id = :s");
        p.addValue("s", schoolId);
        if (f.from() != null) {
            w.append(" and d.downloaded_at >= :from");
            p.addValue("from", Timestamp.valueOf(f.from().atStartOfDay()));
        }
        if (f.to() != null) {
            w.append(" and d.downloaded_at < :to");
            p.addValue("to", Timestamp.valueOf(f.to().plusDays(1).atStartOfDay()));
        }
        if (f.categoryId() != null) {
            w.append(" and b.category_id = :category");
            p.addValue("category", f.categoryId());
        }
        if (f.bookId() != null) {
            w.append(" and d.book_id = :book");
            p.addValue("book", f.bookId());
        }
        if (f.classId() != null) {
            w.append(" and exists (select 1 from parent_telegram_link l join students st on st.id = l.student_id"
                    + " where l.chat_id = d.chat_id and l.active = true and st.class_id = :class)");
            p.addValue("class", f.classId());
        }
        if (f.q() != null && !f.q().isBlank()) {
            String like = "%" + f.q().strip().toLowerCase(Locale.ROOT).replace("%", "").replace("_", "") + "%";
            String digits = f.q().replaceAll("\\D", "");
            w.append(" and (lower(coalesce(ps.first_name, '')) like :q or lower(coalesce(ps.username, '')) like :q"
                    + " or exists (select 1 from parent_telegram_link l join students st on st.id = l.student_id"
                    + " where l.chat_id = d.chat_id and (lower(coalesce(st.guardian_name, '')) like :q"
                    + (digits.length() >= 3 ? " or regexp_replace(coalesce(st.guardian_phone, ''), '\\D', '', 'g') like :digits" : "")
                    + ")))");
            p.addValue("q", like);
            if (digits.length() >= 3) p.addValue("digits", "%" + digits + "%");
        }
        return w.toString();
    }

    private static JournalRow row(java.sql.ResultSet rs) throws java.sql.SQLException {
        String tg = rs.getString("tg_first");
        if (rs.getString("tg_user") != null) tg = (tg == null ? "" : tg + " ") + "@" + rs.getString("tg_user");
        String category = rs.getString("name_uz") == null ? null
                : (rs.getString("emoji") == null ? "" : rs.getString("emoji") + " ") + rs.getString("name_uz");
        return new JournalRow(rs.getLong("id"), rs.getTimestamp("downloaded_at").toLocalDateTime(), rs.getLong("chat_id"),
                rs.getString("guardian_name") != null ? rs.getString("guardian_name") : tg,
                rs.getString("guardian_phone"), tg, rs.getString("children"), rs.getString("classes"),
                rs.getLong("book_id"), rs.getString("title"), rs.getString("author"), category, rs.getString("source"));
    }

    static String sourceLabel(String source) {
        if (source == null) return "";
        return switch (source) {
            case "SEARCH" -> "Qidiruv";
            case "CATEGORY" -> "Bo'lim";
            case "RECOMMENDATION" -> "Tavsiya";
            case "TOP" -> "Eng ko'p o'qilgan";
            case "NEW" -> "Yangi kitoblar";
            case "SAVED" -> "Saqlanganlar";
            default -> source;
        };
    }

    // ------------------------------------------------------------------ one parent

    public record ParentChild(String name, String className) {
    }

    public record ParentDownload(LocalDateTime downloadedAt, Long bookId, String title, String author, String category,
                                 String source) {
    }

    public record ParentDetail(Long chatId, String name, String phone, String telegramName, List<ParentChild> children,
                               List<ParentDownload> downloads, List<CategoryCount> favoriteCategories, long savedBooks) {
    }

    @Transactional(readOnly = true)
    public ParentDetail parent(Long schoolId, Long chatId) {
        access.requireSchool(schoolId);
        MapSqlParameterSource p = new MapSqlParameterSource("s", schoolId).addValue("chat", chatId);
        List<ParentChild> children = jdbc.query("""
                select st.first_name || ' ' || st.last_name name, sc.grade_number || '-' || sc.section_letter cls
                  from parent_telegram_link l join students st on st.id = l.student_id
                  join school_class sc on sc.id = st.class_id join academic_year ay on ay.id = sc.academic_year_id
                 where l.chat_id = :chat and l.active = true and ay.school_id = :s
                 order by sc.grade_number, st.first_name
                """, p, (rs, i) -> new ParentChild(rs.getString("name"), rs.getString("cls")));
        List<ParentDownload> downloads = jdbc.query("""
                select d.downloaded_at, b.id, b.title, b.author, c.emoji, c.name_uz, d.source
                  from book_download d join book b on b.id = d.book_id left join book_category c on c.id = b.category_id
                 where d.school_id = :s and d.chat_id = :chat order by d.downloaded_at desc limit 200
                """, p, (rs, i) -> new ParentDownload(rs.getTimestamp("downloaded_at").toLocalDateTime(), rs.getLong("id"),
                rs.getString("title"), rs.getString("author"),
                rs.getString("name_uz") == null ? null : (rs.getString("emoji") == null ? "" : rs.getString("emoji") + " ")
                        + rs.getString("name_uz"), rs.getString("source")));
        if (children.isEmpty() && downloads.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,
                    "Ota-ona topilmadi");
        }
        List<CategoryCount> favourite = jdbc.query("""
                select c.id, c.emoji, c.name_uz, count(*) n from book_download d join book b on b.id = d.book_id
                  join book_category c on c.id = b.category_id
                 where d.school_id = :s and d.chat_id = :chat group by c.id, c.emoji, c.name_uz order by n desc limit 5
                """, p, (rs, i) -> new CategoryCount(rs.getLong("id"),
                (rs.getString("emoji") == null ? "" : rs.getString("emoji") + " ") + rs.getString("name_uz"), rs.getLong("n")));
        Map<String, Object> who = jdbc.queryForMap("""
                select (select st.guardian_name from parent_telegram_link l join students st on st.id = l.student_id
                         where l.chat_id = :chat order by l.active desc, l.id limit 1) guardian_name,
                       (select st.guardian_phone from parent_telegram_link l join students st on st.id = l.student_id
                         where l.chat_id = :chat order by l.active desc, l.id limit 1) guardian_phone,
                       (select first_name from parent_session where chat_id = :chat) tg_first,
                       (select username from parent_session where chat_id = :chat) tg_user,
                       (select count(*) from book_favorite f join book b on b.id = f.book_id
                         where f.chat_id = :chat and b.school_id = :s) saved
                """, p);
        String tg = (String) who.get("tg_first");
        if (who.get("tg_user") != null) tg = (tg == null ? "" : tg + " ") + "@" + who.get("tg_user");
        String name = who.get("guardian_name") != null ? (String) who.get("guardian_name") : tg;
        return new ParentDetail(chatId, name, (String) who.get("guardian_phone"), tg, children, downloads, favourite,
                num(who.get("saved")));
    }

    private static long num(Object o) {
        return o instanceof Number n ? n.longValue() : 0L;
    }
}
