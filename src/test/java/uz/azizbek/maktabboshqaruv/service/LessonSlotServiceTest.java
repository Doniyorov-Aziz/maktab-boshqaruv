package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.LessonSlotRequestDto;
import uz.azizbek.maktabboshqaruv.dto.LessonSlotResponseDto;
import uz.azizbek.maktabboshqaruv.entity.AcademicYear;
import uz.azizbek.maktabboshqaruv.entity.Building;
import uz.azizbek.maktabboshqaruv.entity.Employee;
import uz.azizbek.maktabboshqaruv.entity.LessonSlot;
import uz.azizbek.maktabboshqaruv.entity.Room;
import uz.azizbek.maktabboshqaruv.entity.School;
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
import org.springframework.web.server.ResponseStatusException;

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
    private uz.azizbek.maktabboshqaruv.service.BotCache botCache;

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private uz.azizbek.maktabboshqaruv.repository.LessonSubstitutionRepository substitutionRepository;

    @Mock
    private uz.azizbek.maktabboshqaruv.service.dashboard.DashboardCache pageCache;

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

    private SchoolClass schoolClassForSchool(School school) {
        AcademicYear academicYear = new AcademicYear();
        academicYear.setSchool(school);
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setAcademicYear(academicYear);
        return schoolClass;
    }

    private Room roomForSchool(School school) {
        Building building = new Building();
        building.setSchool(school);
        Room room = new Room();
        room.setBuilding(building);
        return room;
    }

    // wires SchoolClass/Subject/Employee/Room to the same school (id=1) so
    // validateSameSchool passes and tests can focus on conflict-checking behavior
    private void stubReferencesFound() {
        School school = new School();
        school.setId(1L);

        Subject subject = new Subject();
        subject.setSchool(school);

        Employee employee = new Employee();
        employee.setSchool(school);

        when(schoolClassRepository.findById(1L)).thenReturn(Optional.of(schoolClassForSchool(school)));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(roomForSchool(school)));
    }

    /** A lesson that is already in the timetable at that time (7-B, Fizika, Karimova D., room 204). */
    private LessonSlot existingLesson(long classId, long employeeId, long roomId) {
        School school = new School();
        school.setId(1L);
        SchoolClass c = schoolClassForSchool(school);
        c.setId(classId);
        c.setGradeNumber(classId == 1L ? 5 : 7);
        c.setSectionLetter(classId == 1L ? "A" : "B");
        Employee e = new Employee();
        e.setId(employeeId);
        e.setFirstName("Dilnoza");
        e.setLastName("Karimova");
        Room r = roomForSchool(school);
        r.setId(roomId);
        r.setRoomNumber("204");
        Subject subject = new Subject();
        subject.setName("Fizika");
        LessonSlot l = new LessonSlot();
        l.setSchoolClass(c);
        l.setEmployee(e);
        l.setRoom(r);
        l.setSubject(subject);
        return l;
    }

    private void overlapping(LessonSlot... lessons) {
        when(lessonSlotRepository.findOverlapping(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(java.util.List.of(lessons));
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
    void createLessonSlot_crossSchoolReferences_throws() {
        LessonSlotRequestDto request = validRequest();

        School school1 = new School();
        school1.setId(1L);
        School school2 = new School();
        school2.setId(2L);

        Subject subject = new Subject();
        subject.setSchool(school1);

        Employee employee = new Employee();
        employee.setSchool(school2); // belongs to a different school than the class/subject/room

        when(schoolClassRepository.findById(1L)).thenReturn(Optional.of(schoolClassForSchool(school1)));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(roomForSchool(school1)));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> lessonSlotService.createLessonSlot(request));
        assertEquals("Sinf, fan, o'qituvchi va xona bitta maktabga tegishli bo'lishi kerak", ex.getMessage());
        verify(lessonSlotRepository, never()).save(any());
    }

    @Test
    void createLessonSlot_roomConflict_throws() {
        LessonSlotRequestDto request = validRequest();
        stubReferencesFound();
        overlapping(existingLesson(2L, 2L, 1L));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> lessonSlotService.createLessonSlot(request));
        assertEquals("204-xona bu vaqtda band (7-B, Fizika)", ex.getMessage());
        verify(lessonSlotRepository, never()).save(any());
    }

    @Test
    void createLessonSlot_employeeConflict_throws() {
        LessonSlotRequestDto request = validRequest();
        stubReferencesFound();
        overlapping(existingLesson(2L, 1L, 2L));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> lessonSlotService.createLessonSlot(request));
        assertEquals("Karimova D. bu vaqtda 7-B da", ex.getMessage());
        verify(lessonSlotRepository, never()).save(any());
    }

    @Test
    void createLessonSlot_schoolClassConflict_throws() {
        LessonSlotRequestDto request = validRequest();
        stubReferencesFound();
        // the class itself is busy — said first, even if the teacher is busy there too
        overlapping(existingLesson(2L, 1L, 2L), existingLesson(1L, 1L, 2L));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> lessonSlotService.createLessonSlot(request));
        assertEquals("5-A sinfida bu vaqtda dars bor (Fizika)", ex.getMessage());
        verify(lessonSlotRepository, never()).save(any());
    }

    @Test
    void createLessonSlot_noConflicts_saves() {
        LessonSlotRequestDto request = validRequest();

        School school = new School();
        school.setId(1L);

        SchoolClass schoolClass = schoolClassForSchool(school);
        schoolClass.setId(1L);
        schoolClass.setGradeNumber(5);
        schoolClass.setSectionLetter("A");

        Subject subject = new Subject();
        subject.setId(1L);
        subject.setName("Matematika");
        subject.setSchool(school);

        Employee employee = new Employee();
        employee.setId(1L);
        employee.setFirstName("Aziz");
        employee.setLastName("Karimov");
        employee.setSchool(school);

        Room room = roomForSchool(school);
        room.setId(1L);
        room.setRoomNumber("101");

        when(schoolClassRepository.findById(1L)).thenReturn(Optional.of(schoolClass));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        overlapping();

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
        overlapping();
        when(lessonSlotRepository.save(any(LessonSlot.class))).thenReturn(existing);

        lessonSlotService.updateLessonSlot(5L, request);

        verify(lessonSlotRepository).findOverlapping("Dushanba", request.getStartTime(), request.getEndTime(), 5L, 1L, 1L, 1L);
    }

    @Test
    void deleteLessonSlot_notFound_throws() {
        when(lessonSlotRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> lessonSlotService.deleteLessonSlot(1L));
    }
}
