package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.SchoolClassRequestDto;
import uz.azizbek.maktabboshqaruv.dto.SchoolClassResponseDto;
import uz.azizbek.maktabboshqaruv.entity.AcademicYear;
import uz.azizbek.maktabboshqaruv.entity.SchoolClass;
import uz.azizbek.maktabboshqaruv.repository.AcademicYearRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolClassRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchoolClassService {

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    public Page<SchoolClassResponseDto> getAllSchoolClasses(Pageable pageable) {
        return schoolClassRepository.findAll(pageable)
                .map(this::toResponseDto);
    }

    public SchoolClassResponseDto getSchoolClassById(Long id) {
        SchoolClass schoolClass = schoolClassRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday sinf topilmadi: " + id));
        return toResponseDto(schoolClass);
    }

    @Transactional
    public SchoolClassResponseDto createSchoolClass(SchoolClassRequestDto request) {
        AcademicYear academicYear = academicYearRepository.findById(request.getAcademicYearId())
                .orElseThrow(() -> new IllegalStateException("Bunday o'quv yili mavjud emas"));

        if (schoolClassRepository.existsByAcademicYearAndGradeNumberAndSectionLetter(
                academicYear, request.getGradeNumber(), request.getSectionLetter())) {
            throw new IllegalStateException("Bu sinf ushbu o'quv yilida allaqachon mavjud");
        }

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setAcademicYear(academicYear);
        schoolClass.setGradeNumber(request.getGradeNumber());
        schoolClass.setSectionLetter(request.getSectionLetter());
        schoolClass.setMaxStudents(request.getMaxStudents());

        SchoolClass saved = schoolClassRepository.save(schoolClass);
        return toResponseDto(saved);
    }

    @Transactional
    public SchoolClassResponseDto updateSchoolClass(Long id, SchoolClassRequestDto request) {
        SchoolClass schoolClass = schoolClassRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday sinf topilmadi: " + id));

        AcademicYear academicYear = academicYearRepository.findById(request.getAcademicYearId())
                .orElseThrow(() -> new IllegalStateException("Bunday o'quv yili mavjud emas"));

        boolean changed = !academicYear.getId().equals(schoolClass.getAcademicYear().getId())
                || !request.getGradeNumber().equals(schoolClass.getGradeNumber())
                || !request.getSectionLetter().equals(schoolClass.getSectionLetter());

        if (changed && schoolClassRepository.existsByAcademicYearAndGradeNumberAndSectionLetter(
                academicYear, request.getGradeNumber(), request.getSectionLetter())) {
            throw new IllegalStateException("Bu sinf ushbu o'quv yilida allaqachon mavjud");
        }

        schoolClass.setAcademicYear(academicYear);
        schoolClass.setGradeNumber(request.getGradeNumber());
        schoolClass.setSectionLetter(request.getSectionLetter());
        schoolClass.setMaxStudents(request.getMaxStudents());

        SchoolClass updated = schoolClassRepository.save(schoolClass);
        return toResponseDto(updated);
    }

    @Transactional
    public void deleteSchoolClass(Long id) {
        if (!schoolClassRepository.existsById(id)) {
            throw new IllegalStateException("Bunday sinf topilmadi: " + id);
        }
        schoolClassRepository.deleteById(id);
    }

    private SchoolClassResponseDto toResponseDto(SchoolClass schoolClass) {
        SchoolClassResponseDto dto = new SchoolClassResponseDto();
        dto.setId(schoolClass.getId());
        dto.setGradeNumber(schoolClass.getGradeNumber());
        dto.setSectionLetter(schoolClass.getSectionLetter());
        dto.setMaxStudents(schoolClass.getMaxStudents());
        dto.setAcademicYearId(schoolClass.getAcademicYear().getId());
        dto.setAcademicYearTitle(schoolClass.getAcademicYear().getTitle());
        return dto;
    }
}
