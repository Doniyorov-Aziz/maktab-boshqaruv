package uz.azizbek.maktabboshqaruv.service.dashboard;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import uz.azizbek.maktabboshqaruv.event.AttendanceChangedEvent;
import uz.azizbek.maktabboshqaruv.event.GradeSavedEvent;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Supplier;

/**
 * Home-page widgets are cached for 90 seconds per school (and per teacher for theirs). A saved
 * attendance mark drops that school's entries at once; a saved grade drops all (a grade event
 * carries no school id, and the cache is small).
 */
@Component
public class DashboardCache {

    /** A widget's data and the moment it was computed ("09:42 holatiga"). */
    public record Widget<T>(LocalDateTime asOf, T data) {
    }

    private final Cache<String, Widget<?>> cache = Caffeine.newBuilder()
            .maximumSize(2_000).expireAfterWrite(Duration.ofSeconds(90)).build();
    private final Clock clock;

    public DashboardCache(Clock clock) {
        this.clock = clock;
    }

    @SuppressWarnings("unchecked")
    public <T> Widget<T> get(long schoolId, String key, Supplier<T> compute) {
        return (Widget<T>) cache.get(schoolId + ":" + key, k -> new Widget<>(LocalDateTime.now(clock).withNano(0), compute.get()));
    }

    @EventListener
    public void onAttendance(AttendanceChangedEvent e) {
        String prefix = e.schoolId() + ":";
        cache.asMap().keySet().removeIf(k -> k.startsWith(prefix));
    }

    @EventListener
    public void onGrade(GradeSavedEvent e) {
        cache.invalidateAll();
    }

    /** A school's answers, all of them (a timetable, room or event change). */
    public void evictSchool(long schoolId) {
        String prefix = schoolId + ":";
        cache.asMap().keySet().removeIf(k -> k.startsWith(prefix));
    }

    public void clear() {
        cache.invalidateAll();
    }
}
