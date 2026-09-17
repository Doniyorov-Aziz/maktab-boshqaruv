package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.SchoolClassRequestDto;
import uz.azizbek.maktabboshqaruv.dto.SchoolClassResponseDto;
import uz.azizbek.maktabboshqaruv.entity.AcademicYear;
import uz.azizbek.maktabboshqaruv.entity.SchoolClass;
import uz.azizbek.maktabboshqaruv.repository.AcademicYearRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolClassRepository;
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
class SchoolClassServiceTest {

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @Mock
    private AcademicYearRepository academicYearRepository;

    @InjectMocks
    private SchoolClassService schoolClassService;

    private SchoolClassRequestDto validRequest() {
        SchoolClassRequestDto request = new SchoolClassRequestDto();
        request.setAcademicYearId(1L);
        request.setGradeNumber(5);
        request.setSectionLetter("A");
        request.setMaxStudents(30);
        return request;
    }

    @Test
    void createSchoolClass_duplicateSection_throws() {
        SchoolClassRequestDto request = validRequest();
        AcademicYear academicYear = new AcademicYear();
        academicYear.setId(1L);
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(schoolClassRepository.existsByAcademicYearAndGradeNumberAndSectionLetter(academicYear, 5, "A"))
                .thenReturn(true);

        assertThrows(IllegalStateException.class, () -> schoolClassService.createSchoolClass(request));
        verify(schoolClassRepository, never()).save(any());
    }

    @Test
    void createSchoolClass_valid_saves() {
        SchoolClassRequestDto request = validRequest();
        AcademicYear academicYear = new AcademicYear();
        academicYear.setId(1L);
        academicYear.setTitle("2025-2026");
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(schoolClassRepository.existsByAcademicYearAndGradeNumberAndSectionLetter(academicYear, 5, "A"))
                .thenReturn(false);

        SchoolClass saved = new SchoolClass();
        saved.setId(1L);
        saved.setAcademicYear(academicYear);
        saved.setGradeNumber(5);
        saved.setSectionLetter("A");
        saved.setMaxStudents(30);
        when(schoolClassRepository.save(any(SchoolClass.class))).thenReturn(saved);

        SchoolClassResponseDto result = schoolClassService.createSchoolClass(request);

        assertEquals(5, result.getGradeNumber());
        assertEquals("A", result.getSectionLetter());
        assertEquals("2025-2026", result.getAcademicYearTitle());
    }

    @Test
    void updateSchoolClass_unchangedSection_skipsDuplicateCheck() {
        AcademicYear academicYear = new AcademicYear();
        academicYear.setId(1L);
        academicYear.setTitle("2025-2026");

        SchoolClass existing = new SchoolClass();
        existing.setId(1L);
        existing.setAcademicYear(academicYear);
        existing.setGradeNumber(5);
        existing.setSectionLetter("A");
        existing.setMaxStudents(30);

        when(schoolClassRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(schoolClassRepository.save(any(SchoolClass.class))).thenReturn(existing);

        schoolClassService.updateSchoolClass(1L, validRequest());

        verify(schoolClassRepository, never())
                .existsByAcademicYearAndGradeNumberAndSectionLetter(any(), any(), any());
    }

    @Test
    void deleteSchoolClass_notFound_throws() {
        when(schoolClassRepository.existsById(1L)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> schoolClassService.deleteSchoolClass(1L));
    }
}
