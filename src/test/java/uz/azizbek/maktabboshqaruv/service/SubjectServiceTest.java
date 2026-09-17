package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.SubjectRequestDto;
import uz.azizbek.maktabboshqaruv.dto.SubjectResponseDto;
import uz.azizbek.maktabboshqaruv.entity.Subject;
import uz.azizbek.maktabboshqaruv.repository.SubjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    private SubjectService subjectService;

    @Test
    void createSubject_duplicateName_throws() {
        SubjectRequestDto request = new SubjectRequestDto();
        request.setName("Matematika");
        when(subjectRepository.existsByName("Matematika")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> subjectService.createSubject(request));
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void createSubject_valid_saves() {
        SubjectRequestDto request = new SubjectRequestDto();
        request.setName("Matematika");
        when(subjectRepository.existsByName("Matematika")).thenReturn(false);

        Subject saved = new Subject();
        saved.setId(1L);
        saved.setName("Matematika");
        when(subjectRepository.save(any(Subject.class))).thenReturn(saved);

        SubjectResponseDto result = subjectService.createSubject(request);

        assertEquals("Matematika", result.getName());
    }

    @Test
    void deleteSubject_notFound_throws() {
        when(subjectRepository.existsById(1L)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> subjectService.deleteSubject(1L));
    }
}
