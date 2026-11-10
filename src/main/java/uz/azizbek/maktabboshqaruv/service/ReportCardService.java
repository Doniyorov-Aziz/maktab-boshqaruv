package uz.azizbek.maktabboshqaruv.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;

/**
 * Chorak va yillik tabel. The quarter grade a pupil gets in a subject is suggested from the grades of that
 * quarter (current, homework and exam grades; 4.5 → 5), the teacher or administrator confirms it
 * ("Chorak baholarini qo'yish") or changes a single one, and it is stored as a QUARTERLY grade dated
 * on the quarter's last day (today, if the quarter is not over). The year grade is the average of the
 * quarter grades. Rows are read with a few aggregate queries per class, not one per pupil.
 */
@Service
public class ReportCardService {

    /** Fewer grades than this in a quarter: the suggestion is shown with a warning. */
    public static final int FEW_GRADES = 3;

    public record Term(Long id, String name, LocalDate from, LocalDate to, int number) {
    }

    public record SubjectCol(Long id, String name, String color) {
    }

    /** One pupil × subject: average and count of the quarter's grades, the suggestion, the stored grade. */
    public record Cell(Long subjectId, Double average, int count, Integer suggested, Integer quarterly, Long quarterlyId) {
    }

    public record Row(Long studentId, String name, List<Cell> cells, Double average, int absent, int late,
                      int excused, int lessons, Integer attendancePercent) {
    }

    public record Card(Long classId, String className, String schoolName, String yearTitle, Term term,
                       List<Term> terms, List<SubjectCol> subjects, List<Row> rows, int missing, int stored) {
    }

    /** The year view: one row per subject, the quarter grades in order and the year grade. */
    public record YearSubject(String subject, List<Integer> quarters, Integer year) {
    }

    public record YearAttendance(int absent, int late, int excused, int lessons) {
    }

    public record StudentYear(Long studentId, String name, String className, String schoolName, String yearTitle,
                              List<Term> terms, List<YearSubject> subjects, List<YearAttendance> attendance,
                              Integer upToQuarter) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final SchoolClassRepository classes;
    private final StudentRepository students;
    private final SubjectRepository subjects;
    private final GradeRepository grades;
    private final SchoolAccessService access;
    private final Clock clock;

    public ReportCardService(NamedParameterJdbcTemplate jdbc, SchoolClassRepository classes, StudentRepository students,
                             SubjectRepository subjects, GradeRepository grades, SchoolAccessService access, Clock clock) {
        this.jdbc = jdbc;
        this.classes = classes;
        this.students = students;
        this.subjects = subjects;
        this.grades = grades;
        this.access = access;
        this.clock = clock;
    }

    /** The quarters of the class's school year (O'quv yillari), numbered in date order. */
    @Transactional(readOnly = true)
    public List<Term> terms(Long classId) {
        SchoolClass c = findClass(classId);
        return termsOfYear(c.getAcademicYear().getId());
    }

    List<Term> termsOfYear(Long yearId) {
        List<Term> out = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select id, name, start_date, end_date from academic_term
                 where academic_year_id = :y and kind = 'QUARTER' order by start_date""", Map.of("y", yearId))) {
            out.add(new Term(((Number) r.get("id")).longValue(), (String) r.get("name"), date(r.get("start_date")),
                    date(r.get("end_date")), out.size() + 1));
        }
        return out;
    }

    /** The class's tabel for one quarter (termId), or for the quarter "now" when termId is null. */
    @Transactional(readOnly = true)
    public Card card(Long classId, Long termId) {
        SchoolClass c = findClass(classId);
        List<Term> terms = termsOfYear(c.getAcademicYear().getId());
        if (terms.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Bu o'quv yili uchun choraklar kiritilmagan — «O'quv yillari» sahifasida choraklarni belgilang");
        }
        Term term = termId == null ? currentTerm(terms) : terms.stream().filter(t -> t.id().equals(termId)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bu chorak sinfning o'quv yiliga tegishli emas"));

        List<Student> pupils = students.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(classId);
        MapSqlParameterSource p = new MapSqlParameterSource("c", classId).addValue("f", term.from()).addValue("t", term.to());

        // average and count of the quarter's ordinary grades
        Map<String, double[]> stats = new HashMap<>();
        jdbc.query("""
                select g.student_id, g.subject_id, avg(g.score) a, count(*) n from grade g join students s on s.id = g.student_id
                 where s.class_id = :c and g.type <> 'QUARTERLY' and g.grade_date between :f and :t
                 group by g.student_id, g.subject_id""", p, rs -> {
            stats.put(rs.getLong(1) + ":" + rs.getLong(2), new double[]{rs.getDouble(3), rs.getLong(4)});
        });
        // stored quarter grades (the latest one if several)
        Map<String, long[]> stored = new HashMap<>();
        jdbc.query("""
                select g.student_id, g.subject_id, g.score, g.id from grade g join students s on s.id = g.student_id
                 where s.class_id = :c and g.type = 'QUARTERLY' and g.grade_date between :f and :t
                 order by g.grade_date, g.id""", p, rs -> {
            stored.put(rs.getLong(1) + ":" + rs.getLong(2), new long[]{rs.getInt(3), rs.getLong(4)});
        });
        // attendance of the quarter
        Map<Long, int[]> att = new HashMap<>();
        jdbc.query("""
                select a.student_id, a.status, count(*) from attendance a join students s on s.id = a.student_id
                 where s.class_id = :c and a.record_date between :f and :t group by a.student_id, a.status""", p, rs -> {
            int[] v = att.computeIfAbsent(rs.getLong(1), k -> new int[4]);
            int n = rs.getInt(3);
            switch (rs.getString(2)) {
                case "ABSENT" -> v[0] += n;
                case "LATE" -> v[1] += n;
                case "EXCUSED" -> v[2] += n;
                default -> { }
            }
            v[3] += n;
        });

        List<SubjectCol> cols = subjectsOf(classId, term.from(), term.to());
        List<Row> rows = new ArrayList<>();
        int missing = 0;
        int storedCount = 0;
        for (Student s : pupils) {
            List<Cell> cells = new ArrayList<>();
            List<Integer> forAverage = new ArrayList<>();
            for (SubjectCol col : cols) {
                String key = s.getId() + ":" + col.id();
                double[] st = stats.get(key);
                long[] q = stored.get(key);
                Double avg = st == null ? null : round2(st[0]);
                Integer suggested = st == null ? null : suggest(st[0]);
                Integer quarterly = q == null ? null : (int) q[0];
                if (quarterly != null) storedCount++;
                else if (suggested != null) missing++;
                Integer shown = quarterly != null ? quarterly : suggested;
                if (shown != null) forAverage.add(shown);
                cells.add(new Cell(col.id(), avg, st == null ? 0 : (int) st[1], suggested, quarterly, q == null ? null : q[1]));
            }
            int[] a = att.getOrDefault(s.getId(), new int[4]);
            Integer percent = a[3] == 0 ? null : (int) Math.round(100.0 * (a[3] - a[0]) / a[3]);
            rows.add(new Row(s.getId(), name(s), cells, average(forAverage), a[0], a[1], a[2], a[3], percent));
        }
        School school = c.getAcademicYear().getSchool();
        return new Card(c.getId(), className(c), school.getName(), c.getAcademicYear().getTitle(), term, terms, cols, rows,
                missing, storedCount);
    }

    /**
     * "Chorak baholarini qo'yish": a QUARTERLY grade for every pupil × subject that has grades this quarter
     * and none stored yet ({@code overwrite} = the stored ones are replaced by the suggestion too).
     */
    @Transactional
    public int finalizeQuarter(Long classId, Long termId, boolean overwrite) {
        Card card = card(classId, termId);
        String by = access.caller().username();
        int saved = 0;
        for (Row row : card.rows()) {
            for (Cell cell : row.cells()) {
                if (cell.suggested() == null) continue;
                if (cell.quarterly() != null && !overwrite) continue;
                saved += store(row.studentId(), cell.subjectId(), card.term(), cell.suggested(), cell.average(), by) ? 1 : 0;
            }
        }
        grades.flush();
        return saved;
    }

    /** One cell: set (1..5) or remove (null) the pupil's quarter grade in a subject. */
    @Transactional
    public Card setCell(Long classId, Long termId, Long studentId, Long subjectId, Integer score) {
        if (score != null && (score < 1 || score > 5)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Baho 1 dan 5 gacha bo'lsin");
        }
        Card card = card(classId, termId);
        Student s = students.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday o'quvchi topilmadi"));
        if (!s.getSchoolClass().getId().equals(classId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O'quvchi bu sinfda emas");
        }
        Double avg = card.rows().stream().filter(r -> r.studentId().equals(studentId)).flatMap(r -> r.cells().stream())
                .filter(cl -> cl.subjectId().equals(subjectId)).map(Cell::average).filter(Objects::nonNull).findFirst().orElse(null);
        if (score == null) {
            removeStored(studentId, subjectId, card.term());
        } else {
            store(studentId, subjectId, card.term(), score, avg, access.caller().username());
        }
        // the card below is read with plain SQL: the change must be in the database first
        grades.flush();
        return card(classId, card.term().id());
    }

    /** A pupil's whole year: every quarter grade per subject, the year grade, attendance per quarter. */
    @Transactional(readOnly = true)
    public StudentYear studentYear(Long studentId, Long upToTermId) {
        Student s = students.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday o'quvchi topilmadi"));
        SchoolClass c = s.getSchoolClass();
        List<Term> terms = termsOfYear(c.getAcademicYear().getId());
        Integer upTo = upToTermId == null ? null : terms.stream().filter(t -> t.id().equals(upToTermId))
                .map(Term::number).findFirst().orElse(null);
        LocalDate from = terms.isEmpty() ? c.getAcademicYear().getStartDate() : terms.get(0).from();
        LocalDate to = terms.isEmpty() ? c.getAcademicYear().getEndDate() : terms.get(terms.size() - 1).to();

        Map<String, Integer> byNameQuarter = new HashMap<>();
        TreeSet<String> subjectNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        jdbc.query("""
                select sub.name, g.grade_date, g.score from grade g join subject sub on sub.id = g.subject_id
                 where g.student_id = :s and g.type = 'QUARTERLY' and g.grade_date between :f and :t
                 order by g.grade_date, g.id""",
                new MapSqlParameterSource("s", studentId).addValue("f", from).addValue("t", to), rs -> {
                    LocalDate d = rs.getDate(2).toLocalDate();
                    int n = quarterOf(terms, d);
                    if (n == 0) return;
                    subjectNames.add(rs.getString(1));
                    byNameQuarter.put(rs.getString(1) + "|" + n, rs.getInt(3));
                });
        for (SubjectCol col : subjectsOf(c.getId(), from, to)) subjectNames.add(col.name());

        List<YearSubject> rows = new ArrayList<>();
        for (String name : subjectNames) {
            List<Integer> qs = new ArrayList<>();
            List<Integer> present = new ArrayList<>();
            for (Term t : terms) {
                Integer v = upTo != null && t.number() > upTo ? null : byNameQuarter.get(name + "|" + t.number());
                qs.add(v);
                if (v != null) present.add(v);
            }
            // the year grade only once every quarter (up to the chosen one) is in
            boolean complete = !terms.isEmpty() && present.size() == (upTo == null ? terms.size() : upTo)
                    && (upTo == null || upTo == terms.size());
            rows.add(new YearSubject(name, qs, complete ? suggest(present.stream().mapToInt(i -> i).average().orElse(0)) : null));
        }

        List<YearAttendance> attendance = new ArrayList<>();
        for (Term t : terms) {
            if (upTo != null && t.number() > upTo) {
                attendance.add(null);
                continue;
            }
            int[] v = new int[4];
            jdbc.query("select status, count(*) from attendance where student_id = :s and record_date between :f and :t group by status",
                    new MapSqlParameterSource("s", studentId).addValue("f", t.from()).addValue("t", t.to()), rs -> {
                        int n = rs.getInt(2);
                        switch (rs.getString(1)) {
                            case "ABSENT" -> v[0] += n;
                            case "LATE" -> v[1] += n;
                            case "EXCUSED" -> v[2] += n;
                            default -> { }
                        }
                        v[3] += n;
                    });
            attendance.add(new YearAttendance(v[0], v[1], v[2], v[3]));
        }
        return new StudentYear(s.getId(), name(s), className(c), c.getAcademicYear().getSchool().getName(),
                c.getAcademicYear().getTitle(), terms, rows, attendance, upTo);
    }

    // ------------------------------------------------------------------ helpers

    /** 4.5 → 5, 4.49 → 4 (the usual school rounding). */
    public static int suggest(double average) {
        return Math.max(1, Math.min(5, BigDecimal.valueOf(average).setScale(0, RoundingMode.HALF_UP).intValue()));
    }

    private boolean store(Long studentId, Long subjectId, Term term, int score, Double average, String by) {
        List<Grade> existing = grades.findByStudentIdAndGradeDateBetweenOrderByGradeDateDescIdDesc(studentId, term.from(), term.to())
                .stream().filter(g -> g.getType() == GradeType.QUARTERLY && g.getSubject().getId().equals(subjectId)).toList();
        Grade g;
        if (existing.isEmpty()) {
            g = new Grade();
            g.setStudent(students.getReferenceById(studentId));
            g.setSubject(subjects.getReferenceById(subjectId));
            g.setType(GradeType.QUARTERLY);
            LocalDate today = LocalDate.now(clock);
            g.setGradeDate(term.to().isAfter(today) ? (today.isBefore(term.from()) ? term.from() : today) : term.to());
        } else {
            g = existing.get(0);
            for (int i = 1; i < existing.size(); i++) grades.delete(existing.get(i));
            if (Objects.equals(g.getScore(), score)) return false;
        }
        g.setScore(score);
        g.setCreatedBy(by);
        g.setSource(AttendanceSource.MANUAL);
        g.setComment(average == null ? term.name() + " bahosi" : term.name() + " bahosi (o'rtacha " + String.format(Locale.ROOT, "%.2f", average) + ")");
        grades.save(g);
        return true;
    }

    private void removeStored(Long studentId, Long subjectId, Term term) {
        grades.findByStudentIdAndGradeDateBetweenOrderByGradeDateDescIdDesc(studentId, term.from(), term.to()).stream()
                .filter(g -> g.getType() == GradeType.QUARTERLY && g.getSubject().getId().equals(subjectId))
                .forEach(grades::delete);
    }

    /** The class's subjects: the timetable's and any other subject with grades in the range, by name. */
    private List<SubjectCol> subjectsOf(Long classId, LocalDate from, LocalDate to) {
        return jdbc.query("""
                select distinct sub.id, sub.name, sub.color from subject sub
                 where sub.id in (select subject_id from lesson_slot where class_id = :c)
                    or sub.id in (select g.subject_id from grade g join students s on s.id = g.student_id
                                   where s.class_id = :c and g.grade_date between :f and :t)
                 order by sub.name""",
                new MapSqlParameterSource("c", classId).addValue("f", from).addValue("t", to),
                (rs, i) -> new SubjectCol(rs.getLong(1), rs.getString(2), rs.getString(3)));
    }

    private Term currentTerm(List<Term> terms) {
        LocalDate today = LocalDate.now(clock);
        Term last = terms.get(0);
        for (Term t : terms) if (!t.from().isAfter(today)) last = t;
        return last;
    }

    private static int quarterOf(List<Term> terms, LocalDate d) {
        for (Term t : terms) if (!d.isBefore(t.from()) && !d.isAfter(t.to())) return t.number();
        return 0;
    }

    private SchoolClass findClass(Long classId) {
        return classes.findById(classId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday sinf topilmadi"));
    }

    private static Double average(List<Integer> values) {
        return values.isEmpty() ? null : round2(values.stream().mapToInt(i -> i).average().orElse(0));
    }

    private static double round2(double v) {
        return Math.round(v * 100) / 100.0;
    }

    static String name(Student s) {
        return (s.getLastName() + " " + s.getFirstName()).strip();
    }

    static String className(SchoolClass c) {
        return c.getGradeNumber() + "-" + c.getSectionLetter();
    }

    private static LocalDate date(Object o) {
        return o == null ? null : o instanceof Date d ? d.toLocalDate() : (LocalDate) o;
    }
}
