package uz.azizbek.maktabboshqaruv.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import uz.azizbek.maktabboshqaruv.service.attendance.DemoMode;

import java.util.List;
import java.util.Map;

/**
 * Demo mode only: rooms that have never been given equipment get realistic values once, so the
 * rooms page has something to show — projectors and smart boards in some classrooms, air
 * conditioning upstairs, computers in the computer room, and one unused classroom per school under
 * repair. A room someone already filled in (any of equipment / computers / status set) is never
 * touched; the fill runs only where all three are empty, so it never overwrites a real value.
 */
@Component
@Order(3)
public class RoomDetailsDemoFill implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RoomDetailsDemoFill.class);

    private final JdbcTemplate jdbc;
    private final DemoMode demo;

    public RoomDetailsDemoFill(JdbcTemplate jdbc, DemoMode demo) {
        this.jdbc = jdbc;
        this.demo = demo;
    }

    @Override
    public void run(String... args) {
        if (!demo.enabled()) return;
        try {
            fill();
        } catch (Exception e) {
            log.warn("Xonalar demo ma'lumoti to'ldirilmadi: {}", e.getMessage());
        }
    }

    public void fill() {
        List<Map<String, Object>> rooms = jdbc.queryForList("""
                select r.id, r.room_number, r.type, r.floor, b.school_id,
                       exists (select 1 from lesson_slot ls where ls.room_id = r.id) as used
                  from room r join building b on b.id = r.building_id
                 where r.equipment is null and r.computers is null and r.status is null
                 order by b.school_id, r.id""");
        if (rooms.isEmpty()) return;
        java.util.Set<Long> repairDone = new java.util.HashSet<>(jdbc.queryForList(
                "select distinct b.school_id from room r join building b on b.id = r.building_id where r.status = 'REPAIR'", Long.class));
        int n = 0;
        for (Map<String, Object> r : rooms) {
            long id = ((Number) r.get("id")).longValue();
            long school = ((Number) r.get("school_id")).longValue();
            String number = (String) r.get("room_number");
            String type = r.get("type") == null ? "CLASSROOM" : (String) r.get("type");
            int floor = r.get("floor") == null ? 1 : ((Number) r.get("floor")).intValue();
            boolean used = Boolean.TRUE.equals(r.get("used"));
            String status = "ACTIVE";
            if (!used && type.equals("CLASSROOM") && repairDone.add(school)) status = "REPAIR";
            List<String> tags = new java.util.ArrayList<>();
            Integer computers = null;
            switch (type) {
                case "COMPUTER_LAB" -> {
                    computers = 24;
                    tags.add("PROJECTOR");
                    tags.add("AC");
                }
                case "LIBRARY" -> tags.add("AC");
                case "LAB" -> tags.add("PROJECTOR");
                case "GYM" -> {
                }
                default -> {
                    if (id % 2 == 0) tags.add("PROJECTOR");
                    if (id % 3 == 0) tags.add("SMARTBOARD");
                    if (floor >= 2 || id % 4 == 0) tags.add("AC");
                }
            }
            // an empty string marks "filled, nothing to list" so the next start leaves the room alone
            jdbc.update("update room set equipment = ?, computers = ?, status = ? where id = ?",
                    tags.isEmpty() ? "" : String.join(",", tags), computers, status, id);
            n++;
            if (number != null && number.toLowerCase().contains("faollar")) {
                jdbc.update("update room set type = 'ASSEMBLY_HALL' where id = ?", id);
            }
        }
        log.info("DEMO: {} ta xonaga jihozlar va holat qo'yildi (faqat bo'sh xonalarga)", n);
    }
}
