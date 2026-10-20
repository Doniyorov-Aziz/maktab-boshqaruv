package uz.azizbek.maktabboshqaruv.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uz.azizbek.maktabboshqaruv.entity.AcademicTerm;
import uz.azizbek.maktabboshqaruv.entity.AcademicYear;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.entity.SchoolClass;
import uz.azizbek.maktabboshqaruv.repository.AcademicTermRepository;
import uz.azizbek.maktabboshqaruv.repository.AcademicYearRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolClassRepository;
import uz.azizbek.maktabboshqaruv.service.AcademicYearService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * The demo schools' school year of today. The seeder's years stop at 2025-2026, so on 20.10.2026 no
 * year held today and the pages had no current year. This adds the year after the seeder's latest
 * one (2026-2027: 02.09.2026 – 31.05.2027, four quarters and three vacations), marks it current and
 * moves the latest year's classes — with their students, lessons, attendance and grades — into it.
 * <p>
 * Only once and only the next year: when a year already holds today nothing happens, and a clock far
 * from the latest year (a test on 2031-03-03) adds nothing. Real years a user made are never moved:
 * classes move only out of the seeder's own latest year, and only when the new year is created here.
 */
@Component
public class CurrentYearSeeder {

    private static final Logger log = LoggerFactory.getLogger(CurrentYearSeeder.class);

    /** The seeder names its years "2025-2026" (a hyphen); the page writes "2026–2027". */
    private static final java.util.regex.Pattern SEEDED = java.util.regex.Pattern.compile("\\d{4}-\\d{4}");

    private final AcademicYearRepository years;
    private final AcademicTermRepository terms;
    private final SchoolClassRepository classes;

    public CurrentYearSeeder(AcademicYearRepository years, AcademicTermRepository terms, SchoolClassRepository classes) {
        this.years = years;
        this.terms = terms;
        this.classes = classes;
    }

    /** The school's year of today: the existing one, the one created now, or empty when neither fits. */
    @Transactional
    public Optional<AcademicYear> ensure(School school, LocalDate today) {
        List<AcademicYear> all = years.findBySchoolIdOrderByStartDateDesc(school.getId());
        for (AcademicYear y : all) {
            if (!today.isBefore(y.getStartDate()) && !today.isAfter(y.getEndDate())) return Optional.of(y);
        }
        int y = today.getMonthValue() >= 7 ? today.getYear() : today.getYear() - 1;
        String title = y + "-" + (y + 1);
        LocalDate start = LocalDate.of(y, 9, 2);
        LocalDate end = LocalDate.of(y + 1, 5, 31);
        Optional<AcademicYear> sameTitle = all.stream().filter(a -> a.getTitle().equals(title)).findFirst();
        if (sameTitle.isPresent()) return sameTitle;
        // only the year right after the seeder's latest one, and never over another year's days
        AcademicYear latest = all.isEmpty() ? null : all.get(0);
        if (latest == null || !SEEDED.matcher(latest.getTitle()).matches() || latest.getStartDate().getYear() + 1 != y) {
            return Optional.empty();
        }
        boolean overlaps = all.stream().anyMatch(a -> !start.isAfter(a.getEndDate()) && !end.isBefore(a.getStartDate()));
        if (overlaps) return Optional.empty();

        AcademicYear year = new AcademicYear();
        year.setSchool(school);
        year.setTitle(title);
        year.setStartDate(start);
        year.setEndDate(end);
        year.setCurrent(true);
        year = years.save(year);
        for (AcademicYear other : all) {
            if (other.isCurrent()) {
                other.setCurrent(false);
                years.save(other);
            }
        }
        for (AcademicYearService.Term t : AcademicYearService.usualCalendar(y).terms()) {
            AcademicTerm e = new AcademicTerm();
            e.setAcademicYear(year);
            e.setName(t.name());
            e.setKind("VACATION".equals(t.kind()) ? AcademicTerm.Kind.VACATION : AcademicTerm.Kind.QUARTER);
            e.setStartDate(t.startDate());
            e.setEndDate(t.endDate());
            terms.save(e);
        }
        int moved = 0;
        for (SchoolClass c : classes.findByAcademicYearSchoolId(school.getId())) {
            if (c.getAcademicYear().getId().equals(latest.getId())) {
                c.setAcademicYear(year);
                classes.save(c);
                moved++;
            }
        }
        log.info("DataSeeder: {} — {} o'quv yili yaratildi va joriy qilindi, {} ta sinf ko'chirildi",
                school.getName(), title, moved);
        return Optional.of(year);
    }
}
