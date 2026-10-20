package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.AcademicYearRequestDto;
import uz.azizbek.maktabboshqaruv.dto.AcademicYearResponseDto;
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

import java.util.List;
import java.util.Map;

@Service
public class AcademicYearService {

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private SchoolRepository schoolRepository;

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
        academicYearRepository.deleteById(id);
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
        return dto;
    }
}
