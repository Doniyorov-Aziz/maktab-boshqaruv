package uz.azizbek.maktabboshqaruv.service.attendance;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uz.azizbek.maktabboshqaruv.entity.CalendarEvent;
import uz.azizbek.maktabboshqaruv.entity.CalendarEventType;
import uz.azizbek.maktabboshqaruv.repository.CalendarEventRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** School days: Monday–Saturday, minus the school's holidays and vacations (Taqvim). */
@Component
public class SchoolDays {

    public static final Map<DayOfWeek, String> WEEKDAY_UZ = Map.of(
            DayOfWeek.MONDAY, "Dushanba", DayOfWeek.TUESDAY, "Seshanba", DayOfWeek.WEDNESDAY, "Chorshanba",
            DayOfWeek.THURSDAY, "Payshanba", DayOfWeek.FRIDAY, "Juma", DayOfWeek.SATURDAY, "Shanba");

    private final CalendarEventRepository events;

    public SchoolDays(CalendarEventRepository events) {
        this.events = events;
    }

    /** Holidays and vacations of the school between the two dates (inclusive), one query. */
    @Transactional(readOnly = true)
    public Set<LocalDate> daysOff(long schoolId, LocalDate from, LocalDate to) {
        Set<LocalDate> off = new HashSet<>();
        for (CalendarEvent e : events.findInRange(schoolId, from, to)) {
            if (e.getType() != CalendarEventType.HOLIDAY && e.getType() != CalendarEventType.VACATION) continue;
            for (LocalDate d = e.getStartDate().isBefore(from) ? from : e.getStartDate();
                 !d.isAfter(e.getEndDate()) && !d.isAfter(to); d = d.plusDays(1)) {
                off.add(d);
            }
        }
        return off;
    }

    /** A predicate for "is a school day" that answers from one query over the given range. */
    public Predicate<LocalDate> predicate(long schoolId, LocalDate from, LocalDate to) {
        Set<LocalDate> off = daysOff(schoolId, from, to);
        return d -> d.getDayOfWeek() != DayOfWeek.SUNDAY && !off.contains(d);
    }

    public boolean isSchoolDay(long schoolId, LocalDate date) {
        return predicate(schoolId, date, date).test(date);
    }

    /** The last {@code count} school days up to and including {@code end}, oldest first. */
    public List<LocalDate> lastSchoolDays(long schoolId, LocalDate end, int count) {
        Predicate<LocalDate> isSchool = predicate(schoolId, end.minusDays(count * 3L + 30), end);
        List<LocalDate> days = new ArrayList<>();
        for (LocalDate d = end; days.size() < count && d.isAfter(end.minusDays(count * 3L + 30)); d = d.minusDays(1)) {
            if (isSchool.test(d)) days.add(0, d);
        }
        return days;
    }
}
