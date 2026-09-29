package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.BuildingRequestDto;
import uz.azizbek.maktabboshqaruv.dto.BuildingResponseDto;
import uz.azizbek.maktabboshqaruv.entity.Building;
import uz.azizbek.maktabboshqaruv.entity.Room;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.repository.BuildingRepository;
import uz.azizbek.maktabboshqaruv.repository.LessonSlotRepository;
import uz.azizbek.maktabboshqaruv.repository.RoomRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
public class BuildingService {

    private static final Map<DayOfWeek, String> WEEKDAY_NAMES = Map.of(
            DayOfWeek.MONDAY, "Dushanba", DayOfWeek.TUESDAY, "Seshanba", DayOfWeek.WEDNESDAY, "Chorshanba",
            DayOfWeek.THURSDAY, "Payshanba", DayOfWeek.FRIDAY, "Juma", DayOfWeek.SATURDAY, "Shanba");

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private LessonSlotRepository lessonSlotRepository;

    public Page<BuildingResponseDto> getAllBuildings(Long schoolId, Pageable pageable) {
        return buildingRepository.findBySchoolId(schoolId, pageable)
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

        List<Room> rooms = roomRepository.findByBuildingId(building.getId());
        dto.setRoomCount((long) rooms.size());
        dto.setFloorCount(rooms.stream().map(Room::getFloor).filter(f -> f != null)
                .max(Integer::compareTo).orElse(1));

        if (!rooms.isEmpty()) {
            LocalDate today = LocalDate.now();
            String weekday = WEEKDAY_NAMES.get(today.getDayOfWeek());
            long occupied = 0;
            if (weekday != null) {
                LocalTime now = LocalTime.now();
                occupied = rooms.stream()
                        .filter(r -> lessonSlotRepository.findCurrentlyInSessionByRoom(r.getId(), weekday, now).isPresent())
                        .count();
            }
            dto.setOccupiedPercentage(Math.round(occupied * 1000.0 / rooms.size()) / 10.0);
        } else {
            dto.setOccupiedPercentage(0.0);
        }

        return dto;
    }
}
