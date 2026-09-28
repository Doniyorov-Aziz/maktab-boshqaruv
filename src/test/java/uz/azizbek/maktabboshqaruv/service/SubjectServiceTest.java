package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.SubjectRequestDto;
import uz.azizbek.maktabboshqaruv.dto.SubjectResponseDto;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.entity.Subject;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import uz.azizbek.maktabboshqaruv.repository.SubjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubjectServiceTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @InjectMocks
    private SubjectService subjectService;

    private SubjectRequestDto validRequest() {
        SubjectRequestDto request = new SubjectRequestDto();
        request.setSchoolId(1L);
        request.setName("Matematika");
        return request;
    }

    @Test
    void createSubject_unknownSchool_throws() {
        SubjectRequestDto request = validRequest();
        when(schoolRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> subjectService.createSubject(request));
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void createSubject_duplicateName_throws() {
        SubjectRequestDto request = validRequest();
        School school = new School();
        school.setId(1L);
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(subjectRepository.existsBySchoolIdAndName(1L, "Matematika")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> subjectService.createSubject(request));
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void createSubject_valid_saves() {
        SubjectRequestDto request = validRequest();
        School school = new School();
        school.setId(1L);
        school.setName("Maktab 1");
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(subjectRepository.existsBySchoolIdAndName(1L, "Matematika")).thenReturn(false);

        Subject saved = new Subject();
        saved.setId(1L);
        saved.setSchool(school);
        saved.setName("Matematika");
        when(subjectRepository.save(any(Subject.class))).thenReturn(saved);

        SubjectResponseDto result = subjectService.createSubject(request);

        assertEquals("Matematika", result.getName());
        assertEquals("Maktab 1", result.getSchoolName());
    }

    @Test
    void deleteSubject_notFound_throws() {
        when(subjectRepository.existsById(1L)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> subjectService.deleteSubject(1L));
    }
}
