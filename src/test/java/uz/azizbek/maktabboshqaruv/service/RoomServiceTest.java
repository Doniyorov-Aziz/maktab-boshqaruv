package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.RoomRequestDto;
import uz.azizbek.maktabboshqaruv.dto.RoomResponseDto;
import uz.azizbek.maktabboshqaruv.entity.Building;
import uz.azizbek.maktabboshqaruv.entity.Room;
import uz.azizbek.maktabboshqaruv.repository.BuildingRepository;
import uz.azizbek.maktabboshqaruv.repository.LessonSlotRepository;
import uz.azizbek.maktabboshqaruv.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BuildingRepository buildingRepository;

    @Mock
    private LessonSlotRepository lessonSlotRepository;

    @Mock
    private uz.azizbek.maktabboshqaruv.service.dashboard.DashboardCache pageCache;

    @InjectMocks
    private RoomService roomService;

    private RoomRequestDto validRequest() {
        RoomRequestDto request = new RoomRequestDto();
        request.setBuildingId(1L);
        request.setRoomNumber("101");
        request.setCapacity(30);
        return request;
    }

    @Test
    void createRoom_unknownBuilding_throws() {
        RoomRequestDto request = validRequest();
        when(buildingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> roomService.createRoom(request));
        verify(roomRepository, never()).save(any());
    }

    @Test
    void createRoom_valid_saves() {
        RoomRequestDto request = validRequest();
        Building building = new Building();
        building.setId(1L);
        building.setName("Asosiy bino");
        uz.azizbek.maktabboshqaruv.entity.School school = new uz.azizbek.maktabboshqaruv.entity.School();
        school.setId(1L);
        building.setSchool(school);
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));

        Room saved = new Room();
        saved.setId(1L);
        saved.setBuilding(building);
        saved.setRoomNumber("101");
        saved.setCapacity(30);
        when(roomRepository.save(any(Room.class))).thenReturn(saved);

        RoomResponseDto result = roomService.createRoom(request);

        assertEquals("101", result.getRoomNumber());
        assertEquals("Asosiy bino", result.getBuildingName());
    }

    @Test
    void deleteRoom_notFound_throws() {
        when(roomRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> roomService.deleteRoom(1L));
    }
}
