package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.BuildingRequestDto;
import uz.azizbek.maktabboshqaruv.dto.BuildingResponseDto;
import uz.azizbek.maktabboshqaruv.entity.Building;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.repository.BuildingRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
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
class BuildingServiceTest {

    @Mock
    private BuildingRepository buildingRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @InjectMocks
    private BuildingService buildingService;

    @Test
    void createBuilding_unknownSchool_throws() {
        BuildingRequestDto request = new BuildingRequestDto();
        request.setSchoolId(1L);
        request.setName("Asosiy bino");
        when(schoolRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> buildingService.createBuilding(request));
        verify(buildingRepository, never()).save(any());
    }

    @Test
    void createBuilding_valid_saves() {
        BuildingRequestDto request = new BuildingRequestDto();
        request.setSchoolId(1L);
        request.setName("Asosiy bino");

        School school = new School();
        school.setId(1L);
        school.setName("Maktab 1");
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));

        Building saved = new Building();
        saved.setId(1L);
        saved.setSchool(school);
        saved.setName("Asosiy bino");
        when(buildingRepository.save(any(Building.class))).thenReturn(saved);

        BuildingResponseDto result = buildingService.createBuilding(request);

        assertEquals("Asosiy bino", result.getName());
        assertEquals("Maktab 1", result.getSchoolName());
    }

    @Test
    void getBuildingById_notFound_throws() {
        when(buildingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> buildingService.getBuildingById(1L));
    }

    @Test
    void deleteBuilding_notFound_throws() {
        when(buildingRepository.existsById(1L)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> buildingService.deleteBuilding(1L));
    }

    @Test
    void deleteBuilding_found_deletes() {
        when(buildingRepository.existsById(1L)).thenReturn(true);

        buildingService.deleteBuilding(1L);

        verify(buildingRepository).deleteById(1L);
    }
}
