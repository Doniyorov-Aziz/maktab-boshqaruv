package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.SchoolRequestDto;
import uz.azizbek.maktabboshqaruv.dto.SchoolResponseDto;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolServiceTest {

    @Mock
    private SchoolRepository schoolRepository;

    @InjectMocks
    private SchoolService schoolService;

    @Test
    void getAllSchools_returnsMappedPage() {
        School school = new School();
        school.setId(1L);
        school.setName("Maktab 1");
        school.setAddress("Toshkent");
        Pageable pageable = PageRequest.of(0, 20);
        when(schoolRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(school), pageable, 1));

        Page<SchoolResponseDto> result = schoolService.getAllSchools(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Maktab 1", result.getContent().get(0).getName());
    }

    @Test
    void getSchoolById_notFound_throws() {
        when(schoolRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> schoolService.getSchoolById(1L));
    }

    @Test
    void createSchool_savesAndReturnsDto() {
        SchoolRequestDto request = new SchoolRequestDto();
        request.setName("Maktab 1");
        request.setAddress("Toshkent");

        School saved = new School();
        saved.setId(1L);
        saved.setName("Maktab 1");
        saved.setAddress("Toshkent");
        when(schoolRepository.save(any(School.class))).thenReturn(saved);

        SchoolResponseDto result = schoolService.createSchool(request);

        assertEquals(1L, result.getId());
        assertEquals("Maktab 1", result.getName());
    }

    @Test
    void updateSchool_notFound_throws() {
        when(schoolRepository.findById(1L)).thenReturn(Optional.empty());

        SchoolRequestDto request = new SchoolRequestDto();
        request.setName("Maktab 1");
        request.setAddress("Toshkent");

        assertThrows(IllegalStateException.class, () -> schoolService.updateSchool(1L, request));
    }

    @Test
    void deleteSchool_notFound_throws() {
        when(schoolRepository.existsById(1L)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> schoolService.deleteSchool(1L));
        verify(schoolRepository, never()).deleteById(any());
    }

    @Test
    void deleteSchool_found_deletes() {
        when(schoolRepository.existsById(1L)).thenReturn(true);

        schoolService.deleteSchool(1L);

        verify(schoolRepository).deleteById(1L);
    }
}
