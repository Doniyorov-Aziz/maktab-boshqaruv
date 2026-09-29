package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.RoomRequestDto;
import uz.azizbek.maktabboshqaruv.dto.RoomResponseDto;
import uz.azizbek.maktabboshqaruv.entity.Building;
import uz.azizbek.maktabboshqaruv.entity.LessonSlot;
import uz.azizbek.maktabboshqaruv.entity.Room;
import uz.azizbek.maktabboshqaruv.entity.RoomType;
import uz.azizbek.maktabboshqaruv.repository.BuildingRepository;
import uz.azizbek.maktabboshqaruv.repository.LessonSlotRepository;
import uz.azizbek.maktabboshqaruv.repository.RoomRepository;
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
import java.util.Optional;

@Service
public class RoomService {

    private static final Map<DayOfWeek, String> WEEKDAY_NAMES = Map.of(
            DayOfWeek.MONDAY, "Dushanba", DayOfWeek.TUESDAY, "Seshanba", DayOfWeek.WEDNESDAY, "Chorshanba",
            DayOfWeek.THURSDAY, "Payshanba", DayOfWeek.FRIDAY, "Juma", DayOfWeek.SATURDAY, "Shanba");

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private LessonSlotRepository lessonSlotRepository;

    public Page<RoomResponseDto> getAllRooms(Long schoolId, Long buildingId, String type, Pageable pageable) {
        RoomType parsedType = type != null && !type.isBlank() ? RoomType.valueOf(type) : null;
        return roomRepository.findFiltered(schoolId, buildingId, parsedType, pageable)
                .map(this::toResponseDto);
    }

    public RoomResponseDto getRoomById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday xona topilmadi: " + id));
        return toResponseDto(room);
    }

    @Transactional
    public RoomResponseDto createRoom(RoomRequestDto request) {
        Building building = buildingRepository.findById(request.getBuildingId())
                .orElseThrow(() -> new IllegalStateException("Bunday bino mavjud emas"));

        Room room = new Room();
        room.setBuilding(building);
        room.setRoomNumber(request.getRoomNumber());
        room.setCapacity(request.getCapacity());
        room.setFloor(request.getFloor());
        if (request.getType() != null) {
            room.setType(RoomType.valueOf(request.getType()));
        }

        Room saved = roomRepository.save(room);
        return toResponseDto(saved);
    }

    @Transactional
    public RoomResponseDto updateRoom(Long id, RoomRequestDto request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday xona topilmadi: " + id));

        Building building = buildingRepository.findById(request.getBuildingId())
                .orElseThrow(() -> new IllegalStateException("Bunday bino mavjud emas"));

        room.setBuilding(building);
        room.setRoomNumber(request.getRoomNumber());
        room.setCapacity(request.getCapacity());
        room.setFloor(request.getFloor());
        if (request.getType() != null) {
            room.setType(RoomType.valueOf(request.getType()));
        }

        Room updated = roomRepository.save(room);
        return toResponseDto(updated);
    }

    @Transactional
    public void deleteRoom(Long id) {
        if (!roomRepository.existsById(id)) {
            throw new IllegalStateException("Bunday xona topilmadi: " + id);
        }
        roomRepository.deleteById(id);
    }

    private RoomResponseDto toResponseDto(Room room) {
        RoomResponseDto dto = new RoomResponseDto();
        dto.setId(room.getId());
        dto.setRoomNumber(room.getRoomNumber());
        dto.setCapacity(room.getCapacity());
        dto.setFloor(room.getFloor());
        dto.setType(room.getType().name());
        dto.setBuildingId(room.getBuilding().getId());
        dto.setBuildingName(room.getBuilding().getName());
        dto.setCurrentStatus(currentStatus(room));
        return dto;
    }

    public List<uz.azizbek.maktabboshqaruv.dto.RoomOccupancySlotDto> getDayOccupancy(Long roomId) {
        LocalDate today = LocalDate.now();
        String weekday = WEEKDAY_NAMES.get(today.getDayOfWeek());
        if (weekday == null) return java.util.List.of();

        LocalTime now = LocalTime.now();
        return lessonSlotRepository.findByRoomIdAndWeekday(roomId, weekday).stream()
                .map(ls -> {
                    uz.azizbek.maktabboshqaruv.dto.RoomOccupancySlotDto dto = new uz.azizbek.maktabboshqaruv.dto.RoomOccupancySlotDto();
                    dto.setStartTime(ls.getStartTime().toString());
                    dto.setEndTime(ls.getEndTime().toString());
                    dto.setClassName(ls.getSchoolClass().getGradeNumber() + "-" + ls.getSchoolClass().getSectionLetter());
                    dto.setSubjectName(ls.getSubject().getName());
                    dto.setTeacherName(ls.getEmployee().getFirstName() + " " + ls.getEmployee().getLastName());
                    dto.setCurrent(!now.isBefore(ls.getStartTime()) && now.isBefore(ls.getEndTime()));
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    private String currentStatus(Room room) {
        LocalDate today = LocalDate.now();
        String weekday = WEEKDAY_NAMES.get(today.getDayOfWeek());
        if (weekday == null) return "Bo'sh";
        Optional<LessonSlot> lesson = lessonSlotRepository
                .findCurrentlyInSessionByRoom(room.getId(), weekday, LocalTime.now());
        if (lesson.isEmpty()) return "Bo'sh";
        LessonSlot l = lesson.get();
        return "Band: " + l.getSchoolClass().getGradeNumber() + "-" + l.getSchoolClass().getSectionLetter()
                + ", " + l.getSubject().getName();
    }
}
