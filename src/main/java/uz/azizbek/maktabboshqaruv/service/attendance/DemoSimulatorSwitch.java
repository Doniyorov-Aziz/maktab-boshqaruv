package uz.azizbek.maktabboshqaruv.service.attendance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Whether the demo attendance simulator runs. Only application.properties can switch it on
 * ({@code app.demo.attendance-simulator.enabled=true}); under the "prod" profile it stays off
 * whatever the property says — the app starts normally and logs a warning.
 */
@Component
public class DemoSimulatorSwitch {

    private static final Logger log = LoggerFactory.getLogger(DemoSimulatorSwitch.class);

    private final boolean enabled;
    private final int delayMinutes;

    public DemoSimulatorSwitch(Environment env,
                               @Value("${app.demo.attendance-simulator.enabled:false}") boolean requested,
                               @Value("${app.demo.attendance-simulator.delay-minutes:10}") int delayMinutes) {
        boolean prod = Arrays.stream(env.getActiveProfiles()).anyMatch(p -> p.equalsIgnoreCase("prod"));
        if (requested && prod) {
            log.warn("DIQQAT: app.demo.attendance-simulator.enabled=true, lekin 'prod' profili yoqilgan — "
                    + "demo davomat simulyatori ISHLAMAYDI");
        } else if (requested) {
            log.warn("DEMO rejim: qo'lda olinmagan davomat avtomatik simulyatsiya qilinadi "
                    + "(app.demo.attendance-simulator.enabled=true)");
        }
        this.enabled = requested && !prod;
        this.delayMinutes = Math.max(1, delayMinutes);
    }

    public boolean enabled() {
        return enabled;
    }

    public int delayMinutes() {
        return delayMinutes;
    }
}
