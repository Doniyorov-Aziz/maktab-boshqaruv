package uz.azizbek.maktabboshqaruv.service.parent;

import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.*;
import uz.azizbek.maktabboshqaruv.service.BotSettingService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentViews.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Everything a parent can see about one child, computed from the school data.
 * Callers must pass a student they have already authorized for the chat
 * (see {@link ParentAccessService}) — this class does no access control itself.
 */
@Service
@Transactional(readOnly = true)
public class ParentDataService {

    @Autowired
    private LessonSlotRepository lessonSlotRepository;
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private GradeRepository gradeRepository;
    @Autowired
    private BehaviorRecordRepository behaviorRecordRepository;
    @Autowired
    private AnnouncementRepository announcementRepository;
    @Autowired
    private CalendarEventRepository calendarEventRepository;
    @Autowired
    private ParentMessageRepository parentMessageRepository;
    @Autowired
    private AbsenceRequestRepository absenceRequestRepository;
    @Autowired
    private BotSettingService botSettingService;
    @Autowired
    private Clock clock;

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    // ------------------------------------------------------------------ child

    public ChildInfo child(Student s) {
        SchoolClass c = s.getSchoolClass();
        School school = c.getAcademicYear().getSchool();
        Employee teacher = c.getClassTeacher();
        return new ChildInfo(s.getId(), fullName(s), s.getFirstName(), className(c), c.getId(),
                school.getId(), school.getName(), teacher == null ? null : teacher.getFirstName() + " " + teacher.getLastName());
    }

    // --------------------------------------------------------------- schedule

    public DaySchedule day(Student s, LocalDate date) {
        SchoolClass c = s.getSchoolClass();
        Long schoolId = c.getAcademicYear().getSchool().getId();
        List<EventView> events = calendarEventRepository.findInRange(schoolId, date, date).stream()
                .map(this::toEventView).toList();
        String holiday = events.stream()
                .filter(e -> "HOLIDAY".equals(e.type()) || "VACATION".equals(e.type()))
                .map(EventView::title).findFirst().orElse(null);
        List<LessonView> lessons = lessonsOn(c.getId(), date);
        boolean weekend = date.getDayOfWeek() == DayOfWeek.SUNDAY || (lessons.isEmpty() && date.getDayOfWeek() == DayOfWeek.SATURDAY);
        return new DaySchedule(date, date.getDayOfWeek(), holiday != null ? List.of() : lessons, holiday, weekend, events);
    }

    public List<DaySchedule> week(Student s, LocalDate anyDay) {
        LocalDate monday = anyDay.with(DayOfWeek.MONDAY);
        List<DaySchedule> days = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            days.add(day(s, monday.plusDays(i)));
        }
        return days;
    }

    /** Lessons of a class on a date, numbered by start time, with breaks and the "now" marker. */
    List<LessonView> lessonsOn(Long classId, LocalDate date) {
        String weekday = ParentStats.WEEKDAY_UZ.get(date.getDayOfWeek());
        List<LessonSlot> slots = slotsOf(classId, weekday);
        LocalDateTime now = now();
        boolean isToday = date.equals(now.toLocalDate());
        List<LessonView> result = new ArrayList<>();
        LocalTime previousEnd = null;
        int n = 1;
        for (LessonSlot slot : slots) {
            Integer breakMinutes = previousEnd == null ? null
                    : (int) Duration.between(previousEnd, slot.getStartTime()).toMinutes();
            boolean current = isToday && !now.toLocalTime().isBefore(slot.getStartTime()) && now.toLocalTime().isBefore(slot.getEndTime());
            result.add(new LessonView(n++, slot.getStartTime(), slot.getEndTime(), slot.getSubject().getId(),
                    slot.getSubject().getName(), personName(slot.getEmployee()),
                    slot.getRoom() != null ? slot.getRoom().getRoomNumber() : null, current,
                    breakMinutes != null && breakMinutes > 0 ? breakMinutes : null));
            previousEnd = slot.getEndTime();
        }
        return result;
    }

    private List<LessonSlot> slotsOf(Long classId, String weekday) {
        return lessonSlotRepository.findBySchoolClassId(classId).stream()
                .filter(l -> l.getWeekday().equals(weekday))
                .sorted(Comparator.comparing(LessonSlot::getStartTime))
                .toList();
    }

    /** 1-based lesson number of a slot within its class's day. */
    public int lessonNumber(LessonSlot slot) {
        List<LessonSlot> sameDay = slotsOf(slot.getSchoolClass().getId(), slot.getWeekday());
        for (int i = 0; i < sameDay.size(); i++) {
            if (sameDay.get(i).getId().equals(slot.getId())) return i + 1;
        }
        return 0;
    }

    // ------------------------------------------------------------- attendance

    /** kind: "month" (value "YYYY-MM"), "quarter" (value "1".."4") or "year". */
    public LocalDate[] periodRange(String kind, String value) {
        LocalDate today = today();
        return switch (kind) {
            case "quarter" -> ParentStats.quarterRange(today, Integer.parseInt(value));
            case "year" -> ParentStats.schoolYearRange(today);
            default -> {
                YearMonth ym = value == null ? YearMonth.from(today) : YearMonth.parse(value);
                yield new LocalDate[]{ym.atDay(1), ym.atEndOfMonth()};
            }
        };
    }

    public AttendanceSummary attendance(Student s, String kind, String value, String periodLabel) {
        LocalDate[] range = periodRange(kind, value);
        List<Attendance> records = attendanceRepository
                .findByStudentIdAndRecordDateBetweenOrderByRecordDate(s.getId(), range[0], range[1]);
        ParentStats.Counts counts = ParentStats.count(records.stream().map(Attendance::getStatus).toList());

        Map<LocalDate, List<AttendanceStatus>> byDay = new TreeMap<>();
        for (Attendance a : records) {
            byDay.computeIfAbsent(a.getRecordDate(), d -> new ArrayList<>()).add(a.getStatus());
        }
        List<AttendanceDay> days = new ArrayList<>();
        for (LocalDate d = range[0]; !d.isAfter(range[1]); d = d.plusDays(1)) {
            days.add(new AttendanceDay(d, ParentStats.dayStatus(byDay.get(d))));
        }

        Double classRate = null;
        List<Object[]> totals = attendanceRepository.classAttendanceTotals(s.getSchoolClass().getId(), range[0], range[1]);
        if (!totals.isEmpty() && totals.get(0)[1] != null) {
            long attended = totals.get(0)[0] == null ? 0 : ((Number) totals.get(0)[0]).longValue();
            classRate = ParentStats.rate(attended, ((Number) totals.get(0)[1]).longValue());
        }
        return new AttendanceSummary(periodLabel, range[0], range[1], counts.total(), counts.present(), counts.late(),
                counts.absent(), counts.excused(), counts.rate(), classRate, days);
    }

    public List<AttendanceIncident> incidents(Student s, LocalDate from, LocalDate to) {
        return attendanceRepository.findByStudentIdAndRecordDateBetweenOrderByRecordDate(s.getId(), from, to).stream()
                .filter(a -> a.getStatus() == AttendanceStatus.ABSENT || a.getStatus() == AttendanceStatus.LATE)
                .sorted(Comparator.comparing(Attendance::getRecordDate).reversed()
                        .thenComparing(a -> a.getLessonSlot().getStartTime()))
                .map(a -> new AttendanceIncident(a.getRecordDate(), lessonNumber(a.getLessonSlot()),
                        a.getLessonSlot().getSubject().getName(), a.getStatus().name()))
                .toList();
    }

    public List<SubjectAbsence> absencesBySubject(Student s, LocalDate from, LocalDate to) {
        Map<String, int[]> bySubject = new TreeMap<>();
        for (Attendance a : attendanceRepository.findByStudentIdAndRecordDateBetweenOrderByRecordDate(s.getId(), from, to)) {
            if (a.getStatus() != AttendanceStatus.ABSENT && a.getStatus() != AttendanceStatus.LATE) continue;
            int[] c = bySubject.computeIfAbsent(a.getLessonSlot().getSubject().getName(), k -> new int[2]);
            if (a.getStatus() == AttendanceStatus.ABSENT) c[0]++;
            else c[1]++;
        }
        return bySubject.entrySet().stream()
                .map(e -> new SubjectAbsence(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .sorted(Comparator.comparingInt((SubjectAbsence x) -> -(x.absent() * 10 + x.late())))
                .toList();
    }

    // ----------------------------------------------------------------- grades

    public List<GradeView> recentGrades(Student s, int limit) {
        return gradeRepository.findByStudentIdOrderByGradeDateDesc(s.getId()).stream()
                .limit(limit).map(this::toGradeView).toList();
    }

    public List<GradeView> gradesBetween(Student s, LocalDate from, LocalDate to) {
        return gradeRepository.findByStudentIdAndGradeDateBetweenOrderByGradeDateDescIdDesc(s.getId(), from, to)
                .stream().map(this::toGradeView).toList();
    }

    /**
     * Averages per subject over the current school year (the same set the
     * subject detail page lists), with the trend of this month vs the previous
     * one — taken from all grades, so September still compares with August.
     */
    public List<SubjectAverage> subjectAverages(Student s) {
        LocalDate today = today();
        LocalDate[] year = ParentStats.schoolYearRange(today);
        List<Grade> all = gradeRepository.findByStudentIdOrderByGradeDateDesc(s.getId());
        YearMonth thisMonth = YearMonth.from(today);
        YearMonth previous = thisMonth.minusMonths(1);
        Map<Long, List<Grade>> bySubject = all.stream().collect(Collectors.groupingBy(g -> g.getSubject().getId(),
                LinkedHashMap::new, Collectors.toList()));
        List<SubjectAverage> result = new ArrayList<>();
        for (List<Grade> list : bySubject.values()) {
            List<Grade> thisYear = list.stream().filter(g -> inRange(g.getGradeDate(), year)).toList();
            if (thisYear.isEmpty()) continue;
            Subject subject = list.get(0).getSubject();
            Double avg = ParentStats.average(thisYear.stream().map(Grade::getScore).toList());
            Double now = ParentStats.average(list.stream().filter(g -> YearMonth.from(g.getGradeDate()).equals(thisMonth)).map(Grade::getScore).toList());
            Double before = ParentStats.average(list.stream().filter(g -> YearMonth.from(g.getGradeDate()).equals(previous)).map(Grade::getScore).toList());
            result.add(new SubjectAverage(subject.getId(), subject.getName(), avg, thisYear.size(), ParentStats.trend(now, before)));
        }
        result.sort(Comparator.comparingDouble(SubjectAverage::average).reversed().thenComparing(SubjectAverage::subject));
        return result;
    }

    /** All of the current school year's grades in one subject, with its teacher. */
    public SubjectDetail subjectDetail(Student s, Long subjectId) {
        LocalDate[] year = ParentStats.schoolYearRange(today());
        List<Grade> grades = gradeRepository.findByStudentIdOrderByGradeDateDesc(s.getId()).stream()
                .filter(g -> g.getSubject().getId().equals(subjectId) && inRange(g.getGradeDate(), year)).toList();
        String subjectName = grades.isEmpty() ? null : grades.get(0).getSubject().getName();
        String teacher = null;
        for (LessonSlot slot : lessonSlotRepository.findBySchoolClassId(s.getSchoolClass().getId())) {
            if (slot.getSubject().getId().equals(subjectId)) {
                teacher = personName(slot.getEmployee());
                if (subjectName == null) subjectName = slot.getSubject().getName();
                break;
            }
        }
        Double avg = ParentStats.average(grades.stream().map(Grade::getScore).toList());
        return new SubjectDetail(subjectId, subjectName, teacher, avg == null ? 0 : avg,
                grades.stream().map(this::toGradeView).toList());
    }

    public List<GradeView> quarterGrades(Student s) {
        return gradeRepository.findByStudentIdOrderByGradeDateDesc(s.getId()).stream()
                .filter(g -> g.getType() == GradeType.QUARTERLY).map(this::toGradeView).toList();
    }

    // ----------------------------------------------------------------- report

    /** kind "week" = this Monday..today, "month" = 1st of month..today. */
    public ReportView report(Student s, String kind, String periodLabel) {
        LocalDate today = today();
        LocalDate from = "month".equals(kind) ? today.withDayOfMonth(1) : today.with(DayOfWeek.MONDAY);
        return report(s, from, today, periodLabel);
    }

    public ReportView report(Student s, LocalDate from, LocalDate to, String periodLabel) {
        List<Attendance> records = attendanceRepository.findByStudentIdAndRecordDateBetweenOrderByRecordDate(s.getId(), from, to);
        ParentStats.Counts counts = ParentStats.count(records.stream().map(Attendance::getStatus).toList());
        AttendanceSummary att = new AttendanceSummary(periodLabel, from, to, counts.total(), counts.present(),
                counts.late(), counts.absent(), counts.excused(), counts.rate(), null, List.of());

        List<Grade> grades = gradeRepository.findByStudentIdAndGradeDateBetweenOrderByGradeDateDescIdDesc(s.getId(), from, to);
        Double avg = ParentStats.average(grades.stream().map(Grade::getScore).toList());

        List<BehaviorRecord> behavior = behaviorRecordRepository.findByStudentIdAndRecordDateBetweenOrderByRecordDateDesc(s.getId(), from, to);
        int rewards = (int) behavior.stream().filter(b -> b.getType() == BehaviorType.REWARD).count();

        Map<String, List<Integer>> bySubject = new TreeMap<>();
        for (Grade g : grades) {
            bySubject.computeIfAbsent(g.getSubject().getName(), k -> new ArrayList<>()).add(g.getScore());
        }
        Map<String, Integer> absences = new HashMap<>();
        for (Attendance a : records) {
            if (a.getStatus() == AttendanceStatus.ABSENT) absences.merge(a.getLessonSlot().getSubject().getName(), 1, Integer::sum);
        }
        List<String> strong = new ArrayList<>();
        Set<String> attention = new LinkedHashSet<>();
        bySubject.forEach((subject, scores) -> {
            double a = ParentStats.average(scores);
            if (a >= 4.5) strong.add(subject);
            if (a < 3.5) attention.add(subject);
        });
        absences.forEach((subject, n) -> {
            if (n >= 2) attention.add(subject);
        });
        return new ReportView(periodLabel, from, to, att, grades.size(), avg, rewards, behavior.size() - rewards,
                strong, new ArrayList<>(attention));
    }

    // --------------------------------------------------------------- behavior

    public List<BehaviorView> behavior(Student s, LocalDate from, LocalDate to) {
        return behaviorRecordRepository.findByStudentIdAndRecordDateBetweenOrderByRecordDateDesc(s.getId(), from, to).stream()
                // Positive records first — the section should open on good news.
                .sorted(Comparator.comparing((BehaviorRecord b) -> b.getType() == BehaviorType.REWARD ? 0 : 1)
                        .thenComparing(BehaviorRecord::getRecordDate, Comparator.reverseOrder()))
                .map(b -> new BehaviorView(b.getRecordDate(), b.getType().name(), b.getDescription()))
                .toList();
    }

    // ---------------------------------------------------------- announcements

    public List<AnnouncementView> announcements(Student s, ParentSession session) {
        SchoolClass c = s.getSchoolClass();
        return announcementRepository.findForParents(c.getAcademicYear().getSchool().getId(), c.getId()).stream()
                .map(a -> toAnnouncementView(a, session))
                .toList();
    }

    public Optional<AnnouncementView> announcement(Student s, ParentSession session, Long id) {
        return announcements(s, session).stream().filter(a -> a.id().equals(id)).findFirst();
    }

    private AnnouncementView toAnnouncementView(Announcement a, ParentSession session) {
        return new AnnouncementView(a.getId(), a.getTitle(), a.getContent(),
                a.getCreatedDate() != null ? a.getCreatedDate().toLocalDate() : null,
                a.getPriority() == AnnouncementPriority.HIGH,
                session != null && !session.hasRead(a.getId()), a.getDeadline(),
                a.getSchoolClass() != null ? className(a.getSchoolClass()) : null);
    }

    // ------------------------------------------------------------------ events

    public List<EventView> upcomingEvents(Student s, int days) {
        LocalDate today = today();
        return calendarEventRepository.findInRange(s.getSchoolClass().getAcademicYear().getSchool().getId(), today, today.plusDays(days))
                .stream().map(this::toEventView).toList();
    }

    private EventView toEventView(CalendarEvent e) {
        return new EventView(e.getId(), e.getTitle(), e.getType().name(), e.getStartDate(), e.getEndDate(), e.getDescription());
    }

    // ---------------------------------------------------------------- teachers

    public List<TeacherView> teachers(Student s) {
        SchoolClass c = s.getSchoolClass();
        boolean showPhones = Boolean.TRUE.equals(botSettingService.getOrDefault(c.getAcademicYear().getSchool()).getShowTeacherPhones());
        List<TeacherView> result = new ArrayList<>();
        if (c.getClassTeacher() != null) {
            result.add(new TeacherView(null, personName(c.getClassTeacher()),
                    showPhones ? c.getClassTeacher().getPhone() : null, true));
        }
        Map<String, Employee> bySubject = new TreeMap<>();
        for (LessonSlot slot : lessonSlotRepository.findBySchoolClassId(c.getId())) {
            bySubject.putIfAbsent(slot.getSubject().getName(), slot.getEmployee());
        }
        bySubject.forEach((subject, e) -> result.add(new TeacherView(subject, personName(e), showPhones ? e.getPhone() : null, false)));
        return result;
    }

    /** Subject teacher for the "✍️ O'qituvchiga yozish" button on a low-grade message. */
    public String teacherOf(Student s, Long subjectId) {
        for (LessonSlot slot : lessonSlotRepository.findBySchoolClassId(s.getSchoolClass().getId())) {
            if (slot.getSubject().getId().equals(subjectId)) return personName(slot.getEmployee());
        }
        return null;
    }

    // ------------------------------------------------------------------ school

    public SchoolInfo school(Student s) {
        School school = s.getSchoolClass().getAcademicYear().getSchool();
        BotSetting setting = botSettingService.getOrDefault(school);
        List<BellView> bells = new ArrayList<>();
        int n = 1;
        for (Object[] row : lessonSlotRepository.findDistinctPeriodsForWeekday(school.getId(), "Dushanba")) {
            bells.add(new BellView(n++, (LocalTime) row[0], (LocalTime) row[1]));
        }
        return new SchoolInfo(school.getName(), school.getAddress(), setting.getPhone(), setting.getDirectorName(),
                setting.getReceptionHours(), setting.getBellScheduleNote(), bells);
    }

    // ------------------------------------------------------------------- today

    public TodaySummary todaySummary(Student s, ParentSession session) {
        LocalDate today = today();
        DaySchedule schedule = day(s, today);
        List<Attendance> todayRecords = attendanceRepository.findByStudentIdAndRecordDateBetweenOrderByRecordDate(s.getId(), today, today);
        ParentStats.Counts counts = ParentStats.count(todayRecords.stream().map(Attendance::getStatus).toList());
        String state = todayRecords.isEmpty() ? "NONE" : ParentStats.dayStatus(todayRecords.stream().map(Attendance::getStatus).toList());
        List<GradeView> gradesToday = gradesBetween(s, today, today);
        int unread = (int) announcements(s, session).stream().filter(AnnouncementView::unread).count();
        EventView next = upcomingEvents(s, 30).stream().findFirst().orElse(null);
        return new TodaySummary(child(s), today, schedule, state, counts.late(), counts.absent(), gradesToday, unread, next);
    }

    // ------------------------------------------------------- messages, requests

    public List<MessageView> messages(Student s, Long chatId) {
        return parentMessageRepository.findTop5ByChatIdAndStudentIdOrderByCreatedAtDesc(chatId, s.getId()).stream()
                .map(m -> new MessageView(m.getId(), m.getRecipient().name(), m.getText(), m.getStatus().name(),
                        m.getCreatedAt(), m.getReplyText()))
                .toList();
    }

    public List<AbsenceView> absenceRequests(Student s, Long chatId) {
        return absenceRequestRepository.findTop5ByChatIdAndStudentIdOrderByCreatedAtDesc(chatId, s.getId()).stream()
                .map(r -> new AbsenceView(r.getId(), r.getDateFrom(), r.getDateTo(), r.getReason().name(),
                        r.getStatus().name(), r.getDecisionNote()))
                .toList();
    }

    // ----------------------------------------------------------------- helpers

    private static boolean inRange(LocalDate d, LocalDate[] range) {
        return !d.isBefore(range[0]) && !d.isAfter(range[1]);
    }

    private GradeView toGradeView(Grade g) {
        return new GradeView(g.getId(), g.getGradeDate(), g.getSubject().getId(), g.getSubject().getName(),
                g.getScore(), g.getType().name(), g.getComment());
    }

    public static String fullName(Student s) {
        return s.getFirstName() + " " + s.getLastName();
    }

    public static String className(SchoolClass c) {
        return c.getGradeNumber() + "-" + c.getSectionLetter();
    }

    private static String personName(Employee e) {
        return e == null ? null : e.getFirstName() + " " + e.getLastName();
    }
}
