package uz.azizbek.maktabboshqaruv.service.attendance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Demo mode: the simulator fills attendance and grades nobody entered by hand (DEMO_SIM).
 * Only application.properties switches it ({@code app.demo.enabled}, on by default for now);
 * under the "prod" profile it is off whatever the property says — the app starts normally and
 * logs a warning.
 */
@Component
public class DemoMode {

    private static final Logger log = LoggerFactory.getLogger(DemoMode.class);

    private final boolean enabled;
    private final int delayMinutes;

    public DemoMode(Environment env,
                    @Value("${app.demo.enabled:${app.demo.attendance-simulator.enabled:true}}") boolean requested,
                    @Value("${app.demo.delay-minutes:${app.demo.attendance-simulator.delay-minutes:10}}") int delayMinutes) {
        String[] active = env.getActiveProfiles();
        boolean prod = Arrays.stream(active).anyMatch(p -> p.equalsIgnoreCase("prod"));
        String profile = active.length == 0 ? "default" : String.join(",", active);
        if (requested && prod) {
            log.warn("DEMO requested (app.demo.enabled=true) but profile={} — demo is OFF, nothing is simulated", profile);
        } else if (requested) {
            log.warn("DEMO enabled, profile={}, zone=Asia/Tashkent — attendance and grades nobody entered are simulated (DEMO_SIM)", profile);
        } else {
            log.info("DEMO disabled, profile={}", profile);
        }
        this.enabled = requested && !prod;
        this.delayMinutes = Math.max(1, delayMinutes);
    }

    public boolean enabled() {
        return enabled;
    }

    /** Minutes into a lesson before the simulator marks its attendance. */
    public int delayMinutes() {
        return delayMinutes;
    }
}
