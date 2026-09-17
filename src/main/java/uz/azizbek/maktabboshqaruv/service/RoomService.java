package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.RoomRequestDto;
import uz.azizbek.maktabboshqaruv.dto.RoomResponseDto;
import uz.azizbek.maktabboshqaruv.entity.Building;
import uz.azizbek.maktabboshqaruv.entity.Room;
import uz.azizbek.maktabboshqaruv.repository.BuildingRepository;
import uz.azizbek.maktabboshqaruv.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BuildingRepository buildingRepository;

    public Page<RoomResponseDto> getAllRooms(Pageable pageable) {
        return roomRepository.findAll(pageable)
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
        dto.setBuildingId(room.getBuilding().getId());
        dto.setBuildingName(room.getBuilding().getName());
        return dto;
    }
}
