package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.SchoolClassRequestDto;
import uz.azizbek.maktabboshqaruv.dto.SchoolClassResponseDto;
import uz.azizbek.maktabboshqaruv.entity.AcademicYear;
import uz.azizbek.maktabboshqaruv.entity.Employee;
import uz.azizbek.maktabboshqaruv.entity.SchoolClass;
import uz.azizbek.maktabboshqaruv.repository.AcademicYearRepository;
import uz.azizbek.maktabboshqaruv.repository.EmployeeRepository;
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

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private uz.azizbek.maktabboshqaruv.repository.StudentRepository studentRepository;

    public Page<SchoolClassResponseDto> getAllSchoolClasses(Long schoolId, Pageable pageable) {
        Page<SchoolClassResponseDto> page = schoolClassRepository.findByAcademicYearSchoolId(schoolId, pageable)
                .map(this::toResponseDto);
        // student counts for the class cards: one grouped query for the page
        java.util.List<Long> ids = page.getContent().stream().map(SchoolClassResponseDto::getId).toList();
        if (!ids.isEmpty()) {
            java.util.Map<Long, Long> counts = new java.util.HashMap<>();
            for (Object[] r : studentRepository.countByClassIds(ids)) counts.put((Long) r[0], (Long) r[1]);
            page.getContent().forEach(c -> c.setStudentCount(counts.getOrDefault(c.getId(), 0L)));
        }
        return page;
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
        schoolClass.setClassTeacher(resolveClassTeacher(request.getClassTeacherId()));

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
        schoolClass.setClassTeacher(resolveClassTeacher(request.getClassTeacherId()));

        SchoolClass updated = schoolClassRepository.save(schoolClass);
        return toResponseDto(updated);
    }

    private Employee resolveClassTeacher(Long classTeacherId) {
        if (classTeacherId == null) {
            return null;
        }
        return employeeRepository.findById(classTeacherId)
                .orElseThrow(() -> new IllegalStateException("Bunday xodim mavjud emas"));
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
        if (schoolClass.getClassTeacher() != null) {
            dto.setClassTeacherId(schoolClass.getClassTeacher().getId());
            dto.setClassTeacherName(schoolClass.getClassTeacher().getFirstName() + " " + schoolClass.getClassTeacher().getLastName());
        }
        return dto;
    }
}
