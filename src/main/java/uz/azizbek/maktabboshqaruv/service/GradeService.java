package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.GradeRequestDto;
import uz.azizbek.maktabboshqaruv.dto.GradeResponseDto;
import uz.azizbek.maktabboshqaruv.dto.GradebookResponseDto;
import uz.azizbek.maktabboshqaruv.entity.Grade;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.entity.Subject;
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

        Grade grade = new Grade();
        grade.setStudent(student);
        grade.setSubject(subject);
        grade.setGradeDate(request.getGradeDate());
        grade.setScore(request.getScore());
        grade.setType(request.getType());
        grade.setComment(request.getComment());

        Grade saved = gradeRepository.save(grade);
        activityLogService.record(
                student.getSchoolClass().getAcademicYear().getSchool(),
                "grade",
                student.getFirstName() + " " + student.getLastName() + "ga " + subject.getName()
                        + " fanidan " + request.getScore() + " baho qo'yildi");
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

        grade.setStudent(student);
        grade.setSubject(subject);
        grade.setGradeDate(request.getGradeDate());
        grade.setScore(request.getScore());
        grade.setType(request.getType());
        grade.setComment(request.getComment());

        Grade updated = gradeRepository.save(grade);
        return toResponseDto(updated);
    }

    @Transactional
    public void deleteGrade(Long id) {
        if (!gradeRepository.existsById(id)) {
            throw new IllegalStateException("Bunday baho yozuvi topilmadi: " + id);
        }
        gradeRepository.deleteById(id);
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
        dto.setGradeDate(grade.getGradeDate());
        dto.setScore(grade.getScore());
        dto.setType(grade.getType());
        dto.setComment(grade.getComment());
        return dto;
    }
}
