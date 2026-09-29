package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.*;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private BehaviorRecordRepository behaviorRecordRepository;

    @Autowired
    private LessonSlotRepository lessonSlotRepository;

    public StudentProfileDto getStudentProfile(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalStateException("Bunday o'quvchi topilmadi: " + studentId));

        StudentProfileDto dto = new StudentProfileDto();
        dto.setId(student.getId());
        dto.setFirstName(student.getFirstName());
        dto.setLastName(student.getLastName());
        dto.setFullName(student.getFirstName() + " " + student.getLastName());
        dto.setBirthDate(student.getBirthDate());
        dto.setAge(Period.between(student.getBirthDate(), LocalDate.now()).getYears());
        dto.setSchoolClassId(student.getSchoolClass().getId());
        dto.setClassName(student.getSchoolClass().getGradeNumber() + "-" + student.getSchoolClass().getSectionLetter());
        dto.setGuardianName(student.getGuardianName());
        dto.setGuardianPhone(student.getGuardianPhone());

        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(89);
        List<Attendance> attendanceHistory = attendanceRepository
                .findByStudentIdAndRecordDateBetweenOrderByRecordDate(studentId, from, today);

        long presentOrLate = attendanceHistory.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.LATE)
                .count();
        dto.setAttendanceRate(attendanceHistory.isEmpty() ? null
                : Math.round(presentOrLate * 1000.0 / attendanceHistory.size()) / 10.0);

        Map<LocalDate, List<Attendance>> byDate = attendanceHistory.stream()
                .collect(Collectors.groupingBy(Attendance::getRecordDate));
        List<HeatmapDayDto> heatmap = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {
            List<Attendance> dayRecords = byDate.get(d);
            String status;
            if (dayRecords == null || dayRecords.isEmpty()) {
                status = "NONE";
            } else {
                long absent = dayRecords.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();
                long late = dayRecords.stream().filter(a -> a.getStatus() == AttendanceStatus.LATE).count();
                if (absent == dayRecords.size()) status = "ABSENT";
                else if (absent > 0) status = "PARTIAL";
                else if (late > 0) status = "LATE";
                else status = "PRESENT";
            }
            heatmap.add(new HeatmapDayDto(d, status));
        }
        dto.setAttendanceHeatmap(heatmap);

        List<Grade> grades = gradeRepository.findByStudentIdOrderByGradeDateDesc(studentId);
        dto.setAverageGrade(grades.isEmpty() ? null
                : Math.round(grades.stream().mapToInt(Grade::getScore).average().orElse(0) * 100) / 100.0);

        List<SubjectAverageDto> subjectAverages = new ArrayList<>();
        for (Object[] row : gradeRepository.averageScoreByStudentGroupedBySubject(studentId)) {
            SubjectAverageDto sa = new SubjectAverageDto();
            sa.setSubjectId(((Number) row[0]).longValue());
            sa.setSubjectName((String) row[1]);
            Double avg = (Double) row[2];
            sa.setAverageScore(avg == null ? null : Math.round(avg * 100) / 100.0);
            subjectAverages.add(sa);
        }
        dto.setSubjectAverages(subjectAverages);

        dto.setRewardCount(behaviorRecordRepository.countByStudentIdAndType(studentId, BehaviorType.REWARD));
        dto.setWarningCount(behaviorRecordRepository.countByStudentIdAndType(studentId, BehaviorType.WARNING));

        List<BehaviorRecordResponseDto> behaviorDtos = behaviorRecordRepository.findByStudentIdOrderByRecordDateDesc(studentId)
                .stream().map(this::toBehaviorDto).collect(Collectors.toList());
        dto.setBehaviorRecords(behaviorDtos);

        List<LessonSlot> lessons = lessonSlotRepository.findBySchoolClassId(student.getSchoolClass().getId());
        dto.setWeeklySchedule(lessons.stream().map(this::toTimetableEntryDto).collect(Collectors.toList()));

        return dto;
    }

    public TeacherProfileDto getTeacherProfile(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalStateException("Bunday xodim topilmadi: " + employeeId));

        TeacherProfileDto dto = new TeacherProfileDto();
        dto.setId(employee.getId());
        dto.setFirstName(employee.getFirstName());
        dto.setLastName(employee.getLastName());
        dto.setFullName(employee.getFirstName() + " " + employee.getLastName());
        dto.setPhone(employee.getPhone());
        dto.setPositionTitle(employee.getPosition().getTitle());

        List<LessonSlot> lessons = lessonSlotRepository.findByEmployeeId(employeeId);
        dto.setWeeklyLoadCount((long) lessons.size());
        dto.setSubjects(lessons.stream().map(l -> l.getSubject().getName()).distinct().sorted().collect(Collectors.toList()));
        dto.setClasses(lessons.stream()
                .map(l -> l.getSchoolClass().getGradeNumber() + "-" + l.getSchoolClass().getSectionLetter())
                .distinct().sorted().collect(Collectors.toList()));
        dto.setWeeklySchedule(lessons.stream().map(this::toTimetableEntryDto).collect(Collectors.toList()));

        String weekday = weekdayName(LocalDate.now());
        dto.setTodayLessons(lessons.stream()
                .filter(l -> l.getWeekday().equals(weekday))
                .sorted(Comparator.comparing(LessonSlot::getStartTime))
                .map(this::toTimetableEntryDto).collect(Collectors.toList()));

        schoolClassRepository.findByClassTeacherId(employeeId).ifPresent(sc ->
                dto.setHomeroomClassName(sc.getGradeNumber() + "-" + sc.getSectionLetter()));

        return dto;
    }

    public ClassProfileDto getClassProfile(Long schoolClassId) {
        SchoolClass schoolClass = schoolClassRepository.findById(schoolClassId)
                .orElseThrow(() -> new IllegalStateException("Bunday sinf topilmadi: " + schoolClassId));

        ClassProfileDto dto = new ClassProfileDto();
        dto.setId(schoolClass.getId());
        dto.setClassName(schoolClass.getGradeNumber() + "-" + schoolClass.getSectionLetter());
        dto.setAcademicYearTitle(schoolClass.getAcademicYear().getTitle());
        if (schoolClass.getClassTeacher() != null) {
            dto.setClassTeacherId(schoolClass.getClassTeacher().getId());
            dto.setClassTeacherName(schoolClass.getClassTeacher().getFirstName() + " " + schoolClass.getClassTeacher().getLastName());
        }

        List<Student> students = studentRepository.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(schoolClassId);
        dto.setStudentCount((long) students.size());
        dto.setStudents(students.stream().map(s -> {
            StudentResponseDto sdto = new StudentResponseDto();
            sdto.setId(s.getId());
            sdto.setFirstName(s.getFirstName());
            sdto.setLastName(s.getLastName());
            sdto.setFullName(s.getFirstName() + " " + s.getLastName());
            sdto.setBirthDate(s.getBirthDate());
            sdto.setSchoolClassId(schoolClassId);
            sdto.setClassName(dto.getClassName());
            sdto.setGuardianName(s.getGuardianName());
            sdto.setGuardianPhone(s.getGuardianPhone());
            return sdto;
        }).collect(Collectors.toList()));

        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(30);
        long totalAttendance = 0;
        long presentAttendance = 0;
        double gradeSum = 0;
        long gradeCount = 0;
        for (Student s : students) {
            List<Attendance> hist = attendanceRepository.findByStudentIdAndRecordDateBetweenOrderByRecordDate(s.getId(), from, today);
            totalAttendance += hist.size();
            presentAttendance += hist.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.LATE).count();
            List<Grade> grades = gradeRepository.findByStudentIdOrderByGradeDateDesc(s.getId());
            gradeSum += grades.stream().mapToInt(Grade::getScore).sum();
            gradeCount += grades.size();
        }
        dto.setAttendanceRate(totalAttendance == 0 ? null : Math.round(presentAttendance * 1000.0 / totalAttendance) / 10.0);
        dto.setAverageGrade(gradeCount == 0 ? null : Math.round(gradeSum / gradeCount * 100) / 100.0);

        List<LessonSlot> lessons = lessonSlotRepository.findBySchoolClassId(schoolClassId);
        dto.setWeeklySchedule(lessons.stream().map(this::toTimetableEntryDto).collect(Collectors.toList()));

        return dto;
    }

    private static final Map<java.time.DayOfWeek, String> WEEKDAY_NAMES = Map.of(
            java.time.DayOfWeek.MONDAY, "Dushanba", java.time.DayOfWeek.TUESDAY, "Seshanba",
            java.time.DayOfWeek.WEDNESDAY, "Chorshanba", java.time.DayOfWeek.THURSDAY, "Payshanba",
            java.time.DayOfWeek.FRIDAY, "Juma", java.time.DayOfWeek.SATURDAY, "Shanba");

    private String weekdayName(LocalDate date) {
        return WEEKDAY_NAMES.get(date.getDayOfWeek());
    }

    private BehaviorRecordResponseDto toBehaviorDto(BehaviorRecord record) {
        BehaviorRecordResponseDto dto = new BehaviorRecordResponseDto();
        dto.setId(record.getId());
        dto.setStudentId(record.getStudent().getId());
        dto.setStudentName(record.getStudent().getFirstName() + " " + record.getStudent().getLastName());
        dto.setRecordDate(record.getRecordDate());
        dto.setType(record.getType());
        dto.setDescription(record.getDescription());
        return dto;
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
}
