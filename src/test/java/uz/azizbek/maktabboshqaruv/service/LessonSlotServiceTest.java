package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.LessonSlotRequestDto;
import uz.azizbek.maktabboshqaruv.dto.LessonSlotResponseDto;
import uz.azizbek.maktabboshqaruv.entity.Employee;
import uz.azizbek.maktabboshqaruv.entity.LessonSlot;
import uz.azizbek.maktabboshqaruv.entity.Room;
import uz.azizbek.maktabboshqaruv.entity.SchoolClass;
import uz.azizbek.maktabboshqaruv.entity.Subject;
import uz.azizbek.maktabboshqaruv.repository.EmployeeRepository;
import uz.azizbek.maktabboshqaruv.repository.LessonSlotRepository;
import uz.azizbek.maktabboshqaruv.repository.RoomRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolClassRepository;
import uz.azizbek.maktabboshqaruv.repository.SubjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonSlotServiceTest {

    @Mock
    private LessonSlotRepository lessonSlotRepository;

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private LessonSlotService lessonSlotService;

    private LessonSlotRequestDto validRequest() {
        LessonSlotRequestDto request = new LessonSlotRequestDto();
        request.setSchoolClassId(1L);
        request.setSubjectId(1L);
        request.setEmployeeId(1L);
        request.setRoomId(1L);
        request.setWeekday("Dushanba");
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));
        return request;
    }

    private void stubReferencesFound() {
        when(schoolClassRepository.findById(1L)).thenReturn(Optional.of(new SchoolClass()));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(new Subject()));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(new Employee()));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(new Room()));
    }

    @Test
    void createLessonSlot_startAfterEnd_throws() {
        LessonSlotRequestDto request = validRequest();
        request.setStartTime(LocalTime.of(11, 0));
        request.setEndTime(LocalTime.of(10, 0));

        assertThrows(IllegalStateException.class, () -> lessonSlotService.createLessonSlot(request));
        verify(schoolClassRepository, never()).findById(any());
    }

    @Test
    void createLessonSlot_roomConflict_throws() {
        LessonSlotRequestDto request = validRequest();
        stubReferencesFound();
        when(lessonSlotRepository.existsRoomConflict(1L, "Dushanba", request.getStartTime(), request.getEndTime(), null))
                .thenReturn(true);

        assertThrows(IllegalStateException.class, () -> lessonSlotService.createLessonSlot(request));
        verify(lessonSlotRepository, never()).save(any());
    }

    @Test
    void createLessonSlot_employeeConflict_throws() {
        LessonSlotRequestDto request = validRequest();
        stubReferencesFound();
        when(lessonSlotRepository.existsRoomConflict(any(), any(), any(), any(), any())).thenReturn(false);
        when(lessonSlotRepository.existsEmployeeConflict(1L, "Dushanba", request.getStartTime(), request.getEndTime(), null))
                .thenReturn(true);

        assertThrows(IllegalStateException.class, () -> lessonSlotService.createLessonSlot(request));
        verify(lessonSlotRepository, never()).save(any());
    }

    @Test
    void createLessonSlot_schoolClassConflict_throws() {
        LessonSlotRequestDto request = validRequest();
        stubReferencesFound();
        when(lessonSlotRepository.existsRoomConflict(any(), any(), any(), any(), any())).thenReturn(false);
        when(lessonSlotRepository.existsEmployeeConflict(any(), any(), any(), any(), any())).thenReturn(false);
        when(lessonSlotRepository.existsSchoolClassConflict(1L, "Dushanba", request.getStartTime(), request.getEndTime(), null))
                .thenReturn(true);

        assertThrows(IllegalStateException.class, () -> lessonSlotService.createLessonSlot(request));
        verify(lessonSlotRepository, never()).save(any());
    }

    @Test
    void createLessonSlot_noConflicts_saves() {
        LessonSlotRequestDto request = validRequest();

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(1L);
        schoolClass.setGradeNumber(5);
        schoolClass.setSectionLetter("A");

        Subject subject = new Subject();
        subject.setId(1L);
        subject.setName("Matematika");

        Employee employee = new Employee();
        employee.setId(1L);
        employee.setFirstName("Aziz");
        employee.setLastName("Karimov");

        Room room = new Room();
        room.setId(1L);
        room.setRoomNumber("101");

        when(schoolClassRepository.findById(1L)).thenReturn(Optional.of(schoolClass));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(lessonSlotRepository.existsRoomConflict(any(), any(), any(), any(), any())).thenReturn(false);
        when(lessonSlotRepository.existsEmployeeConflict(any(), any(), any(), any(), any())).thenReturn(false);
        when(lessonSlotRepository.existsSchoolClassConflict(any(), any(), any(), any(), any())).thenReturn(false);

        LessonSlot saved = new LessonSlot();
        saved.setId(1L);
        saved.setSchoolClass(schoolClass);
        saved.setSubject(subject);
        saved.setEmployee(employee);
        saved.setRoom(room);
        saved.setWeekday("Dushanba");
        saved.setStartTime(request.getStartTime());
        saved.setEndTime(request.getEndTime());
        when(lessonSlotRepository.save(any(LessonSlot.class))).thenReturn(saved);

        LessonSlotResponseDto result = lessonSlotService.createLessonSlot(request);

        assertEquals("5-A", result.getClassName());
        assertEquals("Matematika", result.getSubjectName());
        assertEquals("Aziz Karimov", result.getEmployeeName());
        assertEquals("101", result.getRoomNumber());
    }

    @Test
    void updateLessonSlot_excludesSelfFromConflictCheck() {
        LessonSlotRequestDto request = validRequest();

        LessonSlot existing = new LessonSlot();
        existing.setId(5L);
        when(lessonSlotRepository.findById(5L)).thenReturn(Optional.of(existing));

        stubReferencesFound();
        when(lessonSlotRepository.existsRoomConflict(1L, "Dushanba", request.getStartTime(), request.getEndTime(), 5L))
                .thenReturn(false);
        when(lessonSlotRepository.existsEmployeeConflict(1L, "Dushanba", request.getStartTime(), request.getEndTime(), 5L))
                .thenReturn(false);
        when(lessonSlotRepository.existsSchoolClassConflict(1L, "Dushanba", request.getStartTime(), request.getEndTime(), 5L))
                .thenReturn(false);
        when(lessonSlotRepository.save(any(LessonSlot.class))).thenReturn(existing);

        lessonSlotService.updateLessonSlot(5L, request);

        verify(lessonSlotRepository).existsRoomConflict(1L, "Dushanba", request.getStartTime(), request.getEndTime(), 5L);
        verify(lessonSlotRepository).existsEmployeeConflict(1L, "Dushanba", request.getStartTime(), request.getEndTime(), 5L);
        verify(lessonSlotRepository).existsSchoolClassConflict(1L, "Dushanba", request.getStartTime(), request.getEndTime(), 5L);
    }

    @Test
    void deleteLessonSlot_notFound_throws() {
        when(lessonSlotRepository.existsById(1L)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> lessonSlotService.deleteLessonSlot(1L));
    }
}
