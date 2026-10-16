package uz.azizbek.maktabboshqaruv.service.attendance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Demo mode: once the app is up, the last {@code app.demo.startup-backfill-days} school days
 * (default 30) are filled with attendance and grades — in the background, so the app is usable at
 * once. Days that already have demo grades are skipped, so a restart costs almost nothing.
 */
@Component
public class DemoStartupFill {

    private static final Logger log = LoggerFactory.getLogger(DemoStartupFill.class);

    private final DemoMode demo;
    private final AttendanceAutomation automation;
    private final JdbcTemplate jdbc;
    private final int days;

    public DemoStartupFill(DemoMode demo, AttendanceAutomation automation, JdbcTemplate jdbc,
                           @Value("${app.demo.startup-backfill-days:30}") int days) {
        this.demo = demo;
        this.automation = automation;
        this.jdbc = jdbc;
        this.days = days;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (!demo.enabled() || days <= 0) return;
        Thread t = new Thread(this::fill, "demo-startup-fill");
        t.setDaemon(true);
        t.start();
    }

    void fill() {
        long started = System.currentTimeMillis();
        int attendance = 0;
        int grades = 0;
        try {
            for (Long schoolId : jdbc.queryForList("select id from school order by id", Long.class)) {
                int[] n = null;
                // the minute's scheduler run may hold the lock: wait for it
                for (int attempt = 0; n == null && attempt < 20; attempt++) {
                    n = automation.backfillDemo(schoolId, days, true);
                    if (n == null) Thread.sleep(3_000);
                }
                if (n == null) {
                    log.warn("DEMO startup fill: school {} skipped, the automation lock stayed busy", schoolId);
                    continue;
                }
                attendance += n[0];
                grades += n[1];
            }
            log.info("DEMO startup fill ({} school days): attendance inserted={} grades inserted={} in {} ms",
                    days, attendance, grades, System.currentTimeMillis() - started);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("DEMO startup fill failed", e);
        }
    }
}
