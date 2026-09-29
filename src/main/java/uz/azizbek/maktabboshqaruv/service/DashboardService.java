package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.*;
import uz.azizbek.maktabboshqaruv.entity.LessonSlot;
import uz.azizbek.maktabboshqaruv.entity.SchoolClass;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Tashkent");

    private static final Map<DayOfWeek, String> WEEKDAY_NAMES = Map.of(
            DayOfWeek.MONDAY, "Dushanba", DayOfWeek.TUESDAY, "Seshanba", DayOfWeek.WEDNESDAY, "Chorshanba",
            DayOfWeek.THURSDAY, "Payshanba", DayOfWeek.FRIDAY, "Juma", DayOfWeek.SATURDAY, "Shanba");

    private static final List<String> TEACHER_POSITIONS = List.of("Fan o'qituvchisi", "Sinf rahbari");

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LessonSlotRepository lessonSlotRepository;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private BehaviorRecordRepository behaviorRecordRepository;

    @Autowired
    private CalendarEventRepository calendarEventRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    private String weekdayName(LocalDate date) {
        return WEEKDAY_NAMES.get(date.getDayOfWeek());
    }

    /** Only Sunday is a non-working day for this school calendar. */
    private boolean isWorkingDay(LocalDate date) {
        return date.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    /** Last {@code count} working days (Mon-Sat) up to and including {@code endInclusive}, oldest first. */
    private List<LocalDate> lastWorkingDays(LocalDate endInclusive, int count) {
        List<LocalDate> days = new ArrayList<>();
        LocalDate d = endInclusive;
        while (days.size() < count) {
            if (isWorkingDay(d)) {
                days.add(0, d);
            }
            d = d.minusDays(1);
        }
        return days;
    }

    /** Attendance rate for the day, counted only among classes that have actually recorded attendance; null when nothing recorded yet. */
    private Double attendanceRateOn(Long schoolId, LocalDate date) {
        long total = attendanceRepository.countBySchoolIdAndRecordDate(schoolId, date);
        if (total == 0) return null;
        long present = attendanceRepository.countPresentBySchoolIdAndRecordDate(schoolId, date);
        return Math.round(present * 1000.0 / total) / 10.0;
    }

    public DashboardSummaryDto getSummary(Long schoolId) {
        DashboardSummaryDto dto = new DashboardSummaryDto();
        LocalDate today = LocalDate.now(ZONE);
        LocalDate yesterday = today.minusDays(1);
        LocalDateTime now = LocalDateTime.now(ZONE);
        LocalDateTime sameTimeYesterday = yesterday.atTime(now.toLocalTime());

        long totalClasses = schoolClassRepository.countByAcademicYearSchoolId(schoolId);
        long classesWithAttendanceToday = attendanceRepository.countDistinctClassesBySchoolIdAndRecordDate(schoolId, today);
        dto.setTotalClasses(totalClasses);
        dto.setClassesWithAttendanceToday(classesWithAttendanceToday);

        Double todayRate = attendanceRateOn(schoolId, today);
        dto.setTodayAttendanceRate(todayRate == null ? 0.0 : todayRate);

        Double yesterdaySameTimeRate = rateFromRows(attendanceRepository.attendanceRateAsOf(schoolId, yesterday, sameTimeYesterday));
        if (todayRate != null && yesterdaySameTimeRate != null) {
            dto.setAttendanceRateDelta(Math.round((todayRate - yesterdaySameTimeRate) * 10) / 10.0);
        }

        long todayAbsent = attendanceRepository.countAbsentBySchoolIdAndRecordDate(schoolId, today);
        long yesterdayAbsent = attendanceRepository.countAbsentBySchoolIdAndRecordDate(schoolId, yesterday);
        dto.setTodayAbsentCount(todayAbsent);
        dto.setAbsentCountDelta(todayAbsent - yesterdayAbsent);

        List<LocalDate> workingDays = lastWorkingDays(today, 14);
        List<Double> attendanceTrend = new ArrayList<>();
        List<Long> absentTrend = new ArrayList<>();
        for (LocalDate d : workingDays) {
            attendanceTrend.add(attendanceRateOn(schoolId, d));
            long total = attendanceRepository.countBySchoolIdAndRecordDate(schoolId, d);
            absentTrend.add(total == 0 ? null : attendanceRepository.countAbsentBySchoolIdAndRecordDate(schoolId, d));
        }
        dto.setAttendanceTrend14(attendanceTrend);
        dto.setAbsentTrend14(absentTrend);

        Double avgGrade = gradeRepository.averageScoreBySchoolIdBetween(schoolId, today.minusDays(60), today);
        dto.setAverageGrade(avgGrade == null ? null : Math.round(avgGrade * 100) / 100.0);

        Double lastPeriodAvg = gradeRepository.averageScoreBySchoolIdBetween(schoolId, today.minusDays(7), today);
        Double prevPeriodAvg = gradeRepository.averageScoreBySchoolIdBetween(schoolId, today.minusDays(14), today.minusDays(7));
        if (lastPeriodAvg != null && prevPeriodAvg != null) {
            dto.setAverageGradeDelta(Math.round((lastPeriodAvg - prevPeriodAvg) * 100) / 100.0);
        }

        Map<LocalDate, Double> gradeByDate = new HashMap<>();
        for (Object[] row : gradeRepository.dailyAverageTrend(schoolId, workingDays.get(0), today)) {
            gradeByDate.put((LocalDate) row[0], (Double) row[1]);
        }
        List<Double> gradeTrend = new ArrayList<>();
        for (LocalDate d : workingDays) {
            Double v = gradeByDate.get(d);
            gradeTrend.add(v == null ? null : Math.round(v * 100) / 100.0);
        }
        dto.setGradeTrend14(gradeTrend);

        dto.setTotalStudents(studentRepository.countBySchoolClassAcademicYearSchoolId(schoolId));
        dto.setTotalTeachers(employeeRepository.countBySchoolIdAndPositionTitleIn(schoolId, TEACHER_POSITIONS));

        return dto;
    }

    private Double rateFromRows(List<Object[]> rows) {
        if (rows.isEmpty()) return null;
        long total = ((Number) rows.get(0)[1]).longValue();
        if (total == 0) return null;
        long present = ((Number) rows.get(0)[0]).longValue();
        return Math.round(present * 1000.0 / total) / 10.0;
    }

    public List<LiveLessonDto> getLiveLessons(Long schoolId) {
        LocalDate today = LocalDate.now(ZONE);
        String weekday = weekdayName(today);
        List<LiveLessonDto> result = new ArrayList<>();
        if (weekday == null) return result;

        LocalTime now = LocalTime.now(ZONE);

        for (LessonSlot ls : lessonSlotRepository.findCurrentlyInSession(schoolId, weekday, now)) {
            LiveLessonDto dto = toLiveLessonDto(ls);
            dto.setStatus("HOZIR");
            dto.setMinutesUntilOrRemaining(Duration.between(now, ls.getEndTime()).toMinutes());
            result.add(dto);
        }

        List<LessonSlot> upcoming = lessonSlotRepository.findUpcomingToday(schoolId, weekday, now);
        if (!upcoming.isEmpty()) {
            LessonSlot next = upcoming.get(0);
            LiveLessonDto dto = toLiveLessonDto(next);
            dto.setStatus("KEYINGI");
            dto.setMinutesUntilOrRemaining(Duration.between(now, next.getStartTime()).toMinutes());
            result.add(dto);
        }

        return result;
    }

    /**
     * Header line for the live-lessons table: which period it is right now
     * (derived from the distinct start/end-time grid for today), how many
     * classes are currently in a lesson, and minutes until the next
     * transition (break start or next lesson start).
     */
    public LiveLessonsSummaryDto getLiveLessonsSummary(Long schoolId) {
        LiveLessonsSummaryDto dto = new LiveLessonsSummaryDto();
        LocalDate today = LocalDate.now(ZONE);
        String weekday = weekdayName(today);
        if (weekday == null) {
            dto.setPeriodLabel("Dam olish kuni");
            dto.setClassesInSession(0);
            return dto;
        }

        LocalTime now = LocalTime.now(ZONE);
        List<Object[]> periods = lessonSlotRepository.findDistinctPeriodsForWeekday(schoolId, weekday);
        dto.setClassesInSession(lessonSlotRepository.findCurrentlyInSession(schoolId, weekday, now).size());

        if (periods.isEmpty()) {
            dto.setPeriodLabel("Darslar yo'q");
            return dto;
        }

        for (int i = 0; i < periods.size(); i++) {
            LocalTime start = (LocalTime) periods.get(i)[0];
            LocalTime end = (LocalTime) periods.get(i)[1];
            if (!now.isBefore(start) && now.isBefore(end)) {
                dto.setPeriodLabel((i + 1) + "-dars");
                dto.setTransitionLabel("tanaffusgacha");
                dto.setMinutesToTransition(Duration.between(now, end).toMinutes());
                return dto;
            }
        }

        LocalTime firstStart = (LocalTime) periods.get(0)[0];
        LocalTime lastEnd = (LocalTime) periods.get(periods.size() - 1)[1];
        if (now.isBefore(firstStart)) {
            dto.setPeriodLabel("Darslar hali boshlanmagan");
            dto.setTransitionLabel("1-darsgacha");
            dto.setMinutesToTransition(Duration.between(now, firstStart).toMinutes());
        } else if (!now.isBefore(lastEnd)) {
            dto.setPeriodLabel("Darslar tugadi");
        } else {
            for (int i = 0; i < periods.size(); i++) {
                LocalTime end = (LocalTime) periods.get(i)[1];
                if (now.isBefore(end)) continue;
                if (i + 1 < periods.size()) {
                    LocalTime nextStart = (LocalTime) periods.get(i + 1)[0];
                    if (now.isBefore(nextStart)) {
                        dto.setPeriodLabel("Tanaffus");
                        dto.setTransitionLabel((i + 2) + "-darsgacha");
                        dto.setMinutesToTransition(Duration.between(now, nextStart).toMinutes());
                        break;
                    }
                }
            }
        }
        return dto;
    }

    private LiveLessonDto toLiveLessonDto(LessonSlot ls) {
        LiveLessonDto dto = new LiveLessonDto();
        dto.setLessonSlotId(ls.getId());
        dto.setClassName(ls.getSchoolClass().getGradeNumber() + "-" + ls.getSchoolClass().getSectionLetter());
        dto.setSubjectName(ls.getSubject().getName());
        dto.setTeacherName(ls.getEmployee().getFirstName() + " " + ls.getEmployee().getLastName());
        dto.setRoomNumber(ls.getRoom().getRoomNumber());
        dto.setStartTime(ls.getStartTime());
        dto.setEndTime(ls.getEndTime());
        return dto;
    }

    public List<AttendanceTrendPointDto> getAttendanceTrend(Long schoolId, int days) {
        LocalDate today = LocalDate.now(ZONE);
        List<LocalDate> workingDays = lastWorkingDays(today, days);
        LocalDate from = workingDays.get(0);
        Map<LocalDate, double[]> byDate = new HashMap<>();
        for (Object[] row : attendanceRepository.dailyAttendanceTrend(schoolId, from, today)) {
            LocalDate d = (LocalDate) row[0];
            long present = ((Number) row[1]).longValue();
            long total = ((Number) row[2]).longValue();
            byDate.put(d, new double[]{present, total});
        }
        List<AttendanceTrendPointDto> result = new ArrayList<>();
        for (LocalDate d : workingDays) {
            double[] v = byDate.get(d);
            Double rate = v == null ? null : (v[1] == 0 ? null : Math.round(v[0] * 1000.0 / v[1]) / 10.0);
            result.add(new AttendanceTrendPointDto(d, rate));
        }
        return result;
    }

    public List<ClassRankingDto> getClassRankings(Long schoolId) {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate from = today.minusDays(30);

        Map<Long, String> classNames = schoolClassRepository.findByAcademicYearSchoolId(schoolId).stream()
                .collect(Collectors.toMap(SchoolClass::getId, c -> c.getGradeNumber() + "-" + c.getSectionLetter()));

        Map<Long, Double> attendanceByClass = new HashMap<>();
        for (Object[] row : attendanceRepository.attendanceRateByClass(schoolId, from, today)) {
            Long classId = ((Number) row[0]).longValue();
            long present = ((Number) row[1]).longValue();
            long total = ((Number) row[2]).longValue();
            attendanceByClass.put(classId, total == 0 ? 0 : Math.round(present * 1000.0 / total) / 10.0);
        }

        Map<Long, Double> gradeByClass = new HashMap<>();
        for (Object[] row : gradeRepository.averageScoreByClass(schoolId)) {
            Long classId = ((Number) row[0]).longValue();
            Double avg = (Double) row[1];
            gradeByClass.put(classId, avg == null ? null : Math.round(avg * 100) / 100.0);
        }

        List<ClassRankingDto> result = new ArrayList<>();
        for (Map.Entry<Long, String> e : classNames.entrySet()) {
            ClassRankingDto dto = new ClassRankingDto();
            dto.setSchoolClassId(e.getKey());
            dto.setClassName(e.getValue());
            dto.setAttendanceRate(attendanceByClass.getOrDefault(e.getKey(), null));
            dto.setAverageGrade(gradeByClass.get(e.getKey()));
            result.add(dto);
        }
        result.sort((a, b) -> {
            double ra = a.getAttendanceRate() == null ? -1 : a.getAttendanceRate();
            double rb = b.getAttendanceRate() == null ? -1 : b.getAttendanceRate();
            return Double.compare(rb, ra);
        });
        return result;
    }

    public List<SubjectAverageDto> getSubjectAverages(Long schoolId) {
        List<SubjectAverageDto> result = new ArrayList<>();
        for (Object[] row : gradeRepository.averageScoreBySubject(schoolId)) {
            SubjectAverageDto dto = new SubjectAverageDto();
            dto.setSubjectId(((Number) row[0]).longValue());
            dto.setSubjectName((String) row[1]);
            Double avg = (Double) row[2];
            dto.setAverageScore(avg == null ? null : Math.round(avg * 100) / 100.0);
            result.add(dto);
        }
        return result;
    }

    public List<AttentionItemDto> getAttentionItems(Long schoolId) {
        List<AttentionItemDto> items = new ArrayList<>();
        LocalDate today = LocalDate.now(ZONE);

        Map<Long, Student> studentsById = studentRepository.findBySchoolClassAcademicYearSchoolId(schoolId).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));

        // 1) 3+ consecutive fully-absent days
        Map<Long, TreeMap<LocalDate, boolean[]>> perStudent = new HashMap<>();
        for (Object[] row : attendanceRepository.studentDailyAbsence(schoolId, today.minusDays(10), today)) {
            Long studentId = ((Number) row[0]).longValue();
            LocalDate date = (LocalDate) row[1];
            long absentCount = ((Number) row[2]).longValue();
            long total = ((Number) row[3]).longValue();
            perStudent.computeIfAbsent(studentId, k -> new TreeMap<>())
                    .put(date, new boolean[]{total > 0 && absentCount == total});
        }
        for (Map.Entry<Long, TreeMap<LocalDate, boolean[]>> entry : perStudent.entrySet()) {
            int streak = 0;
            for (Map.Entry<LocalDate, boolean[]> dayEntry : entry.getValue().descendingMap().entrySet()) {
                if (dayEntry.getValue()[0]) {
                    streak++;
                } else {
                    break;
                }
            }
            if (streak >= 3) {
                Student s = studentsById.get(entry.getKey());
                if (s != null) {
                    items.add(new AttentionItemDto("CONSECUTIVE_ABSENCE",
                            s.getFirstName() + " " + s.getLastName() + " " + streak + " kun ketma-ket kelmadi",
                            "students", s.getId()));
                }
            }
        }

        // 2) average grade below 3
        List<Object[]> avgRows = gradeRepository.averageScoreByStudent(schoolId);
        avgRows.sort(Comparator.comparingDouble(r -> (Double) r[1]));
        int lowGradeCount = 0;
        for (Object[] row : avgRows) {
            Double avg = (Double) row[1];
            if (avg != null && avg < 3.0) {
                Student s = studentsById.get(((Number) row[0]).longValue());
                if (s != null) {
                    items.add(new AttentionItemDto("LOW_GRADE",
                            s.getFirstName() + " " + s.getLastName() + " o'rtacha bahosi " + Math.round(avg * 100) / 100.0,
                            "students", s.getId()));
                    lowGradeCount++;
                }
            }
            if (lowGradeCount >= 30) break;
        }

        // 3) today's first lesson per class (the one attendance is normally taken for)
        // whose scheduled time has already passed but has no attendance recorded yet
        String weekday = weekdayName(today);
        if (weekday != null) {
            LocalTime now = LocalTime.now(ZONE);
            int count = 0;
            for (SchoolClass cls : schoolClassRepository.findByAcademicYearSchoolId(schoolId)) {
                if (count >= 30) break;
                LessonSlot firstLesson = lessonSlotRepository.findBySchoolClassId(cls.getId()).stream()
                        .filter(l -> l.getWeekday().equals(weekday))
                        .min(Comparator.comparing(LessonSlot::getStartTime))
                        .orElse(null);
                if (firstLesson == null || !firstLesson.getStartTime().isBefore(now)) continue;
                if (!attendanceRepository.existsByLessonSlotIdAndRecordDate(firstLesson.getId(), today)) {
                    items.add(new AttentionItemDto("MISSING_ATTENDANCE",
                            cls.getGradeNumber() + "-" + cls.getSectionLetter()
                                    + " sinfida bugungi davomat hali olinmagan",
                            "attendance-take", firstLesson.getId()));
                    count++;
                }
            }
        }

        return items;
    }

    public List<AbsenteeDto> getAbsenteesToday(Long schoolId) {
        LocalDate today = LocalDate.now(ZONE);
        return attendanceRepository.findAbsentBySchoolIdAndRecordDate(schoolId, today).stream()
                .map(a -> new AbsenteeDto(
                        a.getStudent().getId(),
                        a.getStudent().getFirstName() + " " + a.getStudent().getLastName(),
                        a.getLessonSlot().getSchoolClass().getGradeNumber() + "-" + a.getLessonSlot().getSchoolClass().getSectionLetter(),
                        a.getLessonSlot().getSubject().getName()))
                .collect(Collectors.toList());
    }

    public List<ActivityItemDto> getRecentActivity(Long schoolId, int limit) {
        return activityLogRepository.findBySchoolIdOrderByOccurredAtDesc(schoolId, PageRequest.of(0, limit)).stream()
                .map(log -> new ActivityItemDto(
                        log.getDescription(),
                        log.getIcon(),
                        log.getOccurredAt(),
                        log.getActorUsername()))
                .collect(Collectors.toList());
    }

    /**
     * Bundles every widget the dashboard needs into one payload so the page
     * can load with a single request instead of six parallel round trips.
     */
    public DashboardOverviewDto getOverview(Long schoolId, int trendDays, int activityLimit) {
        DashboardOverviewDto dto = new DashboardOverviewDto();
        dto.setSummary(getSummary(schoolId));
        dto.setLiveLessons(getLiveLessons(schoolId));
        dto.setLiveLessonsSummary(getLiveLessonsSummary(schoolId));
        dto.setAttendanceTrend(getAttendanceTrend(schoolId, trendDays));
        dto.setClassRankings(getClassRankings(schoolId));
        dto.setSubjectAverages(getSubjectAverages(schoolId));
        dto.setAttention(getAttentionItems(schoolId));
        dto.setActivity(getRecentActivity(schoolId, activityLimit));
        return dto;
    }
}
