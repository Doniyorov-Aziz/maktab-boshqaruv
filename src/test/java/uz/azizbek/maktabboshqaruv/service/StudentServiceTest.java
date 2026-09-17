package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.StudentRequestDto;
import uz.azizbek.maktabboshqaruv.dto.StudentResponseDto;
import uz.azizbek.maktabboshqaruv.entity.SchoolClass;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.repository.SchoolClassRepository;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @InjectMocks
    private StudentService studentService;

    private StudentRequestDto validRequest() {
        StudentRequestDto request = new StudentRequestDto();
        request.setSchoolClassId(1L);
        request.setFirstName("Vali");
        request.setLastName("Aliyev");
        request.setBirthDate(LocalDate.of(2015, 3, 10));
        return request;
    }

    @Test
    void createStudent_unknownClass_throws() {
        StudentRequestDto request = validRequest();
        when(schoolClassRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> studentService.createStudent(request));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void createStudent_valid_saves() {
        StudentRequestDto request = validRequest();

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(1L);
        schoolClass.setGradeNumber(5);
        schoolClass.setSectionLetter("A");
        when(schoolClassRepository.findById(1L)).thenReturn(Optional.of(schoolClass));

        Student saved = new Student();
        saved.setId(1L);
        saved.setSchoolClass(schoolClass);
        saved.setFirstName("Vali");
        saved.setLastName("Aliyev");
        saved.setBirthDate(request.getBirthDate());
        when(studentRepository.save(any(Student.class))).thenReturn(saved);

        StudentResponseDto result = studentService.createStudent(request);

        assertEquals("Vali Aliyev", result.getFullName());
        assertEquals("5-A", result.getClassName());
    }

    @Test
    void updateStudent_notFound_throws() {
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> studentService.updateStudent(1L, validRequest()));
    }

    @Test
    void deleteStudent_notFound_throws() {
        when(studentRepository.existsById(1L)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> studentService.deleteStudent(1L));
    }
}
