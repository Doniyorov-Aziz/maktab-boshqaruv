package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.LessonSlotRequestDto;
import uz.azizbek.maktabboshqaruv.dto.LessonSlotResponseDto;
import uz.azizbek.maktabboshqaruv.dto.TimetableEntryDto;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LessonSlotService {

    @Autowired
    private LessonSlotRepository lessonSlotRepository;

    @Autowired
    private BotCache botCache;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private uz.azizbek.maktabboshqaruv.repository.LessonSubstitutionRepository substitutionRepository;

    /** The timetable and rooms pages cache their numbers per school. */
    @Autowired
    private uz.azizbek.maktabboshqaruv.service.dashboard.DashboardCache pageCache;

    private void evictPages(SchoolClass schoolClass) {
        pageCache.evictSchool(schoolClass.getAcademicYear().getSchool().getId());
    }

    public Page<LessonSlotResponseDto> getAllLessonSlots(Long schoolId, Pageable pageable) {
        return lessonSlotRepository.findBySchoolClassAcademicYearSchoolId(schoolId, pageable)
                .map(this::toResponseDto);
    }

    public List<TimetableEntryDto> getTimetable(Long schoolId, Long schoolClassId, Long employeeId, Long roomId) {
        List<LessonSlot> lessons;
        if (schoolClassId != null) {
            lessons = lessonSlotRepository.findBySchoolClassId(schoolClassId);
        } else if (employeeId != null) {
            lessons = lessonSlotRepository.findByEmployeeId(employeeId);
        } else if (roomId != null) {
            lessons = lessonSlotRepository.findByRoomId(roomId);
        } else {
            lessons = lessonSlotRepository.findBySchoolClassAcademicYearSchoolId(schoolId);
        }
        return lessons.stream().map(this::toTimetableEntryDto).collect(Collectors.toList());
    }

    private TimetableEntryDto toTimetableEntryDto(LessonSlot lessonSlot) {
        TimetableEntryDto dto = new TimetableEntryDto();
        dto.setLessonSlotId(lessonSlot.getId());
        dto.setSchoolClassId(lessonSlot.getSchoolClass().getId());
        dto.setClassName(lessonSlot.getSchoolClass().getGradeNumber() + "-" + lessonSlot.getSchoolClass().getSectionLetter());
        dto.setSubjectId(lessonSlot.getSubject().getId());
        dto.setSubjectName(lessonSlot.getSubject().getName());
        dto.setSubjectColor(lessonSlot.getSubject().getColor());
        dto.setTeacherId(lessonSlot.getEmployee().getId());
        dto.setTeacherName(lessonSlot.getEmployee().getFirstName() + " " + lessonSlot.getEmployee().getLastName());
        dto.setTeacherLastName(lessonSlot.getEmployee().getLastName());
        dto.setRoomId(lessonSlot.getRoom().getId());
        dto.setRoomNumber(lessonSlot.getRoom().getRoomNumber());
        dto.setWeekday(lessonSlot.getWeekday());
        dto.setStartTime(lessonSlot.getStartTime());
        dto.setEndTime(lessonSlot.getEndTime());
        return dto;
    }

    public LessonSlotResponseDto getLessonSlotById(Long id) {
        LessonSlot lessonSlot = lessonSlotRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday dars jadvali topilmadi: " + id));
        return toResponseDto(lessonSlot);
    }

    @Transactional
    public LessonSlotResponseDto createLessonSlot(LessonSlotRequestDto request) {
        validateTimeRange(request);

        SchoolClass schoolClass = schoolClassRepository.findById(request.getSchoolClassId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday sinf mavjud emas"));
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday fan mavjud emas"));
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday o'qituvchi mavjud emas"));
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday xona mavjud emas"));

        validateSameSchool(schoolClass, subject, employee, room);
        if (!subject.isActive()) {
            throw new IllegalStateException("«" + subject.getName() + "» fani nofaol — jadvalga qo'shib bo'lmaydi");
        }
        validateNoConflicts(request, null);

        LessonSlot lessonSlot = new LessonSlot();
        lessonSlot.setSchoolClass(schoolClass);
        lessonSlot.setSubject(subject);
        lessonSlot.setEmployee(employee);
        lessonSlot.setRoom(room);
        lessonSlot.setWeekday(request.getWeekday());
        lessonSlot.setStartTime(request.getStartTime());
        lessonSlot.setEndTime(request.getEndTime());

        LessonSlot saved = lessonSlotRepository.save(lessonSlot);
        botCache.evictAllClasses();
        evictPages(schoolClass);
        return toResponseDto(saved);
    }

    @Transactional
    public LessonSlotResponseDto updateLessonSlot(Long id, LessonSlotRequestDto request) {
        LessonSlot lessonSlot = lessonSlotRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday dars jadvali topilmadi: " + id));

        validateTimeRange(request);

        SchoolClass schoolClass = schoolClassRepository.findById(request.getSchoolClassId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday sinf mavjud emas"));
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday fan mavjud emas"));
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday o'qituvchi mavjud emas"));
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday xona mavjud emas"));

        validateSameSchool(schoolClass, subject, employee, room);
        if (!subject.isActive() && (lessonSlot.getSubject() == null || !lessonSlot.getSubject().getId().equals(subject.getId()))) {
            throw new IllegalStateException("«" + subject.getName() + "» fani nofaol — jadvalga qo'shib bo'lmaydi");
        }
        validateNoConflicts(request, id);

        lessonSlot.setSchoolClass(schoolClass);
        lessonSlot.setSubject(subject);
        lessonSlot.setEmployee(employee);
        lessonSlot.setRoom(room);
        lessonSlot.setWeekday(request.getWeekday());
        lessonSlot.setStartTime(request.getStartTime());
        lessonSlot.setEndTime(request.getEndTime());

        LessonSlot updated = lessonSlotRepository.save(lessonSlot);
        botCache.evictAllClasses();
        evictPages(schoolClass);
        return toResponseDto(updated);
    }

    @Transactional
    public void deleteLessonSlot(Long id) {
        LessonSlot existing = lessonSlotRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday dars jadvali topilmadi: " + id));
        evictPages(existing.getSchoolClass());
        substitutionRepository.deleteByLessonSlotId(id);
        lessonSlotRepository.deleteById(id);
        botCache.evictAllClasses();
    }

    private void validateTimeRange(LessonSlotRequestDto request) {
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new IllegalStateException("Boshlanish vaqti tugash vaqtidan oldin bo'lishi kerak");
        }
    }

    private void validateSameSchool(SchoolClass schoolClass, Subject subject, Employee employee, Room room) {
        Long schoolId = schoolClass.getAcademicYear().getSchool().getId();
        boolean sameSchool = subject.getSchool() != null && schoolId.equals(subject.getSchool().getId())
                && employee.getSchool() != null && schoolId.equals(employee.getSchool().getId())
                && schoolId.equals(room.getBuilding().getSchool().getId());
        if (!sameSchool) {
            throw new IllegalStateException("Sinf, fan, o'qituvchi va xona bitta maktabga tegishli bo'lishi kerak");
        }
    }

    /**
     * 409 with what exactly is in the way: "Karimova D. bu vaqtda 7-B da", "204-xona bu vaqtda band
     * (7-B, Fizika)", "5-A sinfida bu vaqtda dars bor (Ona tili)". The class first, then the teacher,
     * then the room — the drag-and-drop check (timetable.js) uses the same order and words.
     */
    private void validateNoConflicts(LessonSlotRequestDto request, Long excludeId) {
        List<LessonSlot> overlapping = lessonSlotRepository.findOverlapping(request.getWeekday(), request.getStartTime(),
                request.getEndTime(), excludeId, request.getRoomId(), request.getEmployeeId(), request.getSchoolClassId());
        for (LessonSlot l : overlapping) {
            if (l.getSchoolClass().getId().equals(request.getSchoolClassId())) {
                throw new IllegalStateException(className(l) + " sinfida bu vaqtda dars bor (" + l.getSubject().getName() + ")");
            }
        }
        for (LessonSlot l : overlapping) {
            if (l.getEmployee().getId().equals(request.getEmployeeId())) {
                throw new IllegalStateException(shortName(l.getEmployee()) + " bu vaqtda " + className(l) + " da");
            }
        }
        for (LessonSlot l : overlapping) {
            if (l.getRoom().getId().equals(request.getRoomId())) {
                throw new IllegalStateException(roomLabel(l.getRoom().getRoomNumber()) + " bu vaqtda band ("
                        + className(l) + ", " + l.getSubject().getName() + ")");
            }
        }
    }

    /** "Karimova D." */
    static String shortName(Employee e) {
        String first = e.getFirstName() == null || e.getFirstName().isBlank() ? "" : " " + e.getFirstName().charAt(0) + ".";
        return e.getLastName() + first;
    }

    private static String className(LessonSlot l) {
        return l.getSchoolClass().getGradeNumber() + "-" + l.getSchoolClass().getSectionLetter();
    }

    /** "204-xona", a named room as it is. */
    static String roomLabel(String number) {
        if (number == null || number.isEmpty()) return "Xona";
        return Character.isDigit(number.charAt(0)) ? number + "-xona" : number;
    }

    private LessonSlotResponseDto toResponseDto(LessonSlot lessonSlot) {
        LessonSlotResponseDto dto = new LessonSlotResponseDto();
        dto.setId(lessonSlot.getId());
        dto.setSchoolClassId(lessonSlot.getSchoolClass().getId());
        dto.setClassName(lessonSlot.getSchoolClass().getGradeNumber() + "-" + lessonSlot.getSchoolClass().getSectionLetter());
        dto.setSubjectId(lessonSlot.getSubject().getId());
        dto.setSubjectName(lessonSlot.getSubject().getName());
        dto.setEmployeeId(lessonSlot.getEmployee().getId());
        dto.setEmployeeName(lessonSlot.getEmployee().getFirstName() + " " + lessonSlot.getEmployee().getLastName());
        dto.setRoomId(lessonSlot.getRoom().getId());
        dto.setRoomNumber(lessonSlot.getRoom().getRoomNumber());
        dto.setWeekday(lessonSlot.getWeekday());
        dto.setStartTime(lessonSlot.getStartTime());
        dto.setEndTime(lessonSlot.getEndTime());
        return dto;
    }
}
