package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.SchoolRequestDto;
import uz.azizbek.maktabboshqaruv.dto.SchoolResponseDto;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.repository.EmployeeRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolClassRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SchoolService {

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private SchoolAccessService access;

    @Autowired(required = false)
    private uz.azizbek.maktabboshqaruv.service.pages.SchoolStatsService schoolStats;

    /** A user of one school sees only that school; a user without a school sees all. */
    public Page<SchoolResponseDto> getAllSchools(Pageable pageable) {
        Long own = access.caller().schoolId();
        if (own != null) {
            java.util.List<SchoolResponseDto> one = schoolRepository.findById(own).map(this::toResponseDto).stream().toList();
            one.forEach(this::withCard);
            return new org.springframework.data.domain.PageImpl<>(one, pageable, one.size());
        }
        Page<SchoolResponseDto> page = schoolRepository.findAll(pageable).map(this::toResponseDto);
        page.getContent().forEach(this::withCard);
        return page;
    }

    private void withCard(SchoolResponseDto dto) {
        if (schoolStats != null) dto.setCard(schoolStats.card(dto.getId()));
    }

    public SchoolResponseDto getSchoolById(Long id) {
        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday maktab topilmadi: " + id));
        return toResponseDto(school);

    }

    @Transactional
    public SchoolResponseDto createSchool(SchoolRequestDto request){
        School school = new School();
        school.setName(request.getName());
        school.setAddress(request.getAddress());
        school.setColor(blankToNull(request.getColor()));

        School saved = schoolRepository.save(school);
        return toResponseDto(saved);

    }

    @Transactional
    public SchoolResponseDto updateSchool(Long id, SchoolRequestDto request){

        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday maktab topilmadi: " + id));

        school.setName(request.getName());
        school.setAddress(request.getAddress());
        school.setColor(blankToNull(request.getColor()));

        School updated = schoolRepository.save(school);
        return toResponseDto(updated);

    }

    @Transactional
    public void deleteSchool(Long id){
        if(!schoolRepository.existsById(id)){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday maktab topilmadi: " + id) ;

        }
        schoolRepository.deleteById(id);
    }

    private SchoolResponseDto toResponseDto(School school){
        SchoolResponseDto dto = new SchoolResponseDto();
        dto.setId(school.getId());
        dto.setName(school.getName());
        dto.setAddress(school.getAddress());
        dto.setColor(school.getColor());
        dto.setStudentCount(studentRepository.countBySchoolClassAcademicYearSchoolId(school.getId()));
        dto.setTeacherCount(employeeRepository.countBySchoolId(school.getId()));
        dto.setClassCount(schoolClassRepository.countByAcademicYearSchoolId(school.getId()));
        return dto;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }

}