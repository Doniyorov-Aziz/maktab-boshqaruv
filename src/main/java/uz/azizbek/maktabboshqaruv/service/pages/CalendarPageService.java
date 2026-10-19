package uz.azizbek.maktabboshqaruv.service.pages;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.dto.CalendarEventResponseDto;
import uz.azizbek.maktabboshqaruv.entity.CalendarEvent;
import uz.azizbek.maktabboshqaruv.entity.CalendarEventType;
import uz.azizbek.maktabboshqaruv.entity.CalendarFeedToken;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.repository.CalendarEventRepository;
import uz.azizbek.maktabboshqaruv.repository.CalendarFeedTokenRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import uz.azizbek.maktabboshqaruv.service.CalendarEventService;
import uz.azizbek.maktabboshqaruv.service.attendance.SchoolDays;
import uz.azizbek.maktabboshqaruv.service.dashboard.HomeDashboardService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentStats;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Predicate;

import static uz.azizbek.maktabboshqaruv.service.pages.PageSql.*;

/**
 * "Taqvim" page: the school year as one line (quarters, vacations, "you are here"), the hero
 * numbers, attendance per past school day for the month grid, one day's summary, and the calendar
 * as .ics (a file, or a secret subscription link that can be revoked).
 */
@Service
public class CalendarPageService {

    public record Segment(String kind, String label, LocalDate from, LocalDate to) {
    }

    public record EventRef(long id, String title, String type, LocalDate date, long daysLeft) {
    }

    public record Overview(LocalDate today, LocalDate yearStart, LocalDate yearEnd, List<Segment> segments,
                           Integer quarter, Double quarterDone, Integer quarterDaysLeft, EventRef nextEvent,
                           int monthEvents, List<Integer> eventsByMonth, EventRef nextExam, EventRef nextVacation,
                           boolean onVacation) {
    }

    public record DayAttendance(LocalDate date, Double rate, int absent) {
    }

    public record Birthday(long studentId, String name, String className, int age) {
    }

    public record DaySummary(LocalDate date, boolean schoolDay, String weekday, List<CalendarEventResponseDto> events,
                             int lessons, Double attendance, Integer present, Integer absent, int grades,
                             List<Birthday> birthdays) {
    }

    public record Feed(long id, String token, LocalDateTime createdAt, String createdBy) {
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    private final NamedParameterJdbcTemplate jdbc;
    private final CalendarEventRepository events;
    private final CalendarEventService eventService;
    private final CalendarFeedTokenRepository feeds;
    private final SchoolRepository schools;
    private final HomeDashboardService home;
    private final SchoolDays schoolDays;
    private final Clock clock;

    public CalendarPageService(NamedParameterJdbcTemplate jdbc, CalendarEventRepository events,
                               CalendarEventService eventService, CalendarFeedTokenRepository feeds,
                               SchoolRepository schools, HomeDashboardService home, SchoolDays schoolDays, Clock clock) {
        this.jdbc = jdbc;
        this.events = events;
        this.eventService = eventService;
        this.feeds = feeds;
        this.schools = schools;
        this.home = home;
        this.schoolDays = schoolDays;
        this.clock = clock;
    }

    private static MapSqlParameterSource p(long schoolId) {
        return new MapSqlParameterSource("s", schoolId);
    }

    // ------------------------------------------------------------------ the year line and the numbers

    @Transactional(readOnly = true)
    public Overview overview(long schoolId) {
        LocalDate today = LocalDate.now(clock);
        LocalDate[] year = schoolYear(schoolId, today);
        List<CalendarEvent> inYear = events.findInRange(schoolId, year[0], year[1]);

        List<Segment> segments = new ArrayList<>();
        for (int q = 1; q <= 4; q++) {
            LocalDate[] r = ParentStats.quarterRange(year[0], q);
            LocalDate to = q == 4 && r[1].isAfter(year[1]) ? year[1] : r[1];
            segments.add(new Segment("QUARTER", q + "-chorak", r[0], to));
        }
        for (CalendarEvent e : inYear) {
            if (e.getType() == CalendarEventType.VACATION) segments.add(new Segment("VACATION", e.getTitle(), e.getStartDate(), e.getEndDate()));
        }

        // the quarter we are in: share of its school days gone, school days left
        Integer quarter = null;
        Double done = null;
        Integer left = null;
        for (Segment s : segments) {
            if (!s.kind().equals("QUARTER") || today.isBefore(s.from()) || today.isAfter(s.to())) continue;
            quarter = Integer.valueOf(s.label().substring(0, 1));
            Predicate<LocalDate> isSchool = schoolDays.predicate(schoolId, s.from(), s.to());
            int total = 0, gone = 0;
            for (LocalDate d = s.from(); !d.isAfter(s.to()); d = d.plusDays(1)) {
                if (!isSchool.test(d)) continue;
                total++;
                if (d.isBefore(today)) gone++;
            }
            done = total == 0 ? null : round1(gone * 100.0 / total);
            left = total - gone;
        }

        EventRef next = null, exam = null, vacation = null;
        boolean onVacation = false;
        int[] byMonth = new int[12];
        int monthEvents = 0;
        for (CalendarEvent e : inYear) {
            int m = (e.getStartDate().getMonthValue() + 3) % 12; // September first
            byMonth[m]++;
            if (!e.getStartDate().isAfter(today.withDayOfMonth(today.lengthOfMonth())) && !e.getEndDate().isBefore(today.withDayOfMonth(1))) {
                monthEvents++;
            }
            if (e.getType() == CalendarEventType.VACATION && !today.isBefore(e.getStartDate()) && !today.isAfter(e.getEndDate())) {
                onVacation = true;
            }
            if (e.getStartDate().isBefore(today)) continue;
            EventRef ref = new EventRef(e.getId(), e.getTitle(), e.getType().name(), e.getStartDate(),
                    ChronoUnit.DAYS.between(today, e.getStartDate()));
            if (e.getType() != CalendarEventType.VACATION && (next == null || ref.date().isBefore(next.date()))) next = ref;
            if (e.getType() == CalendarEventType.EXAM && (exam == null || ref.date().isBefore(exam.date()))) exam = ref;
            if (e.getType() == CalendarEventType.VACATION && (vacation == null || ref.date().isBefore(vacation.date()))) vacation = ref;
        }
        List<Integer> months = new ArrayList<>();
        for (int v : byMonth) months.add(v);
        segments.sort(Comparator.comparing(Segment::from));
        return new Overview(today, year[0], year[1], segments, quarter, done, left, next, monthEvents, months, exam, vacation,
                onVacation);
    }

    /**
     * The academic year that holds today; when none does (the new year is not entered yet, or it is
     * summer), the school year around today: September 1 – May 31.
     */
    LocalDate[] schoolYear(long schoolId, LocalDate today) {
        List<Map<String, Object>> years = jdbc.queryForList("""
                select start_date, end_date from academic_year
                 where school_id = :s and :t between start_date and end_date
                 order by start_date desc limit 1""", p(schoolId).addValue("t", today));
        if (!years.isEmpty()) {
            return new LocalDate[]{date(years.get(0).get("start_date")), date(years.get(0).get("end_date"))};
        }
        int start = today.getMonthValue() >= 8 ? today.getYear() : today.getYear() - 1;
        return new LocalDate[]{LocalDate.of(start, 9, 1), LocalDate.of(start + 1, 5, 31)};
    }

    // ------------------------------------------------------------------ the month grid

    /** Attendance of each past school day in the range (today only once marks exist). */
    @Transactional(readOnly = true)
    public List<DayAttendance> attendance(long schoolId, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(clock);
        LocalDate end = to.isAfter(today) ? today : to;
        if (end.isBefore(from)) return List.of();
        List<DayAttendance> out = new ArrayList<>();
        home.dailyRates(schoolId, from, end).values().stream().sorted(Comparator.comparing(HomeDashboardService.DayRate::date))
                .forEach(r -> out.add(new DayAttendance(r.date(), r.rate(), r.absent())));
        return out;
    }

    // ------------------------------------------------------------------ one day

    @Transactional(readOnly = true)
    public DaySummary day(long schoolId, LocalDate date) {
        LocalDate today = LocalDate.now(clock);
        String weekday = SchoolDays.WEEKDAY_UZ.get(date.getDayOfWeek());
        boolean schoolDay = weekday != null && schoolDays.isSchoolDay(schoolId, date);
        int lessons = !schoolDay ? 0 : Optional.ofNullable(jdbc.queryForObject("""
                select count(*) from lesson_slot ls join school_class c on c.id = ls.class_id
                  join academic_year y on y.id = c.academic_year_id where y.school_id = :s and ls.weekday = :w""",
                p(schoolId).addValue("w", weekday), Integer.class)).orElse(0);
        Double rate = null;
        Integer present = null, absent = null;
        if (!date.isAfter(today)) {
            HomeDashboardService.DayRate r = home.dailyRates(schoolId, date, date).get(date);
            if (r != null) {
                rate = r.rate();
                present = r.present();
                absent = r.absent();
            }
        }
        Integer grades = jdbc.queryForObject("""
                select count(*) from grade g join students st on st.id = g.student_id
                  join school_class c on c.id = st.class_id join academic_year y on y.id = c.academic_year_id
                 where y.school_id = :s and g.grade_date = :d""", p(schoolId).addValue("d", date), Integer.class);
        List<Birthday> birthdays = jdbc.query("""
                select st.id, st.first_name, st.last_name, c.grade_number || '-' || c.section_letter as class_name, st.birth_date
                  from students st join school_class c on c.id = st.class_id join academic_year y on y.id = c.academic_year_id
                 where y.school_id = :s and extract(month from st.birth_date) = :m and extract(day from st.birth_date) = :dd
                 order by c.grade_number, c.section_letter, st.last_name""",
                p(schoolId).addValue("m", date.getMonthValue()).addValue("dd", date.getDayOfMonth()),
                (rs, n) -> new Birthday(rs.getLong("id"), (rs.getString("last_name") + " " + rs.getString("first_name")).trim(),
                        rs.getString("class_name"),
                        (int) ChronoUnit.YEARS.between(rs.getDate("birth_date").toLocalDate(), date)));
        return new DaySummary(date, schoolDay, weekday, eventService.getInRange(schoolId, date, date), lessons, rate, present,
                absent, grades == null ? 0 : grades, birthdays);
    }

    // ------------------------------------------------------------------ .ics

    @Transactional(readOnly = true)
    public byte[] ics(long schoolId, String host) {
        School school = schools.findById(schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday maktab topilmadi"));
        LocalDate[] range = IcsWriter.range(LocalDate.now(clock));
        return IcsWriter.write(school.getName() + " — taqvim", events.findInRange(schoolId, range[0], range[1]),
                IcsWriter.utc(clock), host);
    }

    /** The file behind a subscription link; a revoked or unknown token is a 404. */
    @Transactional(readOnly = true)
    public byte[] feed(String token, String host) {
        CalendarFeedToken t = feeds.findByTokenAndActiveTrue(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Havola topilmadi yoki bekor qilingan"));
        return ics(t.getSchool().getId(), host);
    }

    @Transactional(readOnly = true)
    public List<Feed> feeds(long schoolId) {
        return feeds.findBySchoolIdAndActiveTrueOrderByCreatedAtDesc(schoolId).stream()
                .map(f -> new Feed(f.getId(), f.getToken(), f.getCreatedAt(), f.getCreatedBy())).toList();
    }

    @Transactional
    public Feed createFeed(long schoolId, String by) {
        School school = schools.findById(schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday maktab topilmadi"));
        byte[] raw = new byte[24];
        RANDOM.nextBytes(raw);
        CalendarFeedToken f = new CalendarFeedToken();
        f.setSchool(school);
        f.setToken(Base64.getUrlEncoder().withoutPadding().encodeToString(raw));
        f.setCreatedBy(by);
        f.setCreatedAt(LocalDateTime.now(clock));
        f = feeds.save(f);
        return new Feed(f.getId(), f.getToken(), f.getCreatedAt(), f.getCreatedBy());
    }

    @Transactional
    public void revokeFeed(long schoolId, long id) {
        CalendarFeedToken f = feeds.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Havola topilmadi"));
        if (!f.getSchool().getId().equals(schoolId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Havola topilmadi");
        f.setActive(false);
        feeds.save(f);
    }
}
