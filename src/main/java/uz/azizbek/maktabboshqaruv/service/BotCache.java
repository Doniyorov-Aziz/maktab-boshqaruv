package uz.azizbek.maktabboshqaruv.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import uz.azizbek.maktabboshqaruv.entity.LessonSlot;
import uz.azizbek.maktabboshqaruv.entity.Student;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * Short-lived caches for what every bot update reads: which children a chat is
 * linked to (5 min) and a class's weekly timetable (10 min). Writers evict the
 * affected entry right away (linking/unlinking a parent, editing a lesson), so
 * the TTL only bounds staleness for changes made outside those paths.
 * Values are detached, fully loaded entities — read-only by convention.
 */
@Component
public class BotCache {

    private final Cache<Long, List<Student>> chatStudents = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(50_000)
            .build();

    private final Cache<Long, List<LessonSlot>> classSlots = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(5_000)
            .build();

    /** A school's calendar (holidays, events) around today — read for every schedule/home page. */
    private final Cache<Long, List<uz.azizbek.maktabboshqaruv.entity.CalendarEvent>> schoolEvents = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(1_000)
            .build();

    public List<uz.azizbek.maktabboshqaruv.entity.CalendarEvent> events(Long schoolId,
            Supplier<List<uz.azizbek.maktabboshqaruv.entity.CalendarEvent>> loader) {
        return schoolEvents.get(schoolId, k -> List.copyOf(loader.get()));
    }

    public void evictEvents() {
        schoolEvents.invalidateAll();
    }

    public List<Student> students(long chatId, Supplier<List<Student>> loader) {
        return chatStudents.get(chatId, k -> List.copyOf(loader.get()));
    }

    public List<LessonSlot> slots(Long classId, Supplier<List<LessonSlot>> loader) {
        return classSlots.get(classId, k -> List.copyOf(loader.get()));
    }

    public void evictChat(long chatId) {
        chatStudents.invalidate(chatId);
    }

    /** A student was edited (e.g. moved to another class): every chat's list may be stale. */
    public void evictAllChats() {
        chatStudents.invalidateAll();
    }

    public void evictClass(Long classId) {
        if (classId != null) classSlots.invalidate(classId);
    }

    public void evictAllClasses() {
        classSlots.invalidateAll();
    }
}
