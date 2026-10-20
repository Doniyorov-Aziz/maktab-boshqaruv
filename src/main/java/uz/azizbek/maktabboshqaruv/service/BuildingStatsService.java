package uz.azizbek.maktabboshqaruv.service;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Time;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.*;

/**
 * How busy a building is. "Haftalik bandlik" — of all lesson slots of the week in its working rooms
 * (rooms × the school's timetable periods: every weekday and start time the school uses), how many
 * have a lesson. "Hozir band" — the rooms with a lesson at this moment. Rooms under repair count
 * in neither. Also the floors with their rooms (busy now, free, under repair) for the drawing, the
 * room types and the seats. Three queries for any number of buildings.
 */
@Service
public class BuildingStatsService {

    private static final Map<DayOfWeek, String> WEEKDAYS = Map.of(
            DayOfWeek.MONDAY, "Dushanba", DayOfWeek.TUESDAY, "Seshanba", DayOfWeek.WEDNESDAY, "Chorshanba",
            DayOfWeek.THURSDAY, "Payshanba", DayOfWeek.FRIDAY, "Juma", DayOfWeek.SATURDAY, "Shanba");

    /** A room on the drawing: BUSY (a lesson now), FREE, REPAIR; its weekly share of slots. */
    public record RoomCell(long id, String number, String type, String status, int capacity, Double weekly) {
    }

    public record Floor(int floor, List<RoomCell> rooms) {
    }

    public record Stats(Double weeklyOccupancy, int busySlots, int totalSlots, int busyNow, int activeRooms,
                        int repairRooms, int capacity, int floorCount, List<Floor> floors,
                        Map<String, Integer> roomTypes) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    public BuildingStatsService(NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    public Map<Long, Stats> stats(Collection<Long> buildingIds) {
        if (buildingIds.isEmpty()) return Map.of();
        MapSqlParameterSource p = new MapSqlParameterSource("ids", buildingIds);
        // the week's periods of each building's school: distinct (weekday, start time) of its timetable
        Map<Long, Integer> periods = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select b.id, (select count(*) from (select distinct ls.weekday, ls.start_time
                                from lesson_slot ls join room r on r.id = ls.room_id
                                join building ob on ob.id = r.building_id
                               where ob.school_id = b.school_id) x) as n
                  from building b where b.id in (:ids)""", p)) {
            periods.put(((Number) r.get("id")).longValue(), ((Number) r.get("n")).intValue());
        }
        LocalDateTime now = LocalDateTime.now(clock);
        String weekday = WEEKDAYS.get(now.getDayOfWeek());
        Map<Long, int[]> busyByRoom = new HashMap<>(); // room → {slots a week, a lesson now}
        for (Map<String, Object> r : jdbc.queryForList("""
                select ls.room_id, count(distinct (ls.weekday, ls.start_time)) as slots,
                       bool_or(ls.weekday = :wd and ls.start_time <= :t and ls.end_time > :t) as now_busy
                  from lesson_slot ls join room r on r.id = ls.room_id
                 where r.building_id in (:ids) group by ls.room_id""",
                p.addValue("wd", weekday == null ? "" : weekday).addValue("t", Time.valueOf(now.toLocalTime().withNano(0))))) {
            busyByRoom.put(((Number) r.get("room_id")).longValue(),
                    new int[]{((Number) r.get("slots")).intValue(), Boolean.TRUE.equals(r.get("now_busy")) ? 1 : 0});
        }
        Map<Long, List<Map<String, Object>>> rooms = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select id, building_id, room_number, coalesce(floor, 1) as floor, coalesce(type, 'CLASSROOM') as type,
                       coalesce(status, 'ACTIVE') as status, capacity
                  from room where building_id in (:ids) order by floor, room_number""", p)) {
            rooms.computeIfAbsent(((Number) r.get("building_id")).longValue(), k -> new ArrayList<>()).add(r);
        }
        Map<Long, Stats> out = new HashMap<>();
        for (Long id : buildingIds) {
            int perRoom = periods.getOrDefault(id, 0);
            int busySlots = 0, active = 0, repair = 0, seats = 0, busyNow = 0;
            TreeMap<Integer, List<RoomCell>> floors = new TreeMap<>();
            Map<String, Integer> types = new LinkedHashMap<>();
            for (Map<String, Object> r : rooms.getOrDefault(id, List.of())) {
                long roomId = ((Number) r.get("id")).longValue();
                boolean underRepair = "REPAIR".equals(r.get("status"));
                int[] b = busyByRoom.getOrDefault(roomId, new int[2]);
                int capacity = ((Number) r.get("capacity")).intValue();
                String type = (String) r.get("type");
                types.merge(type, 1, Integer::sum);
                seats += capacity;
                String status;
                if (underRepair) {
                    repair++;
                    status = "REPAIR";
                } else {
                    active++;
                    busySlots += Math.min(b[0], perRoom);
                    busyNow += b[1];
                    status = b[1] == 1 ? "BUSY" : "FREE";
                }
                Double weekly = underRepair || perRoom == 0 ? null : round1(Math.min(b[0], perRoom) * 100.0 / perRoom);
                floors.computeIfAbsent(((Number) r.get("floor")).intValue(), k -> new ArrayList<>())
                        .add(new RoomCell(roomId, (String) r.get("room_number"), type, status, capacity, weekly));
            }
            int total = active * perRoom;
            List<Floor> floorList = floors.entrySet().stream().map(e -> new Floor(e.getKey(), e.getValue())).toList();
            out.put(id, new Stats(total == 0 ? null : round1(busySlots * 100.0 / total), busySlots, total, busyNow,
                    active, repair, seats, floors.isEmpty() ? 0 : floors.lastKey(), floorList, types));
        }
        return out;
    }

    private static Double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
