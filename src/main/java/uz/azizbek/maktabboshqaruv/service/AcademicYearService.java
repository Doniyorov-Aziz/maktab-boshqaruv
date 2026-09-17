package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.AcademicYearRequestDto;
import uz.azizbek.maktabboshqaruv.dto.AcademicYearResponseDto;
import uz.azizbek.maktabboshqaruv.entity.AcademicYear;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.repository.AcademicYearRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AcademicYearService {

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    public List<AcademicYearResponseDto> getAllAcademicYears() {
        return academicYearRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    public AcademicYearResponseDto getAcademicYearById(Long id) {
        AcademicYear academicYear = academicYearRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday o'quv yili topilmadi: " + id));
        return toResponseDto(academicYear);
    }

    @Transactional
    public AcademicYearResponseDto createAcademicYear(AcademicYearRequestDto request) {
        validateDateRange(request);

        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new IllegalStateException("Bunday maktab mavjud emas"));

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
                .orElseThrow(() -> new IllegalStateException("Bunday o'quv yili topilmadi: " + id));

        validateDateRange(request);

        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new IllegalStateException("Bunday maktab mavjud emas"));

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
            throw new IllegalStateException("Bunday o'quv yili topilmadi: " + id);
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
