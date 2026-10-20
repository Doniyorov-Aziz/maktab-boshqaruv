package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.AcademicYearRequestDto;
import uz.azizbek.maktabboshqaruv.dto.AcademicYearResponseDto;
import uz.azizbek.maktabboshqaruv.entity.AcademicTerm;
import uz.azizbek.maktabboshqaruv.entity.AcademicYear;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.repository.AcademicYearRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.service.pages.ListStatsService;
import uz.azizbek.maktabboshqaruv.util.PageSort;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class AcademicYearService {

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private uz.azizbek.maktabboshqaruv.repository.AcademicTermRepository termRepository;

    @Autowired
    private ListStatsService listStats;

    public Page<AcademicYearResponseDto> getAllAcademicYears(Long schoolId, Pageable pageable) {
        return getAllAcademicYears(schoolId, null, pageable);
    }

    /** School years with a title search and their classes and students (one grouped query). */
    public Page<AcademicYearResponseDto> getAllAcademicYears(Long schoolId, String q, Pageable pageable) {
        Page<AcademicYearResponseDto> page = academicYearRepository.search(schoolId, PageSort.like(q),
                PageSort.map(pageable, SORTABLE)).map(this::toResponseDto);
        Map<Long, ListStatsService.YearStats> stats = listStats.years(schoolId);
        for (AcademicYearResponseDto dto : page.getContent()) {
            ListStatsService.YearStats st = stats.get(dto.getId());
            dto.setClassCount(st == null ? 0 : st.classes());
            dto.setStudentCount(st == null ? 0 : st.students());
        }
        return page;
    }

    private static final Map<String, List<String>> SORTABLE = Map.of(
            "id", List.of("id"), "title", List.of("title"), "startDate", List.of("startDate"), "endDate", List.of("endDate"));

    public AcademicYearResponseDto getAcademicYearById(Long id) {
        AcademicYear academicYear = academicYearRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday o'quv yili topilmadi: " + id));
        return toResponseDto(academicYear);
    }

    @Transactional
    public AcademicYearResponseDto createAcademicYear(AcademicYearRequestDto request) {
        validateDateRange(request);
        requireNoOverlap(request.getSchoolId(), null, request.getStartDate(), request.getEndDate());

        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday maktab mavjud emas"));

        AcademicYear academicYear = new AcademicYear();
        academicYear.setSchool(school);
        academicYear.setTitle(request.getTitle());
        academicYear.setStartDate(request.getStartDate());
        academicYear.setEndDate(request.getEndDate());

        AcademicYear saved = academicYearRepository.save(academicYear);
        return toResponseDto(saved);
    }

    @Transactional
    public AcademicYearResponseDto updateAcademicYear(Long id, AcademicYearRequestDto request) {
        AcademicYear academicYear = academicYearRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday o'quv yili topilmadi: " + id));

        validateDateRange(request);
        requireNoOverlap(request.getSchoolId(), id, request.getStartDate(), request.getEndDate());

        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday maktab mavjud emas"));

        academicYear.setSchool(school);
        academicYear.setTitle(request.getTitle());
        academicYear.setStartDate(request.getStartDate());
        academicYear.setEndDate(request.getEndDate());

        AcademicYear updated = academicYearRepository.save(academicYear);
        return toResponseDto(updated);
    }

    @Transactional
    public void deleteAcademicYear(Long id) {
        if (!academicYearRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday o'quv yili topilmadi: " + id);
        }
        termRepository.deleteByYear(id);
        academicYearRepository.deleteById(id);
    }

    // ------------------------------------------------------------------ overlap, current year, terms

    /** Two years of one school never share a day: 409 naming the other year. */
    private void requireNoOverlap(Long schoolId, Long selfId, LocalDate start, LocalDate end) {
        for (AcademicYear other : academicYearRepository.findBySchoolIdOrderByStartDateDesc(schoolId)) {
            if (other.getId().equals(selfId)) continue;
            if (!start.isAfter(other.getEndDate()) && !end.isBefore(other.getStartDate())) {
                throw new IllegalStateException("Sanalar «" + other.getTitle() + "» o'quv yili bilan ustma-ust tushadi ("
                        + other.getStartDate() + " — " + other.getEndDate() + ")");
            }
        }
    }

    /** "Joriy yil qilib belgilash": this one becomes the school's current year, the others stop being it. */
    @Transactional
    public AcademicYearResponseDto makeCurrent(Long id) {
        AcademicYear year = find(id);
        for (AcademicYear y : academicYearRepository.findBySchoolIdOrderByStartDateDesc(year.getSchool().getId())) {
            if (y.isCurrent() != y.getId().equals(id)) {
                y.setCurrent(y.getId().equals(id));
                academicYearRepository.save(y);
            }
        }
        return toResponseDto(year);
    }

    private AcademicYear find(Long id) {
        return academicYearRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday o'quv yili topilmadi: " + id));
    }

    /** A quarter or a vacation as the page sends and reads it. */
    public record Term(Long id, String name, String kind, LocalDate startDate, LocalDate endDate) {
    }

    public List<Term> terms(Long yearId) {
        find(yearId);
        return termRepository.findByAcademicYearIdOrderByStartDateAsc(yearId).stream()
                .map(t -> new Term(t.getId(), t.getName(), t.getKind().name(), t.getStartDate(), t.getEndDate())).toList();
    }

    /**
     * The year's quarters and vacations, all at once (the drawer saves the whole list): each inside
     * the year, ending after it starts, and no two overlapping — 409 otherwise, nothing saved.
     */
    @Transactional
    public List<Term> saveTerms(Long yearId, List<Term> terms) {
        AcademicYear year = find(yearId);
        List<Term> sorted = terms.stream().sorted(Comparator.comparing(Term::startDate,
                Comparator.nullsFirst(Comparator.naturalOrder()))).toList();
        Term before = null;
        for (Term t : sorted) {
            if (t.name() == null || t.name().isBlank() || t.startDate() == null || t.endDate() == null) {
                throw new IllegalStateException("Har bir chorak va ta'tilning nomi va ikkala sanasi bo'lishi kerak");
            }
            if (t.endDate().isBefore(t.startDate())) {
                throw new IllegalStateException("«" + t.name() + "»: tugash sanasi boshlanishidan oldin");
            }
            if (t.startDate().isBefore(year.getStartDate()) || t.endDate().isAfter(year.getEndDate())) {
                throw new IllegalStateException("«" + t.name() + "» o'quv yili ichida bo'lishi kerak ("
                        + year.getStartDate() + " — " + year.getEndDate() + ")");
            }
            if (before != null && !t.startDate().isAfter(before.endDate())) {
                throw new IllegalStateException("«" + before.name() + "» va «" + t.name() + "» ustma-ust tushadi");
            }
            before = t;
        }
        termRepository.deleteByYear(yearId);
        termRepository.flush();
        for (Term t : sorted) {
            AcademicTerm e = new AcademicTerm();
            e.setAcademicYear(year);
            e.setName(t.name().strip());
            e.setKind("VACATION".equals(t.kind()) ? AcademicTerm.Kind.VACATION : AcademicTerm.Kind.QUARTER);
            e.setStartDate(t.startDate());
            e.setEndDate(t.endDate());
            termRepository.save(e);
        }
        return terms(yearId);
    }

    /** The next year's proposal: title, dates and terms of the latest year moved one year on (or the usual calendar). */
    public record NextYear(String title, LocalDate startDate, LocalDate endDate, List<Term> terms) {
    }

    public NextYear nextTemplate(Long schoolId) {
        List<AcademicYear> years = academicYearRepository.findBySchoolIdOrderByStartDateDesc(schoolId);
        if (years.isEmpty()) {
            int y = LocalDate.now().getMonthValue() >= 7 ? LocalDate.now().getYear() : LocalDate.now().getYear() - 1;
            return usualCalendar(y);
        }
        AcademicYear last = years.get(0);
        int y = last.getStartDate().getYear() + 1;
        List<Term> terms = termRepository.findByAcademicYearIdOrderByStartDateAsc(last.getId()).stream()
                .map(t -> new Term(null, t.getName(), t.getKind().name(), t.getStartDate().plusYears(1), t.getEndDate().plusYears(1)))
                .toList();
        if (terms.isEmpty()) terms = usualCalendar(y).terms();
        LocalDate start = last.getStartDate().plusYears(1);
        LocalDate end = last.getEndDate().plusYears(1);
        // the new year starts after the last one ends, whatever its dates were
        if (!start.isAfter(last.getEndDate())) start = last.getEndDate().plusDays(1);
        return new NextYear(y + "–" + (y + 1), start, end, terms);
    }

    /** Uzbekistan's usual school calendar: 2 September – 25 May, four quarters and three vacations. */
    static NextYear usualCalendar(int y) {
        List<Term> t = List.of(
                new Term(null, "1-chorak", "QUARTER", LocalDate.of(y, 9, 2), LocalDate.of(y, 11, 2)),
                new Term(null, "Kuzgi ta'til", "VACATION", LocalDate.of(y, 11, 3), LocalDate.of(y, 11, 9)),
                new Term(null, "2-chorak", "QUARTER", LocalDate.of(y, 11, 10), LocalDate.of(y, 12, 28)),
                new Term(null, "Qishki ta'til", "VACATION", LocalDate.of(y, 12, 29), LocalDate.of(y + 1, 1, 11)),
                new Term(null, "3-chorak", "QUARTER", LocalDate.of(y + 1, 1, 12), LocalDate.of(y + 1, 3, 20)),
                new Term(null, "Bahorgi ta'til", "VACATION", LocalDate.of(y + 1, 3, 21), LocalDate.of(y + 1, 3, 29)),
                new Term(null, "4-chorak", "QUARTER", LocalDate.of(y + 1, 3, 30), LocalDate.of(y + 1, 5, 25)));
        return new NextYear(y + "–" + (y + 1), LocalDate.of(y, 9, 2), LocalDate.of(y + 1, 5, 25), t);
    }

    /** "Keyingi yilni yaratish": the proposal (as the page may have changed it) saved with its terms. */
    @Transactional
    public AcademicYearResponseDto createNext(Long schoolId, NextYear next) {
        AcademicYearRequestDto r = new AcademicYearRequestDto();
        r.setSchoolId(schoolId);
        r.setTitle(next.title());
        r.setStartDate(next.startDate());
        r.setEndDate(next.endDate());
        AcademicYearResponseDto created = createAcademicYear(r);
        if (next.terms() != null && !next.terms().isEmpty()) saveTerms(created.getId(), next.terms());
        return created;
    }

    private void validateDateRange(AcademicYearRequestDto request) {
        if (!request.getStartDate().isBefore(request.getEndDate())) {
            throw new IllegalStateException("Boshlanish sanasi tugash sanasidan oldin bo'lishi kerak");
        }
    }

    private AcademicYearResponseDto toResponseDto(AcademicYear academicYear) {
        AcademicYearResponseDto dto = new AcademicYearResponseDto();
        dto.setId(academicYear.getId());
        dto.setTitle(academicYear.getTitle());
        dto.setStartDate(academicYear.getStartDate());
        dto.setEndDate(academicYear.getEndDate());
        dto.setSchoolId(academicYear.getSchool().getId());
        dto.setSchoolName(academicYear.getSchool().getName());
        dto.setCurrent(academicYear.isCurrent());
        return dto;
    }
}
