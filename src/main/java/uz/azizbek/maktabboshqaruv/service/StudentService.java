package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.StudentRequestDto;
import uz.azizbek.maktabboshqaruv.dto.StudentResponseDto;
import uz.azizbek.maktabboshqaruv.entity.SchoolClass;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.repository.SchoolClassRepository;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    public List<StudentResponseDto> getAllStudents() {
        return studentRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .toList();
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

        Student saved = studentRepository.save(student);
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

        Student updated = studentRepository.save(student);
        return toResponseDto(updated);
    }

    @Transactional
    public void deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new IllegalStateException("Bunday o'quvchi topilmadi: " + id);
        }
        studentRepository.deleteById(id);
    }

    private StudentResponseDto toResponseDto(Student student) {
        StudentResponseDto dto = new StudentResponseDto();
        dto.setId(student.getId());
        dto.setFullName(student.getFirstName() + " " + student.getLastName());
        dto.setBirthDate(student.getBirthDate());
        dto.setSchoolClassId(student.getSchoolClass().getId());
        dto.setClassName(student.getSchoolClass().getGradeNumber() + "-" + student.getSchoolClass().getSectionLetter());
        return dto;
    }
}
