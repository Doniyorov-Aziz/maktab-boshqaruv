package uz.azizbek.maktabboshqaruv.service.pages;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.service.RoomBookingService;
import uz.azizbek.maktabboshqaruv.service.attendance.SchoolDays;
import uz.azizbek.maktabboshqaruv.service.dashboard.DashboardCache;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static uz.azizbek.maktabboshqaruv.service.pages.PageSql.*;

/**
 * "Xonalar" page: the live map (every room by building and floor: busy with which lesson until
 * when, free until when, under repair) at now or at a chosen day and lesson, the free-room finder,
 * one room's week and bookings, and which rooms work hardest.
 */
@Service
public class RoomsPageService {

    public record RoomInfo(long id, String number, long buildingId, String building, Integer floor, String type,
                           int capacity, String status, List<String> equipment, Integer computers) {
    }

    public record Occupant(String kind, Long lessonId, Long classId, String className, Long subjectId, String subject,
                           String subjectColor, String teacher, String purpose, LocalTime start, LocalTime end) {
    }

    /** state: BUSY (a lesson), BOOKED (a booking), FREE, REPAIR. */
    public record LiveRoom(RoomInfo room, String state, Occupant now, Double progress, LocalTime until) {
    }

    public record Floor(Integer floor, List<LiveRoom> rooms) {
    }

    public record BuildingMap(long id, String name, List<Floor> floors) {
    }

    public record LiveKpis(int total, int active, int busy, Double busyPct, Double weekUse, int repair) {
    }

    public record Live(LocalDate date, LocalTime time, String weekday, boolean schoolDay,
                       List<TimetablePageService.Period> periods, List<BuildingMap> buildings, LiveKpis kpis) {
    }

    public record Match(RoomInfo room, int score, List<String> matched, List<String> missing, boolean typeMatch) {
    }

    public record Finder(LocalDate date, List<Integer> periods, LocalTime start, LocalTime end, List<Match> rooms) {
    }

    public record Cell(long lessonId, long classId, String className, long subjectId, String subject, String subjectColor,
                       String teacher) {
    }

    public record Count(long id, String name, int lessons) {
    }

    public record Detail(RoomInfo room, List<TimetablePageService.Period> periods, List<List<Cell>> week, Double use,
                         List<Count> classes, List<Count> teachers, List<RoomBookingService.BookingView> bookings) {
    }

    public record RoomUse(long id, String number, String building, String type, Double use, int lessons) {
    }

    public record TypeUse(String type, int rooms, Double use) {
    }

    public record Analytics(List<RoomUse> busiest, List<RoomUse> quietest, List<TypeUse> byType) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final TimetablePageService timetable;
    private final RoomBookingService bookings;
    private final SchoolDays schoolDays;
    private final DashboardCache cache;
    private final Clock clock;

    public RoomsPageService(NamedParameterJdbcTemplate jdbc, TimetablePageService timetable, RoomBookingService bookings,
                            SchoolDays schoolDays, DashboardCache cache, Clock clock) {
        this.jdbc = jdbc;
        this.timetable = timetable;
        this.bookings = bookings;
        this.schoolDays = schoolDays;
        this.cache = cache;
        this.clock = clock;
    }

    private static MapSqlParameterSource p(long schoolId) {
        return new MapSqlParameterSource("s", schoolId);
    }

    List<RoomInfo> rooms(long schoolId) {
        return jdbc.query("""
                select r.id, r.room_number, b.id as building_id, b.name as building, r.floor, r.type, r.capacity,
                       coalesce(r.status, 'ACTIVE') as status, r.equipment, r.computers
                  from room r join building b on b.id = r.building_id
                 where b.school_id = :s
                 order by b.name, r.floor nulls last, r.room_number""", p(schoolId), (rs, n) -> {
            String eq = rs.getString("equipment");
            List<String> tags = eq == null || eq.isBlank() ? List.of()
                    : Arrays.stream(eq.split(",")).map(String::trim).filter(t -> !t.isEmpty()).toList();
            return new RoomInfo(rs.getLong("id"), rs.getString("room_number"), rs.getLong("building_id"),
                    rs.getString("building"), (Integer) rs.getObject("floor"),
                    rs.getString("type") == null ? "CLASSROOM" : rs.getString("type"), rs.getInt("capacity"),
                    rs.getString("status"), tags, (Integer) rs.getObject("computers"));
        });
    }

    /** "301" → 301, "Kompyuter xonasi" → a big number, so numbers come first and in order. */
    static Comparator<RoomInfo> roomOrder() {
        return Comparator.comparing((RoomInfo r) -> {
            String digits = r.number().replaceAll("^(\\d+).*", "$1");
            return digits.matches("\\d+") ? Integer.parseInt(digits) : Integer.MAX_VALUE;
        }).thenComparing(RoomInfo::number);
    }

    /** A day's occupants per room: its lessons (on a school day) and its bookings. */
    private Map<Long, List<Occupant>> occupants(long schoolId, LocalDate date) {
        Map<Long, List<Occupant>> byRoom = new HashMap<>();
        String weekday = SchoolDays.WEEKDAY_UZ.get(date.getDayOfWeek());
        if (weekday != null && schoolDays.isSchoolDay(schoolId, date)) {
            for (TimetablePageService.Lesson l : timetable.lessonsOf(schoolId)) {
                if (!l.weekday().equals(weekday)) continue;
                byRoom.computeIfAbsent(l.roomId(), k -> new ArrayList<>()).add(new Occupant("LESSON", l.id(), l.classId(),
                        l.className(), l.subjectId(), l.subject(), l.subjectColor(), l.teacherShort(), null, l.start(), l.end()));
            }
        }
        for (RoomBookingService.BookingView b : bookings.inRange(schoolId, date, date)) {
            byRoom.computeIfAbsent(b.roomId(), k -> new ArrayList<>()).add(new Occupant("BOOKING", null, null, null, null,
                    null, null, b.responsible(), b.purpose(), b.startTime(), b.endTime()));
        }
        byRoom.values().forEach(list -> list.sort(Comparator.comparing(Occupant::start)));
        return byRoom;
    }

    /** The bell periods of that weekday (else of the whole week). */
    private List<TimetablePageService.Period> periodsOf(long schoolId, LocalDate date) {
        List<TimetablePageService.Lesson> all = timetable.lessonsOf(schoolId);
        String weekday = SchoolDays.WEEKDAY_UZ.get(date.getDayOfWeek());
        List<TimetablePageService.Lesson> ofDay = all.stream().filter(l -> l.weekday().equals(weekday)).toList();
        return TimetablePageService.periods(ofDay.isEmpty() ? all : ofDay);
    }

    // ------------------------------------------------------------------ live map

    @Transactional(readOnly = true)
    public Live live(long schoolId, LocalDate date, LocalTime time) {
        LocalDate d = date != null ? date : LocalDate.now(clock);
        LocalTime t = time != null ? time : LocalTime.now(clock).withSecond(0).withNano(0);
        String weekday = SchoolDays.WEEKDAY_UZ.get(d.getDayOfWeek());
        boolean schoolDay = weekday != null && schoolDays.isSchoolDay(schoolId, d);
        Map<Long, List<Occupant>> occ = occupants(schoolId, d);
        List<RoomInfo> all = rooms(schoolId);

        int busy = 0, active = 0, repair = 0;
        Map<Long, BuildingMap> buildings = new LinkedHashMap<>();
        Map<Long, TreeMap<Integer, List<LiveRoom>>> floors = new LinkedHashMap<>();
        Map<Long, String> names = new LinkedHashMap<>();
        for (RoomInfo r : all) {
            LiveRoom live = state(r, occ.getOrDefault(r.id(), List.of()), t);
            if (live.state().equals("REPAIR")) repair++;
            else {
                active++;
                if (!live.state().equals("FREE")) busy++;
            }
            names.putIfAbsent(r.buildingId(), r.building());
            floors.computeIfAbsent(r.buildingId(), k -> new TreeMap<>())
                    .computeIfAbsent(r.floor() == null ? 0 : r.floor(), k -> new ArrayList<>()).add(live);
        }
        for (Map.Entry<Long, TreeMap<Integer, List<LiveRoom>>> b : floors.entrySet()) {
            List<Floor> list = new ArrayList<>();
            // the top floor first, like a building seen from the front
            for (Map.Entry<Integer, List<LiveRoom>> f : b.getValue().descendingMap().entrySet()) {
                List<LiveRoom> rs = new ArrayList<>(f.getValue());
                rs.sort(Comparator.comparing(LiveRoom::room, roomOrder()));
                list.add(new Floor(f.getKey() == 0 && rs.stream().allMatch(x -> x.room().floor() == null) ? null : f.getKey(), rs));
            }
            buildings.put(b.getKey(), new BuildingMap(b.getKey(), names.get(b.getKey()), list));
        }
        TimetablePageService.Kpis k = cache.get(schoolId, "tt:kpis", () -> timetable.kpis(schoolId)).data();
        LiveKpis kpis = new LiveKpis(all.size(), active, busy, active == 0 ? null : round1(busy * 100.0 / active), k.roomUse(), repair);
        return new Live(d, t, weekday, schoolDay, periodsOf(schoolId, d), new ArrayList<>(buildings.values()), kpis);
    }

    /** A room at time t: under repair, in a lesson / booking (with how far it is), or free until the next one. */
    static LiveRoom state(RoomInfo r, List<Occupant> day, LocalTime t) {
        if ("REPAIR".equals(r.status())) return new LiveRoom(r, "REPAIR", null, null, null);
        for (Occupant o : day) {
            if (!t.isBefore(o.start()) && t.isBefore(o.end())) {
                double total = Duration.between(o.start(), o.end()).toMinutes();
                double gone = Duration.between(o.start(), t).toMinutes();
                return new LiveRoom(r, o.kind().equals("LESSON") ? "BUSY" : "BOOKED", o,
                        total <= 0 ? null : round1(gone * 100.0 / total), o.end());
            }
        }
        LocalTime next = day.stream().map(Occupant::start).filter(s -> s.isAfter(t)).min(Comparator.naturalOrder()).orElse(null);
        return new LiveRoom(r, "FREE", null, null, next);
    }

    // ------------------------------------------------------------------ free-room finder

    static final Map<String, String> EQUIPMENT = Map.of("PROJECTOR", "Proyektor", "SMARTBOARD", "Interaktiv doska",
            "AC", "Konditsioner", "COMPUTERS", "Kompyuterlar");

    /**
     * Rooms free in all the chosen lessons of the date, big enough, sorted by how well they fit:
     * the type asked for, the equipment asked for, and a size close to what is needed.
     */
    @Transactional(readOnly = true)
    public Finder find(long schoolId, LocalDate date, List<Integer> periodNumbers, Integer minCapacity, String type,
                       List<String> equipment) {
        List<TimetablePageService.Period> periods = periodsOf(schoolId, date);
        List<TimetablePageService.Period> chosen = periods.stream()
                .filter(p -> periodNumbers == null || periodNumbers.isEmpty() || periodNumbers.contains(p.n())).toList();
        if (chosen.isEmpty()) throw new IllegalStateException("Dars tanlang");
        LocalTime start = chosen.stream().map(TimetablePageService.Period::start).min(Comparator.naturalOrder()).orElseThrow();
        LocalTime end = chosen.stream().map(TimetablePageService.Period::end).max(Comparator.naturalOrder()).orElseThrow();
        Map<Long, List<Occupant>> occ = occupants(schoolId, date);
        int need = minCapacity == null ? 0 : minCapacity;
        List<String> wanted = equipment == null ? List.of() : equipment.stream().filter(e -> !e.isBlank()).toList();
        List<Match> out = new ArrayList<>();
        for (RoomInfo r : rooms(schoolId)) {
            if ("REPAIR".equals(r.status()) || r.capacity() < need) continue;
            boolean free = true;
            for (TimetablePageService.Period p : chosen) {
                for (Occupant o : occ.getOrDefault(r.id(), List.of())) {
                    if (o.start().isBefore(p.end()) && o.end().isAfter(p.start())) {
                        free = false;
                        break;
                    }
                }
                if (!free) break;
            }
            if (!free) continue;
            List<String> has = new ArrayList<>(r.equipment());
            if (r.computers() != null && r.computers() > 0) has.add("COMPUTERS");
            List<String> matched = wanted.stream().filter(has::contains).toList();
            List<String> missing = wanted.stream().filter(w -> !has.contains(w)).toList();
            boolean typeMatch = type == null || type.isBlank() || type.equals(r.type());
            int score = 50;
            if (type != null && !type.isBlank()) score += typeMatch ? 25 : -25;
            if (!wanted.isEmpty()) score += (int) Math.round(25.0 * (matched.size() - missing.size()) / wanted.size());
            if (need > 0) score -= (int) Math.min(20, Math.round((r.capacity() - need) * 20.0 / Math.max(need, 1)));
            out.add(new Match(r, Math.max(0, Math.min(100, score)), matched, missing, typeMatch));
        }
        out.sort(Comparator.comparingInt(Match::score).reversed().thenComparing(m -> m.room().capacity())
                .thenComparing(Match::room, roomOrder()));
        return new Finder(date, chosen.stream().map(TimetablePageService.Period::n).toList(), start, end, out);
    }

    // ------------------------------------------------------------------ one room

    @Transactional(readOnly = true)
    public Detail detail(long schoolId, long roomId) {
        RoomInfo room = rooms(schoolId).stream().filter(r -> r.id() == roomId).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday xona topilmadi"));
        List<TimetablePageService.Lesson> all = timetable.lessonsOf(schoolId);
        List<TimetablePageService.Period> periods = TimetablePageService.periods(all);
        Map<LocalTime, Integer> row = new HashMap<>();
        for (int k = 0; k < periods.size(); k++) row.put(periods.get(k).start(), k);
        List<List<Cell>> week = new ArrayList<>();
        for (int d = 0; d < 6; d++) week.add(new ArrayList<>(Collections.nCopies(periods.size(), (Cell) null)));
        Map<Long, int[]> classCount = new HashMap<>(), teacherCount = new HashMap<>();
        Map<Long, String> classNames = new HashMap<>(), teacherNames = new HashMap<>();
        int filled = 0;
        for (TimetablePageService.Lesson l : all) {
            if (l.roomId() != roomId) continue;
            int d = weekdayIndex(l.weekday());
            Integer k = row.get(l.start());
            if (d < 0 || k == null) continue;
            if (week.get(d).get(k) == null) filled++;
            week.get(d).set(k, new Cell(l.id(), l.classId(), l.className(), l.subjectId(), l.subject(), l.subjectColor(),
                    l.teacherShort()));
            classCount.computeIfAbsent(l.classId(), x -> new int[1])[0]++;
            teacherCount.computeIfAbsent(l.teacherId(), x -> new int[1])[0]++;
            classNames.put(l.classId(), l.className());
            teacherNames.put(l.teacherId(), l.teacher());
        }
        Double use = periods.isEmpty() ? null : round1(filled * 100.0 / (periods.size() * 6.0));
        LocalDate today = LocalDate.now(clock);
        List<RoomBookingService.BookingView> upcoming = bookings.inRange(schoolId, today, today.plusDays(30)).stream()
                .filter(b -> b.roomId() == roomId).toList();
        return new Detail(room, periods, week, use, counts(classCount, classNames), counts(teacherCount, teacherNames), upcoming);
    }

    private static List<Count> counts(Map<Long, int[]> counts, Map<Long, String> names) {
        return counts.entrySet().stream().map(e -> new Count(e.getKey(), names.get(e.getKey()), e.getValue()[0]))
                .sorted(Comparator.comparingInt(Count::lessons).reversed().thenComparing(Count::name)).toList();
    }

    // ------------------------------------------------------------------ analytics

    @Transactional(readOnly = true)
    public Analytics analytics(long schoolId) {
        List<TimetablePageService.Lesson> all = timetable.lessonsOf(schoolId);
        int slots = TimetablePageService.periods(all).size() * 6;
        Map<Long, Set<String>> taken = new HashMap<>();
        Map<Long, Integer> lessonCount = new HashMap<>();
        for (TimetablePageService.Lesson l : all) {
            taken.computeIfAbsent(l.roomId(), k -> new HashSet<>()).add(l.weekday() + "|" + l.start());
            lessonCount.merge(l.roomId(), 1, Integer::sum);
        }
        List<RoomUse> uses = new ArrayList<>();
        for (RoomInfo r : rooms(schoolId)) {
            if ("REPAIR".equals(r.status())) continue;
            int n = taken.getOrDefault(r.id(), Set.of()).size();
            uses.add(new RoomUse(r.id(), r.number(), r.building(), r.type(), slots == 0 ? null : round1(n * 100.0 / slots),
                    lessonCount.getOrDefault(r.id(), 0)));
        }
        Comparator<RoomUse> byUse = Comparator.comparing((RoomUse u) -> u.use() == null ? 0 : u.use());
        List<RoomUse> busiest = uses.stream().sorted(byUse.reversed()).limit(5).toList();
        List<RoomUse> quietest = uses.stream().sorted(byUse).limit(5).toList();
        Map<String, List<RoomUse>> byType = uses.stream().collect(Collectors.groupingBy(RoomUse::type, LinkedHashMap::new,
                Collectors.toList()));
        List<TypeUse> types = new ArrayList<>();
        byType.forEach((type, list) -> types.add(new TypeUse(type, list.size(),
                round1(list.stream().mapToDouble(u -> u.use() == null ? 0 : u.use()).average().orElse(0)))));
        types.sort(Comparator.comparing((TypeUse x) -> x.use() == null ? 0 : x.use()).reversed());
        return new Analytics(busiest, quietest, types);
    }
}
