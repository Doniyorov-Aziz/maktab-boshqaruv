package uz.azizbek.maktabboshqaruv.service;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.dto.GradeRequestDto;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.*;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/** Imtihonlar va nazorat ishlari jadvali, and the results (EXAM grades through {@link GradeService}). */
@Service
public class ExamService {

    public record Request(@NotNull(message = "Sinf tanlanishi shart") Long schoolClassId,
                          @NotNull(message = "Fan tanlanishi shart") Long subjectId,
                          Long employeeId,
                          @NotNull(message = "Turi tanlanishi shart") Exam.Kind kind,
                          @Size(max = 200, message = "Nomi 200 belgidan oshmasin") String title,
                          @NotNull(message = "Sana kiritilishi shart") LocalDate examDate,
                          LocalTime startTime,
                          @Size(max = 60, message = "Joy 60 belgidan oshmasin") String place,
                          @Size(max = 2000, message = "Izoh 2000 belgidan oshmasin") String description,
                          Boolean notifyParents) {
    }

    public record Response(Long id, Long schoolClassId, String className, Long subjectId, String subjectName,
                           String subjectColor, Long employeeId, String employeeName, Exam.Kind kind, String title,
                           LocalDate examDate, LocalTime startTime, String place, String description,
                           boolean notifyParents, long resultCount, Double resultAverage, List<String> sameDay,
                           String createdBy) {
    }

    /** One pupil on the results page: the stored score (null = none yet). */
    public record ResultRow(Long studentId, String name, Integer score, Long gradeId, String comment) {
    }

    public record Results(Response exam, List<ResultRow> rows) {
    }

    public record ResultInput(Long studentId, Integer score, String comment) {
    }

    private final ExamRepository exams;
    private final SchoolClassRepository classes;
    private final SubjectRepository subjects;
    private final EmployeeRepository employees;
    private final StudentRepository students;
    private final GradeRepository grades;
    private final GradeService gradeService;
    private final SchoolAccessService access;
    private final Clock clock;

    public ExamService(ExamRepository exams, SchoolClassRepository classes, SubjectRepository subjects,
                       EmployeeRepository employees, StudentRepository students, GradeRepository grades,
                       GradeService gradeService, SchoolAccessService access, Clock clock) {
        this.exams = exams;
        this.classes = classes;
        this.subjects = subjects;
        this.employees = employees;
        this.students = students;
        this.grades = grades;
        this.gradeService = gradeService;
        this.access = access;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<Response> list(Long schoolId, Long classId, Long subjectId, LocalDate from, LocalDate to, Pageable pageable) {
        access.requireSchool(schoolId);
        return exams.search(schoolId, classId, subjectId, from, to, pageable).map(x -> toDto(x, false));
    }

    @Transactional(readOnly = true)
    public Response get(Long id) {
        return toDto(find(id), true);
    }

    @Transactional
    public Response create(Request r) {
        SchoolAccessService.Caller caller = access.caller();
        Exam x = new Exam();
        apply(x, r, caller);
        x.setCreatedBy(caller.username());
        x.setCreatedAt(LocalDateTime.now(clock));
        exams.save(x);
        return toDto(x, true);
    }

    @Transactional
    public Response update(Long id, Request r) {
        Exam x = find(id);
        if (!x.getSchoolClass().getId().equals(r.schoolClassId()) && grades.countByExamId(id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Natijalar kiritilgan — sinfni o'zgartirib bo'lmaydi");
        }
        apply(x, r, access.caller());
        // the results follow the exam's subject and date
        for (Grade g : grades.findByExamId(id)) {
            g.setSubject(x.getSubject());
            if (!x.getExamDate().isAfter(LocalDate.now(clock))) g.setGradeDate(x.getExamDate());
        }
        return toDto(x, true);
    }

    /** Removing an exam keeps its results as ordinary EXAM grades of the journal. */
    @Transactional
    public void delete(Long id) {
        Exam x = find(id);
        for (Grade g : grades.findByExamId(id)) g.setExamId(null);
        exams.delete(x);
    }

    @Transactional(readOnly = true)
    public Results results(Long id) {
        Exam x = find(id);
        Map<Long, Grade> byStudent = new HashMap<>();
        for (Grade g : grades.findByExamId(id)) byStudent.put(g.getStudent().getId(), g);
        List<ResultRow> rows = new ArrayList<>();
        for (Student s : students.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(x.getSchoolClass().getId())) {
            Grade g = byStudent.get(s.getId());
            rows.add(new ResultRow(s.getId(), (s.getLastName() + " " + s.getFirstName()).strip(),
                    g == null ? null : g.getScore(), g == null ? null : g.getId(), g == null ? null : g.getComment()));
        }
        return new Results(toDto(x, true), rows);
    }

    /**
     * Saves the results: a score creates or changes the pupil's EXAM grade (parents are told as for any grade),
     * an empty score removes it. Only once the exam day has come.
     */
    @Transactional
    public Results saveResults(Long id, List<ResultInput> inputs) {
        Exam x = find(id);
        if (x.getExamDate().isAfter(LocalDate.now(clock))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Natijalarni imtihon kunidan keyin kiritish mumkin");
        }
        if (x.getExamDate().getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, GradeService.SUNDAY_MESSAGE);
        }
        Map<Long, Grade> byStudent = new HashMap<>();
        for (Grade g : grades.findByExamId(id)) byStudent.put(g.getStudent().getId(), g);
        Set<Long> pupils = new HashSet<>();
        for (Student s : students.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(x.getSchoolClass().getId())) pupils.add(s.getId());
        String label = x.getTitle() != null && !x.getTitle().isBlank() ? x.getTitle() : kindLabel(x.getKind());
        for (ResultInput in : inputs) {
            if (in.studentId() == null || !pupils.contains(in.studentId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O'quvchi bu sinfda emas");
            }
            if (in.score() != null && (in.score() < 1 || in.score() > 5)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Baho 1 dan 5 gacha bo'lsin");
            }
            Grade existing = byStudent.get(in.studentId());
            String comment = in.comment() == null || in.comment().isBlank() ? label : in.comment().strip();
            if (in.score() == null) {
                if (existing != null) gradeService.deleteGrade(existing.getId());
                continue;
            }
            GradeRequestDto dto = new GradeRequestDto();
            dto.setStudentId(in.studentId());
            dto.setSubjectId(x.getSubject().getId());
            dto.setGradeDate(x.getExamDate());
            dto.setScore(in.score());
            dto.setType(GradeType.EXAM);
            dto.setComment(comment);
            if (existing == null) {
                Long gradeId = gradeService.createGrade(dto).getId();
                grades.findById(gradeId).ifPresent(g -> g.setExamId(id));
            } else if (!Objects.equals(existing.getScore(), in.score()) || !Objects.equals(existing.getComment(), comment)) {
                gradeService.updateGrade(existing.getId(), dto);
            }
        }
        return results(id);
    }

    /** The class's exams in a period (the parents' bot and Mini App). */
    @Transactional(readOnly = true)
    public List<Response> ofClass(Long classId, LocalDate from, LocalDate to) {
        return exams.ofClass(classId, from, to).stream().map(x -> toDto(x, false)).toList();
    }

    public static String kindLabel(Exam.Kind kind) {
        return switch (kind) {
            case CONTROL -> "Nazorat ishi";
            case EXAM -> "Imtihon";
            case TEST -> "Test";
            case WRITTEN -> "Yozma ish";
            case ORAL -> "Og'zaki so'rov";
        };
    }

    // ------------------------------------------------------------------ helpers

    private void apply(Exam x, Request r, SchoolAccessService.Caller caller) {
        SchoolClass c = classes.findById(r.schoolClassId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday sinf topilmadi"));
        Subject s = subjects.findById(r.subjectId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday fan topilmadi"));
        School school = c.getAcademicYear().getSchool();
        if (s.getSchool() != null && !s.getSchool().getId().equals(school.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fan boshqa maktabniki");
        }
        if (r.examDate().getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yakshanba kuni imtihon belgilab bo'lmaydi");
        }
        Long employeeId = r.employeeId() != null ? r.employeeId() : x.getId() == null ? caller.employeeId() : null;
        Employee e = employeeId == null ? null : employees.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday xodim topilmadi"));
        if (e != null && e.getSchool() != null && !e.getSchool().getId().equals(school.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O'qituvchi boshqa maktabniki");
        }
        x.setSchool(school);
        x.setSchoolClass(c);
        x.setSubject(s);
        x.setEmployee(e);
        x.setKind(r.kind());
        x.setTitle(blankToNull(r.title()));
        x.setExamDate(r.examDate());
        x.setStartTime(r.startTime());
        x.setPlace(blankToNull(r.place()));
        x.setDescription(blankToNull(r.description()));
        x.setNotifyParents(!Boolean.FALSE.equals(r.notifyParents()));
    }

    private Exam find(Long id) {
        return exams.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday imtihon topilmadi: " + id));
    }

    private Response toDto(Exam x, boolean details) {
        long count = 0;
        Double avg = null;
        List<String> sameDay = List.of();
        if (details) {
            List<Grade> res = grades.findByExamId(x.getId());
            count = res.size();
            avg = res.isEmpty() ? null : Math.round(res.stream().mapToInt(Grade::getScore).average().orElse(0) * 100) / 100.0;
            sameDay = exams.sameDay(x.getSchoolClass().getId(), x.getExamDate(), x.getId() == null ? -1L : x.getId()).stream()
                    .map(o -> o.getSubject().getName() + " — " + kindLabel(o.getKind())).toList();
        }
        Employee e = x.getEmployee();
        return new Response(x.getId(), x.getSchoolClass().getId(),
                x.getSchoolClass().getGradeNumber() + "-" + x.getSchoolClass().getSectionLetter(),
                x.getSubject().getId(), x.getSubject().getName(), x.getSubject().getColor(),
                e == null ? null : e.getId(), e == null ? null : (e.getLastName() + " " + e.getFirstName()).strip(),
                x.getKind(), x.getTitle(), x.getExamDate(), x.getStartTime(), x.getPlace(), x.getDescription(),
                x.isNotifyParents(), count, avg, sameDay, x.getCreatedBy());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
