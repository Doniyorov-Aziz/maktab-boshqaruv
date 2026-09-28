package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.SubjectRequestDto;
import uz.azizbek.maktabboshqaruv.dto.SubjectResponseDto;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.entity.Subject;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import uz.azizbek.maktabboshqaruv.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubjectService {

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    public Page<SubjectResponseDto> getAllSubjects(Long schoolId, Pageable pageable) {
        return subjectRepository.findBySchoolId(schoolId, pageable)
                .map(this::toResponseDto);
    }

    public SubjectResponseDto getSubjectById(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday fan topilmadi: " + id));
        return toResponseDto(subject);
    }

    @Transactional
    public SubjectResponseDto createSubject(SubjectRequestDto request) {
        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new IllegalStateException("Bunday maktab mavjud emas"));

        if (subjectRepository.existsBySchoolIdAndName(school.getId(), request.getName())) {
            throw new IllegalStateException("Bu fan ushbu maktabda allaqachon mavjud");
        }

        Subject subject = new Subject();
        subject.setSchool(school);
        subject.setName(request.getName());

        Subject saved = subjectRepository.save(subject);
        return toResponseDto(saved);
    }

    @Transactional
    public SubjectResponseDto updateSubject(Long id, SubjectRequestDto request) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday fan topilmadi: " + id));

        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new IllegalStateException("Bunday maktab mavjud emas"));

        boolean changed = !school.getId().equals(subject.getSchool() != null ? subject.getSchool().getId() : null)
                || !request.getName().equals(subject.getName());

        if (changed && subjectRepository.existsBySchoolIdAndName(school.getId(), request.getName())) {
            throw new IllegalStateException("Bu fan ushbu maktabda allaqachon mavjud");
        }

        subject.setSchool(school);
        subject.setName(request.getName());

        Subject updated = subjectRepository.save(subject);
        return toResponseDto(updated);
    }

    @Transactional
    public void deleteSubject(Long id) {
        if (!subjectRepository.existsById(id)) {
            throw new IllegalStateException("Bunday fan topilmadi: " + id);
        }
        subjectRepository.deleteById(id);
    }

    private SubjectResponseDto toResponseDto(Subject subject) {
        SubjectResponseDto dto = new SubjectResponseDto();
        dto.setId(subject.getId());
        dto.setName(subject.getName());
        if (subject.getSchool() != null) {
            dto.setSchoolId(subject.getSchool().getId());
            dto.setSchoolName(subject.getSchool().getName());
        }
        return dto;
    }
}
