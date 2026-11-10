package uz.azizbek.maktabboshqaruv.service;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.bot.BotI18n;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.*;
import uz.azizbek.maktabboshqaruv.telegram.MessageFormatter;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** O'quvchilar yutuqlari: olympiads, competitions, certificates — the list, the diploma scan, the parents' message. */
@Service
public class AchievementService {

    public record Request(@NotNull(message = "O'quvchi tanlanishi shart") Long studentId,
                          Long subjectId,
                          @NotBlank(message = "Nomi kiritilishi shart") @Size(max = 200, message = "Nomi 200 belgidan oshmasin") String title,
                          @NotNull(message = "Turi tanlanishi shart") Achievement.Category category,
                          @NotNull(message = "Darajasi tanlanishi shart") Achievement.Level level,
                          @Min(value = 1, message = "O'rin 1 dan 3 gacha") @Max(value = 3, message = "O'rin 1 dan 3 gacha") Integer place,
                          @NotNull(message = "Sana kiritilishi shart") LocalDate achievedOn,
                          @Size(max = 1000, message = "Izoh 1000 belgidan oshmasin") String description,
                          Boolean notifyParents) {
    }

    public record Response(Long id, Long studentId, String studentName, Long schoolClassId, String className,
                           Long subjectId, String subjectName, String title, Achievement.Category category,
                           Achievement.Level level, Integer place, LocalDate achievedOn, String description,
                           String fileName, Long fileSize, String createdBy, LocalDateTime createdAt) {
    }

    public record Count(String key, long count) {
    }

    public record TopStudent(Long studentId, String name, String className, long count, long prizes) {
    }

    public record Stats(long total, long prizes, List<Count> byLevel, List<Count> byCategory, List<TopStudent> top) {
    }

    private final AchievementRepository achievements;
    private final StudentRepository students;
    private final SubjectRepository subjects;
    private final NotificationService notifications;
    private final SchoolAccessService access;
    private final ModuleFiles files;
    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    public AchievementService(AchievementRepository achievements, StudentRepository students, SubjectRepository subjects,
                              NotificationService notifications, SchoolAccessService access, ModuleFiles files,
                              NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.achievements = achievements;
        this.students = students;
        this.subjects = subjects;
        this.notifications = notifications;
        this.access = access;
        this.files = files;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<Response> list(Long schoolId, Long classId, Long studentId, Achievement.Category category,
                               Achievement.Level level, LocalDate from, LocalDate to, Pageable pageable) {
        access.requireSchool(schoolId);
        return achievements.search(schoolId, classId, studentId, category, level, from, to, pageable).map(AchievementService::toDto);
    }

    @Transactional(readOnly = true)
    public List<Response> ofStudent(Long studentId) {
        Student st = student(studentId);
        return achievements.ofStudent(st.getId()).stream().map(AchievementService::toDto).toList();
    }

    @Transactional
    public Response create(Request r) {
        Achievement a = new Achievement();
        apply(a, r);
        a.setCreatedBy(access.caller().username());
        a.setCreatedAt(LocalDateTime.now(clock));
        achievements.save(a);
        if (Boolean.TRUE.equals(r.notifyParents())) notifyParents(a);
        return toDto(a);
    }

    @Transactional
    public Response update(Long id, Request r) {
        Achievement a = find(id);
        apply(a, r);
        return toDto(a);
    }

    @Transactional
    public void delete(Long id) {
        Achievement a = find(id);
        String path = a.getFilePath();
        achievements.delete(a);
        files.delete(path);
    }

    @Transactional
    public Response attach(Long id, MultipartFile file) {
        Achievement a = find(id);
        ModuleFiles.Stored stored = files.save("achievements", a.getSchool().getId(), file);
        files.delete(a.getFilePath());
        a.setFilePath(stored.path());
        a.setFileName(stored.name());
        a.setFileSize(stored.size());
        return toDto(a);
    }

    @Transactional
    public Response detach(Long id) {
        Achievement a = find(id);
        files.delete(a.getFilePath());
        a.setFilePath(null);
        a.setFileName(null);
        a.setFileSize(null);
        return toDto(a);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> download(Long id) {
        Achievement a = find(id);
        return files.download(a.getFilePath(), a.getFileName());
    }

    /** "🏆 Tabriklaymiz!" to the pupil's parents. */
    @Transactional
    public int notifyParents(Long id) {
        return notifyParents(find(id));
    }

    int notifyParents(Achievement a) {
        Student st = a.getStudent();
        String name = NotificationService.fullName(st);
        String title = a.getTitle();
        Integer place = a.getPlace();
        String schoolName = a.getSchool().getName();
        int queued = 0;
        for (NotificationService.Recipient r : notifications.recipientsOf(st)) {
            queued += notifications.enqueueDirect(a.getSchool(), st, r.chatId(), NotificationType.ACHIEVEMENT, a.getId(),
                    a.getAchievedOn(), lang -> BotI18n.get().t(lang, "notif.achievement",
                            "name", MessageFormatter.escape(name), "title", MessageFormatter.escape(title),
                            "level", BotI18n.get().t(lang, "ach.level." + a.getLevel().name()),
                            "place", place == null ? "" : " · " + BotI18n.get().t(lang, "ach.place", "n", place))
                            + MessageFormatter.footer(lang, schoolName),
                    null);
        }
        return queued;
    }

    /** The school's achievements in a period: by level, by kind, and the pupils with the most. */
    @Transactional(readOnly = true)
    public Stats stats(Long schoolId, LocalDate from, LocalDate to) {
        access.requireSchool(schoolId);
        MapSqlParameterSource p = new MapSqlParameterSource("s", schoolId).addValue("f", from).addValue("t", to);
        String where = " where a.school_id = :s and (cast(:f as date) is null or a.achieved_on >= :f)"
                + " and (cast(:t as date) is null or a.achieved_on <= :t)";
        List<Count> byLevel = jdbc.query("select a.level, count(*) from achievement a" + where + " group by a.level order by 2 desc",
                p, (rs, i) -> new Count(rs.getString(1), rs.getLong(2)));
        List<Count> byCategory = jdbc.query("select a.category, count(*) from achievement a" + where + " group by a.category order by 2 desc",
                p, (rs, i) -> new Count(rs.getString(1), rs.getLong(2)));
        List<TopStudent> top = jdbc.query("""
                select s.id, s.last_name || ' ' || s.first_name, c.grade_number || '-' || c.section_letter, count(*),
                       count(*) filter (where a.place is not null)
                  from achievement a join students s on s.id = a.student_id join school_class c on c.id = s.class_id"""
                        + where + " group by s.id, s.last_name, s.first_name, c.grade_number, c.section_letter"
                        + " order by 5 desc, 4 desc, 2 limit 10",
                p, (rs, i) -> new TopStudent(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getLong(4), rs.getLong(5)));
        long total = byLevel.stream().mapToLong(Count::count).sum();
        Long prizes = jdbc.queryForObject("select count(*) from achievement a" + where + " and a.place is not null", p, Long.class);
        return new Stats(total, prizes == null ? 0 : prizes, byLevel, byCategory, top);
    }

    public static String levelLabel(Achievement.Level level) {
        return switch (level) {
            case SCHOOL -> "Maktab";
            case DISTRICT -> "Tuman";
            case REGION -> "Viloyat";
            case NATIONAL -> "Respublika";
            case INTERNATIONAL -> "Xalqaro";
        };
    }

    // ------------------------------------------------------------------ helpers

    private void apply(Achievement a, Request r) {
        Student st = student(r.studentId());
        School school = NotificationService.schoolOf(st);
        Subject sub = null;
        if (r.subjectId() != null) {
            sub = subjects.findById(r.subjectId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday fan topilmadi"));
            if (sub.getSchool() != null && !sub.getSchool().getId().equals(school.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fan boshqa maktabniki");
            }
        }
        if (r.achievedOn().isAfter(LocalDate.now(clock))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sana kelajakda bo'lmasin");
        }
        a.setSchool(school);
        a.setStudent(st);
        a.setSubject(sub);
        a.setTitle(r.title().strip());
        a.setCategory(r.category());
        a.setLevel(r.level());
        a.setPlace(r.place());
        a.setAchievedOn(r.achievedOn());
        a.setDescription(r.description() == null || r.description().isBlank() ? null : r.description().strip());
    }

    private Student student(Long id) {
        return students.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday o'quvchi topilmadi"));
    }

    private Achievement find(Long id) {
        return achievements.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday yutuq topilmadi"));
    }

    static Response toDto(Achievement a) {
        Student st = a.getStudent();
        Subject sub = a.getSubject();
        return new Response(a.getId(), st.getId(), NotificationService.fullName(st), st.getSchoolClass().getId(),
                NotificationService.className(st), sub == null ? null : sub.getId(), sub == null ? null : sub.getName(),
                a.getTitle(), a.getCategory(), a.getLevel(), a.getPlace(), a.getAchievedOn(), a.getDescription(),
                a.getFileName(), a.getFileSize(), a.getCreatedBy(), a.getCreatedAt());
    }
}
