package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.SubjectRequestDto;
import uz.azizbek.maktabboshqaruv.dto.SubjectResponseDto;
import uz.azizbek.maktabboshqaruv.entity.Subject;
import uz.azizbek.maktabboshqaruv.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubjectService {

    @Autowired
    private SubjectRepository subjectRepository;

    public List<SubjectResponseDto> getAllSubjects() {
        return subjectRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    public SubjectResponseDto getSubjectById(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday fan topilmadi: " + id));
        return toResponseDto(subject);
    }

    @Transactional
    public SubjectResponseDto createSubject(SubjectRequestDto request) {
        if (subjectRepository.existsByName(request.getName())) {
            throw new IllegalStateException("Bu fan allaqachon mavjud");
        }

        Subject subject = new Subject();
        subject.setName(request.getName());

        Subject saved = subjectRepository.save(subject);
        return toResponseDto(saved);
    }

    @Transactional
    public SubjectResponseDto updateSubject(Long id, SubjectRequestDto request) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday fan topilmadi: " + id));

        if (!subject.getName().equals(request.getName()) && subjectRepository.existsByName(request.getName())) {
            throw new IllegalStateException("Bu fan allaqachon mavjud");
        }

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
        return dto;
    }
}
