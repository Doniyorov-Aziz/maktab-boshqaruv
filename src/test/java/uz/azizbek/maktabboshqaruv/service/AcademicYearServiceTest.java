package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.AcademicYearRequestDto;
import uz.azizbek.maktabboshqaruv.dto.AcademicYearResponseDto;
import uz.azizbek.maktabboshqaruv.entity.AcademicYear;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.repository.AcademicYearRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
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
class AcademicYearServiceTest {

    @Mock
    private AcademicYearRepository academicYearRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @InjectMocks
    private AcademicYearService academicYearService;

    @Test
    void createAcademicYear_startAfterEnd_throws() {
        AcademicYearRequestDto request = new AcademicYearRequestDto();
        request.setSchoolId(1L);
        request.setTitle("2025-2026");
        request.setStartDate(LocalDate.of(2026, 6, 1));
        request.setEndDate(LocalDate.of(2025, 9, 1));

        assertThrows(IllegalStateException.class, () -> academicYearService.createAcademicYear(request));
        verify(schoolRepository, never()).findById(any());
    }

    @Test
    void createAcademicYear_startEqualsEnd_throws() {
        AcademicYearRequestDto request = new AcademicYearRequestDto();
        request.setSchoolId(1L);
        request.setTitle("2025-2026");
        LocalDate date = LocalDate.of(2025, 9, 1);
        request.setStartDate(date);
        request.setEndDate(date);

        assertThrows(IllegalStateException.class, () -> academicYearService.createAcademicYear(request));
    }

    @Test
    void createAcademicYear_valid_saves() {
        AcademicYearRequestDto request = new AcademicYearRequestDto();
        request.setSchoolId(1L);
        request.setTitle("2025-2026");
        request.setStartDate(LocalDate.of(2025, 9, 1));
        request.setEndDate(LocalDate.of(2026, 6, 1));

        School school = new School();
        school.setId(1L);
        school.setName("Maktab 1");
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));

        AcademicYear saved = new AcademicYear();
        saved.setId(1L);
        saved.setSchool(school);
        saved.setTitle("2025-2026");
        saved.setStartDate(request.getStartDate());
        saved.setEndDate(request.getEndDate());
        when(academicYearRepository.save(any(AcademicYear.class))).thenReturn(saved);

        AcademicYearResponseDto result = academicYearService.createAcademicYear(request);

        assertEquals("2025-2026", result.getTitle());
        assertEquals("Maktab 1", result.getSchoolName());
    }

    @Test
    void updateAcademicYear_notFound_throws() {
        when(academicYearRepository.findById(1L)).thenReturn(Optional.empty());

        AcademicYearRequestDto request = new AcademicYearRequestDto();
        request.setSchoolId(1L);
        request.setTitle("2025-2026");
        request.setStartDate(LocalDate.of(2025, 9, 1));
        request.setEndDate(LocalDate.of(2026, 6, 1));

        assertThrows(IllegalStateException.class, () -> academicYearService.updateAcademicYear(1L, request));
    }

    @Test
    void deleteAcademicYear_notFound_throws() {
        when(academicYearRepository.existsById(1L)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> academicYearService.deleteAcademicYear(1L));
    }
}
