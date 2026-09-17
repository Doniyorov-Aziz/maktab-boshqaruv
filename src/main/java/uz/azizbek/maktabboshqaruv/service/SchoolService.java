package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.SchoolRequestDto;
import uz.azizbek.maktabboshqaruv.dto.SchoolResponseDto;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchoolService {

    @Autowired
    private SchoolRepository schoolRepository;

    public Page<SchoolResponseDto> getAllSchools(Pageable pageable) {
        return schoolRepository.findAll(pageable)
                .map(this::toResponseDto);
    }

    public SchoolResponseDto getSchoolById(Long id) {
        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday maktab topilmadi: " + id));
        return toResponseDto(school);

    }

    @Transactional
    public SchoolResponseDto createSchool(SchoolRequestDto request){
        School school = new School();
        school.setName(request.getName());
        school.setAddress(request.getAddress());

        School saved = schoolRepository.save(school);
        return toResponseDto(saved);

    }

    @Transactional
    public SchoolResponseDto updateSchool(Long id, SchoolRequestDto request){

        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday maktab topilmadi: " + id));

        school.setName(request.getName());
        school.setAddress(request.getAddress());

        School updated = schoolRepository.save(school);
        return toResponseDto(updated);

    }

    @Transactional
    public void deleteSchool(Long id){
        if(!schoolRepository.existsById(id)){
            throw new IllegalStateException("Bunday maktab topilmadi: " + id) ;

        }
        schoolRepository.deleteById(id);
    }

    private SchoolResponseDto toResponseDto(School school){
        SchoolResponseDto dto = new SchoolResponseDto();
        dto.setId(school.getId());
        dto.setName(school.getName());
        dto.setAddress(school.getAddress());
        return dto;
    }

}