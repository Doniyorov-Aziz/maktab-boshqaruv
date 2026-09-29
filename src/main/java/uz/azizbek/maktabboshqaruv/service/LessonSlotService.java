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

@Service
public class LessonSlotService {

    @Autowired
    private LessonSlotRepository lessonSlotRepository;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private RoomRepository roomRepository;

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
        dto.setTeacherId(lessonSlot.getEmployee().getId());
        dto.setTeacherName(lessonSlot.getEmployee().getFirstName() + " " + lessonSlot.getEmployee().getLastName());
        dto.setRoomId(lessonSlot.getRoom().getId());
        dto.setRoomNumber(lessonSlot.getRoom().getRoomNumber());
        dto.setWeekday(lessonSlot.getWeekday());
        dto.setStartTime(lessonSlot.getStartTime());
        dto.setEndTime(lessonSlot.getEndTime());
        return dto;
    }

    public LessonSlotResponseDto getLessonSlotById(Long id) {
        LessonSlot lessonSlot = lessonSlotRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday dars jadvali topilmadi: " + id));
        return toResponseDto(lessonSlot);
    }

    @Transactional
    public LessonSlotResponseDto createLessonSlot(LessonSlotRequestDto request) {
        validateTimeRange(request);

        SchoolClass schoolClass = schoolClassRepository.findById(request.getSchoolClassId())
                .orElseThrow(() -> new IllegalStateException("Bunday sinf mavjud emas"));
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new IllegalStateException("Bunday fan mavjud emas"));
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new IllegalStateException("Bunday o'qituvchi mavjud emas"));
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new IllegalStateException("Bunday xona mavjud emas"));

        validateSameSchool(schoolClass, subject, employee, room);
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
        return toResponseDto(saved);
    }

    @Transactional
    public LessonSlotResponseDto updateLessonSlot(Long id, LessonSlotRequestDto request) {
        LessonSlot lessonSlot = lessonSlotRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday dars jadvali topilmadi: " + id));

        validateTimeRange(request);

        SchoolClass schoolClass = schoolClassRepository.findById(request.getSchoolClassId())
                .orElseThrow(() -> new IllegalStateException("Bunday sinf mavjud emas"));
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new IllegalStateException("Bunday fan mavjud emas"));
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new IllegalStateException("Bunday o'qituvchi mavjud emas"));
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new IllegalStateException("Bunday xona mavjud emas"));

        validateSameSchool(schoolClass, subject, employee, room);
        validateNoConflicts(request, id);

        lessonSlot.setSchoolClass(schoolClass);
        lessonSlot.setSubject(subject);
        lessonSlot.setEmployee(employee);
        lessonSlot.setRoom(room);
        lessonSlot.setWeekday(request.getWeekday());
        lessonSlot.setStartTime(request.getStartTime());
        lessonSlot.setEndTime(request.getEndTime());

        LessonSlot updated = lessonSlotRepository.save(lessonSlot);
        return toResponseDto(updated);
    }

    @Transactional
    public void deleteLessonSlot(Long id) {
        if (!lessonSlotRepository.existsById(id)) {
            throw new IllegalStateException("Bunday dars jadvali topilmadi: " + id);
        }
        lessonSlotRepository.deleteById(id);
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

    private void validateNoConflicts(LessonSlotRequestDto request, Long excludeId) {
        if (lessonSlotRepository.existsRoomConflict(request.getRoomId(), request.getWeekday(),
                request.getStartTime(), request.getEndTime(), excludeId)) {
            throw new IllegalStateException("Bu xona ushbu vaqtda band");
        }
        if (lessonSlotRepository.existsEmployeeConflict(request.getEmployeeId(), request.getWeekday(),
                request.getStartTime(), request.getEndTime(), excludeId)) {
            throw new IllegalStateException("Bu o'qituvchi ushbu vaqtda band");
        }
        if (lessonSlotRepository.existsSchoolClassConflict(request.getSchoolClassId(), request.getWeekday(),
                request.getStartTime(), request.getEndTime(), excludeId)) {
            throw new IllegalStateException("Bu sinf ushbu vaqtda band");
        }
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
