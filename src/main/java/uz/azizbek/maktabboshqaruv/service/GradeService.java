package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.GradeRequestDto;
import uz.azizbek.maktabboshqaruv.dto.GradeResponseDto;
import uz.azizbek.maktabboshqaruv.dto.GradebookResponseDto;
import uz.azizbek.maktabboshqaruv.entity.Grade;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.entity.Subject;
import uz.azizbek.maktabboshqaruv.event.GradeSavedEvent;
import org.springframework.context.ApplicationEventPublisher;
import uz.azizbek.maktabboshqaruv.repository.GradeRepository;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import uz.azizbek.maktabboshqaruv.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class GradeService {

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private java.time.Clock clock;

    public static final String SUNDAY_MESSAGE = "Uzr, bu kun yakshanba — maktab ishlamaydi. Baho qo'yib bo'lmaydi.";

    @Autowired
    private uz.azizbek.maktabboshqaruv.repository.UserRepository userRepository;

    /** Grades only on school days that have already come: never on a Sunday, never ahead of today (409). */
    void validateGradeDate(LocalDate date) {
        if (date == null) return;
        if (date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            throw new IllegalStateException(SUNDAY_MESSAGE);
        }
        if (date.isAfter(LocalDate.now(clock))) {
            throw new IllegalStateException("Kelajak sanaga baho qo'yilmaydi");
        }
    }

    public Page<GradeResponseDto> getAllGrades(Long schoolId, Pageable pageable) {
        return gradeRepository.findBySchoolId(schoolId, pageable).map(this::toResponseDto);
    }

    public GradebookResponseDto getGradebook(Long schoolClassId, Long subjectId, LocalDate from, LocalDate to) {
        List<Student> students = studentRepository.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(schoolClassId);
        List<Grade> grades = gradeRepository.findForGradebook(schoolClassId, subjectId, from, to);

        Map<Long, List<Grade>> byStudent = grades.stream()
                .collect(Collectors.groupingBy(g -> g.getStudent().getId()));

        GradebookResponseDto result = new GradebookResponseDto();
        result.setStudents(students.stream().map(s -> {
            GradebookResponseDto.StudentRow row = new GradebookResponseDto.StudentRow();
            row.setStudentId(s.getId());
            row.setStudentName(s.getFirstName() + " " + s.getLastName());
            List<Grade> studentGrades = byStudent.getOrDefault(s.getId(), List.of());
            double avg = studentGrades.stream().mapToInt(Grade::getScore).average().orElse(0);
            row.setAverage(studentGrades.isEmpty() ? null : Math.round(avg * 100) / 100.0);
            return row;
        }).collect(Collectors.toList()));
        result.setGrades(grades.stream().map(this::toResponseDto).collect(Collectors.toList()));
        return result;
    }

    @Transactional
    public GradeResponseDto createGrade(GradeRequestDto request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new IllegalStateException("Bunday o'quvchi mavjud emas"));
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new IllegalStateException("Bunday fan mavjud emas"));

        validateSameSchool(student, subject);
        requireActive(subject);
        validateGradeDate(request.getGradeDate());

        Grade grade = new Grade();
        grade.setStudent(student);
        grade.setSubject(subject);
        grade.setGradeDate(request.getGradeDate());
        grade.setScore(request.getScore());
        grade.setType(request.getType());
        grade.setComment(request.getComment());
        grade.setCreatedBy(currentAuthor());

        Grade saved = gradeRepository.save(grade);
        activityLogService.record(
                student.getSchoolClass().getAcademicYear().getSchool(),
                "grade",
                student.getFirstName() + " " + student.getLastName() + "ga " + subject.getName()
                        + " fanidan " + request.getScore() + " baho qo'yildi");
        eventPublisher.publishEvent(new GradeSavedEvent(saved.getId(), true));
        return toResponseDto(saved);
    }

    @Transactional
    public GradeResponseDto updateGrade(Long id, GradeRequestDto request) {
        Grade grade = gradeRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday baho yozuvi topilmadi: " + id));

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new IllegalStateException("Bunday o'quvchi mavjud emas"));
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new IllegalStateException("Bunday fan mavjud emas"));

        validateSameSchool(student, subject);
        // an old grade of a now-inactive subject can still be corrected; moving a grade to it cannot
        if (!Objects.equals(grade.getSubject().getId(), subject.getId())) {
            requireActive(subject);
        }
        if (!Objects.equals(grade.getGradeDate(), request.getGradeDate())) {
            validateGradeDate(request.getGradeDate());
        }

        // Only a change parents care about re-notifies — a comment edit does not.
        boolean meaningfulChange = !Objects.equals(grade.getScore(), request.getScore())
                || !Objects.equals(grade.getSubject().getId(), subject.getId())
                || grade.getType() != request.getType()
                || !Objects.equals(grade.getStudent().getId(), student.getId());

        grade.setStudent(student);
        grade.setSubject(subject);
        grade.setGradeDate(request.getGradeDate());
        grade.setScore(request.getScore());
        grade.setType(request.getType());
        grade.setComment(request.getComment());

        Grade updated = gradeRepository.save(grade);
        if (meaningfulChange) {
            eventPublisher.publishEvent(new GradeSavedEvent(updated.getId(), false));
        }
        return toResponseDto(updated);
    }

    @Transactional
    public void deleteGrade(Long id) {
        if (!gradeRepository.existsById(id)) {
            throw new IllegalStateException("Bunday baho yozuvi topilmadi: " + id);
        }
        gradeRepository.deleteById(id);
    }

    /** "Karimova D." for a staff account linked to an employee, otherwise the username. */
    private String currentAuthor() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        return userRepository.findByUsername(auth.getName())
                .map(u -> u.getEmployee() == null ? u.getUsername()
                        : u.getEmployee().getLastName() + " " + u.getEmployee().getFirstName().charAt(0) + ".")
                .orElse(auth.getName());
    }

    private static void requireActive(Subject subject) {
        if (!subject.isActive()) {
            throw new IllegalStateException("«" + subject.getName() + "» fani nofaol — yangi baho qo'yib bo'lmaydi");
        }
    }

    private void validateSameSchool(Student student, Subject subject) {
        Long studentSchoolId = student.getSchoolClass().getAcademicYear().getSchool().getId();
        if (subject.getSchool() == null || !studentSchoolId.equals(subject.getSchool().getId())) {
            throw new IllegalStateException("O'quvchi va fan bitta maktabga tegishli bo'lishi kerak");
        }
    }

    private GradeResponseDto toResponseDto(Grade grade) {
        GradeResponseDto dto = new GradeResponseDto();
        dto.setId(grade.getId());
        dto.setStudentId(grade.getStudent().getId());
        dto.setStudentName(grade.getStudent().getFirstName() + " " + grade.getStudent().getLastName());
        dto.setSubjectId(grade.getSubject().getId());
        dto.setSubjectName(grade.getSubject().getName());
        dto.setSubjectActive(grade.getSubject().isActive());
        dto.setGradeDate(grade.getGradeDate());
        dto.setScore(grade.getScore());
        dto.setType(grade.getType());
        dto.setComment(grade.getComment());
        dto.setCreatedBy(grade.getCreatedBy());
        dto.setCreatedAt(grade.getCreatedDate());
        return dto;
    }
}
