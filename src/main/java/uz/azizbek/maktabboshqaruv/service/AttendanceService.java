package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.AttendanceBulkRequestDto;
import uz.azizbek.maktabboshqaruv.dto.AttendanceRequestDto;
import uz.azizbek.maktabboshqaruv.dto.AttendanceResponseDto;
import uz.azizbek.maktabboshqaruv.dto.AttendanceRosterEntryDto;
import uz.azizbek.maktabboshqaruv.dto.TodayLessonDto;
import uz.azizbek.maktabboshqaruv.entity.Attendance;
import uz.azizbek.maktabboshqaruv.entity.AttendanceStatus;
import uz.azizbek.maktabboshqaruv.event.AttendanceMarkedEvent;
import org.springframework.context.ApplicationEventPublisher;
import uz.azizbek.maktabboshqaruv.entity.LessonSlot;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.repository.AttendanceRepository;
import uz.azizbek.maktabboshqaruv.repository.LessonSlotRepository;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Tashkent");

    private static final Map<DayOfWeek, String> WEEKDAY_NAMES = Map.of(
            DayOfWeek.MONDAY, "Dushanba", DayOfWeek.TUESDAY, "Seshanba", DayOfWeek.WEDNESDAY, "Chorshanba",
            DayOfWeek.THURSDAY, "Payshanba", DayOfWeek.FRIDAY, "Juma", DayOfWeek.SATURDAY, "Shanba");

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private LessonSlotRepository lessonSlotRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public Page<AttendanceResponseDto> getAllAttendance(Long schoolId, Pageable pageable) {
        return attendanceRepository.findBySchoolId(schoolId, pageable).map(this::toResponseDto);
    }

    public List<TodayLessonDto> getTodayLessons(Long schoolId, Long employeeId) {
        LocalDate today = LocalDate.now(ZONE);
        String weekday = WEEKDAY_NAMES.get(today.getDayOfWeek());
        if (weekday == null) return List.of();

        LocalTime now = LocalTime.now(ZONE);
        return lessonSlotRepository.findBySchoolIdAndWeekday(schoolId, weekday).stream()
                .filter(ls -> employeeId == null || employeeId.equals(ls.getEmployee().getId()))
                .map(ls -> {
                    TodayLessonDto dto = new TodayLessonDto();
                    dto.setLessonSlotId(ls.getId());
                    dto.setSchoolClassId(ls.getSchoolClass().getId());
                    dto.setClassName(ls.getSchoolClass().getGradeNumber() + "-" + ls.getSchoolClass().getSectionLetter());
                    dto.setSubjectName(ls.getSubject().getName());
                    dto.setTeacherName(ls.getEmployee().getFirstName() + " " + ls.getEmployee().getLastName());
                    dto.setStartTime(ls.getStartTime().toString());
                    dto.setEndTime(ls.getEndTime().toString());
                    if (!now.isBefore(ls.getStartTime()) && now.isBefore(ls.getEndTime())) {
                        dto.setStatus("HOZIR");
                    } else if (now.isBefore(ls.getStartTime())) {
                        dto.setStatus("KEYINGI");
                    } else {
                        dto.setStatus("OTGAN");
                    }
                    dto.setAttendanceTaken(attendanceRepository.existsByLessonSlotIdAndRecordDate(ls.getId(), today));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public List<AttendanceRosterEntryDto> getRoster(Long lessonSlotId, LocalDate date) {
        LessonSlot lessonSlot = lessonSlotRepository.findById(lessonSlotId)
                .orElseThrow(() -> new IllegalStateException("Bunday dars mavjud emas"));

        List<Student> students = studentRepository
                .findBySchoolClassIdOrderByLastNameAscFirstNameAsc(lessonSlot.getSchoolClass().getId());

        Map<Long, Attendance> existing = new HashMap<>();
        for (Attendance a : attendanceRepository.findByLessonSlotIdAndRecordDate(lessonSlotId, date)) {
            existing.put(a.getStudent().getId(), a);
        }

        List<AttendanceRosterEntryDto> roster = new ArrayList<>();
        for (Student s : students) {
            AttendanceRosterEntryDto dto = new AttendanceRosterEntryDto();
            dto.setStudentId(s.getId());
            dto.setStudentName(s.getFirstName() + " " + s.getLastName());
            Attendance a = existing.get(s.getId());
            if (a != null) {
                dto.setAttendanceId(a.getId());
                dto.setStatus(a.getStatus());
                dto.setComment(a.getComment());
            }
            roster.add(dto);
        }
        return roster;
    }

    @Transactional
    public List<AttendanceRosterEntryDto> bulkMark(AttendanceBulkRequestDto request) {
        LessonSlot lessonSlot = lessonSlotRepository.findById(request.getLessonSlotId())
                .orElseThrow(() -> new IllegalStateException("Bunday dars mavjud emas"));

        Map<Long, Attendance> existing = new HashMap<>();
        for (Attendance a : attendanceRepository.findByLessonSlotIdAndRecordDate(request.getLessonSlotId(), request.getRecordDate())) {
            existing.put(a.getStudent().getId(), a);
        }

        for (AttendanceBulkRequestDto.Entry entry : request.getEntries()) {
            Student student = studentRepository.findById(entry.getStudentId())
                    .orElseThrow(() -> new IllegalStateException("Bunday o'quvchi mavjud emas: " + entry.getStudentId()));
            if (!student.getSchoolClass().getId().equals(lessonSlot.getSchoolClass().getId())) {
                throw new IllegalStateException("O'quvchi ushbu darsning sinfiga tegishli emas: " + entry.getStudentId());
            }

            Attendance attendance = existing.get(entry.getStudentId());
            if (attendance == null) {
                attendance = new Attendance();
                attendance.setLessonSlot(lessonSlot);
                attendance.setStudent(student);
                attendance.setRecordDate(request.getRecordDate());
            }
            AttendanceStatus previousStatus = attendance.getStatus();
            attendance.setStatus(entry.getStatus());
            attendance.setComment(entry.getComment());
            Attendance saved = attendanceRepository.save(attendance);
            publishIfNotifiable(saved, previousStatus);
        }

        String className = lessonSlot.getSchoolClass().getGradeNumber() + "-" + lessonSlot.getSchoolClass().getSectionLetter();
        activityLogService.record(
                lessonSlot.getSchoolClass().getAcademicYear().getSchool(),
                "fact_check",
                className + " sinfida " + lessonSlot.getSubject().getName() + " darsidan davomat olindi");

        return getRoster(request.getLessonSlotId(), request.getRecordDate());
    }

    @Transactional
    public AttendanceResponseDto createAttendance(AttendanceRequestDto request) {
        LessonSlot lessonSlot = lessonSlotRepository.findById(request.getLessonSlotId())
                .orElseThrow(() -> new IllegalStateException("Bunday dars mavjud emas"));
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new IllegalStateException("Bunday o'quvchi mavjud emas"));

        if (!student.getSchoolClass().getId().equals(lessonSlot.getSchoolClass().getId())) {
            throw new IllegalStateException("O'quvchi ushbu darsning sinfiga tegishli emas");
        }
        if (attendanceRepository.existsByLessonSlotIdAndStudentIdAndRecordDate(
                request.getLessonSlotId(), request.getStudentId(), request.getRecordDate())) {
            throw new IllegalStateException("Bu o'quvchi uchun ushbu dars va sanada davomat allaqachon mavjud");
        }

        Attendance attendance = new Attendance();
        attendance.setLessonSlot(lessonSlot);
        attendance.setStudent(student);
        attendance.setRecordDate(request.getRecordDate());
        attendance.setStatus(request.getStatus());
        attendance.setComment(request.getComment());

        Attendance saved = attendanceRepository.save(attendance);
        publishIfNotifiable(saved, null);
        return toResponseDto(saved);
    }

    @Transactional
    public AttendanceResponseDto updateAttendance(Long id, AttendanceRequestDto request) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday davomat yozuvi topilmadi: " + id));

        AttendanceStatus previousStatus = attendance.getStatus();
        attendance.setStatus(request.getStatus());
        attendance.setComment(request.getComment());

        Attendance updated = attendanceRepository.save(attendance);
        publishIfNotifiable(updated, previousStatus);
        return toResponseDto(updated);
    }

    // Re-saving a roster with unchanged statuses must not re-notify parents;
    // only a change into ABSENT/LATE (e.g. PRESENT -> ABSENT) does.
    void publishIfNotifiable(Attendance attendance, AttendanceStatus previousStatus) {
        if (AttendanceMarkedEvent.isNotifiable(previousStatus, attendance.getStatus())) {
            eventPublisher.publishEvent(new AttendanceMarkedEvent(attendance.getId(), previousStatus, attendance.getStatus()));
        }
    }

    @Transactional
    public void deleteAttendance(Long id) {
        if (!attendanceRepository.existsById(id)) {
            throw new IllegalStateException("Bunday davomat yozuvi topilmadi: " + id);
        }
        attendanceRepository.deleteById(id);
    }

    private AttendanceResponseDto toResponseDto(Attendance attendance) {
        AttendanceResponseDto dto = new AttendanceResponseDto();
        dto.setId(attendance.getId());
        dto.setLessonSlotId(attendance.getLessonSlot().getId());
        dto.setClassName(attendance.getLessonSlot().getSchoolClass().getGradeNumber() + "-"
                + attendance.getLessonSlot().getSchoolClass().getSectionLetter());
        dto.setSubjectName(attendance.getLessonSlot().getSubject().getName());
        dto.setStudentId(attendance.getStudent().getId());
        dto.setStudentName(attendance.getStudent().getFirstName() + " " + attendance.getStudent().getLastName());
        dto.setRecordDate(attendance.getRecordDate());
        dto.setStatus(attendance.getStatus());
        dto.setComment(attendance.getComment());
        return dto;
    }
}
