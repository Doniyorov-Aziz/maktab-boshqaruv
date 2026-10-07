package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.StudentRequestDto;
import uz.azizbek.maktabboshqaruv.dto.StudentResponseDto;
import uz.azizbek.maktabboshqaruv.entity.SchoolClass;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.repository.SchoolClassRepository;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private BotCache botCache;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private ActivityLogService activityLogService;

    public Page<StudentResponseDto> getAllStudents(Long schoolId, Pageable pageable) {
        return getAllStudents(schoolId, null, null, pageable);
    }

    public Page<StudentResponseDto> getAllStudents(Long schoolId, Long schoolClassId, String q, Pageable pageable) {
        String like = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase().replaceAll("\\s+", " ") + "%";
        return studentRepository.search(schoolId, schoolClassId, like, pageable)
                .map(this::toResponseDto);
    }

    public StudentResponseDto getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday o'quvchi topilmadi: " + id));
        return toResponseDto(student);
    }

    @Transactional
    public StudentResponseDto createStudent(StudentRequestDto request) {
        SchoolClass schoolClass = schoolClassRepository.findById(request.getSchoolClassId())
                .orElseThrow(() -> new IllegalStateException("Bunday sinf mavjud emas"));

        Student student = new Student();
        student.setSchoolClass(schoolClass);
        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setBirthDate(request.getBirthDate());
        student.setGuardianName(request.getGuardianName());
        student.setGuardianPhone(request.getGuardianPhone());
        student.setGuardianRelation(request.getGuardianRelation());

        Student saved = studentRepository.save(student);
        activityLogService.record(
                schoolClass.getAcademicYear().getSchool(),
                "person_add",
                student.getFirstName() + " " + student.getLastName() + " " + schoolClass.getGradeNumber()
                        + "-" + schoolClass.getSectionLetter() + " sinfiga qo'shildi");
        return toResponseDto(saved);
    }

    @Transactional
    public StudentResponseDto updateStudent(Long id, StudentRequestDto request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday o'quvchi topilmadi: " + id));

        SchoolClass schoolClass = schoolClassRepository.findById(request.getSchoolClassId())
                .orElseThrow(() -> new IllegalStateException("Bunday sinf mavjud emas"));

        student.setSchoolClass(schoolClass);
        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setBirthDate(request.getBirthDate());
        student.setGuardianName(request.getGuardianName());
        student.setGuardianPhone(request.getGuardianPhone());
        student.setGuardianRelation(request.getGuardianRelation());

        Student updated = studentRepository.save(student);
        botCache.evictAllChats();
        return toResponseDto(updated);
    }

    @Transactional
    public void deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new IllegalStateException("Bunday o'quvchi topilmadi: " + id);
        }
        studentRepository.deleteById(id);
        botCache.evictAllChats();
    }

    private StudentResponseDto toResponseDto(Student student) {
        StudentResponseDto dto = new StudentResponseDto();
        dto.setId(student.getId());
        dto.setFirstName(student.getFirstName());
        dto.setLastName(student.getLastName());
        dto.setFullName(student.getFirstName() + " " + student.getLastName());
        dto.setBirthDate(student.getBirthDate());
        dto.setSchoolClassId(student.getSchoolClass().getId());
        dto.setClassName(student.getSchoolClass().getGradeNumber() + "-" + student.getSchoolClass().getSectionLetter());
        dto.setGuardianName(student.getGuardianName());
        dto.setGuardianPhone(student.getGuardianPhone());
        dto.setGuardianRelation(student.getGuardianRelation() == null ? null : student.getGuardianRelation().name());
        return dto;
    }
}
