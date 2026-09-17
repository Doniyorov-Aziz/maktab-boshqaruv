package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.BuildingRequestDto;
import uz.azizbek.maktabboshqaruv.dto.BuildingResponseDto;
import uz.azizbek.maktabboshqaruv.entity.Building;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.repository.BuildingRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BuildingService {

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    public Page<BuildingResponseDto> getAllBuildings(Pageable pageable) {
        return buildingRepository.findAll(pageable)
                .map(this::toResponseDto);
    }

    public BuildingResponseDto getBuildingById(Long id) {
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday bino topilmadi: " + id));
        return toResponseDto(building);
    }

    @Transactional
    public BuildingResponseDto createBuilding(BuildingRequestDto request) {
        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new IllegalStateException("Bunday maktab mavjud emas"));

        Building building = new Building();
        building.setSchool(school);
        building.setName(request.getName());

        Building saved = buildingRepository.save(building);
        return toResponseDto(saved);
    }

    @Transactional
    public BuildingResponseDto updateBuilding(Long id, BuildingRequestDto request) {
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday bino topilmadi: " + id));

        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new IllegalStateException("Bunday maktab mavjud emas"));

        building.setSchool(school);
        building.setName(request.getName());

        Building updated = buildingRepository.save(building);
        return toResponseDto(updated);
    }

    @Transactional
    public void deleteBuilding(Long id) {
        if (!buildingRepository.existsById(id)) {
            throw new IllegalStateException("Bunday bino topilmadi: " + id);
        }
        buildingRepository.deleteById(id);
    }

    private BuildingResponseDto toResponseDto(Building building) {
        BuildingResponseDto dto = new BuildingResponseDto();
        dto.setId(building.getId());
        dto.setName(building.getName());
        dto.setSchoolId(building.getSchool().getId());
        dto.setSchoolName(building.getSchool().getName());
        return dto;
    }
}
