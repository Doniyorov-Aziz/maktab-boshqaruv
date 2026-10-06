package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.BehaviorRecordRequestDto;
import uz.azizbek.maktabboshqaruv.dto.BehaviorRecordResponseDto;
import uz.azizbek.maktabboshqaruv.entity.BehaviorRecord;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.repository.BehaviorRecordRepository;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BehaviorRecordService {

    @Autowired
    private BehaviorRecordRepository behaviorRecordRepository;

    @Autowired
    private StudentRepository studentRepository;

    public Page<BehaviorRecordResponseDto> getAllRecords(Long schoolId, Pageable pageable) {
        return getAllRecords(schoolId, null, pageable);
    }

    public Page<BehaviorRecordResponseDto> getAllRecords(Long schoolId, Long schoolClassId, Pageable pageable) {
        return behaviorRecordRepository.search(schoolId, schoolClassId, pageable).map(this::toResponseDto);
    }

    private static String currentUsername() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    @Transactional
    public BehaviorRecordResponseDto createRecord(BehaviorRecordRequestDto request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new IllegalStateException("Bunday o'quvchi mavjud emas"));

        BehaviorRecord record = new BehaviorRecord();
        record.setStudent(student);
        record.setRecordDate(request.getRecordDate());
        record.setType(request.getType());
        record.setDescription(request.getDescription());
        record.setCreatedBy(currentUsername());

        BehaviorRecord saved = behaviorRecordRepository.save(record);
        return toResponseDto(saved);
    }

    @Transactional
    public BehaviorRecordResponseDto updateRecord(Long id, BehaviorRecordRequestDto request) {
        BehaviorRecord record = behaviorRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday xulq yozuvi topilmadi: " + id));
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new IllegalStateException("Bunday o'quvchi mavjud emas"));

        record.setStudent(student);
        record.setRecordDate(request.getRecordDate());
        record.setType(request.getType());
        record.setDescription(request.getDescription());

        BehaviorRecord updated = behaviorRecordRepository.save(record);
        return toResponseDto(updated);
    }

    @Transactional
    public void deleteRecord(Long id) {
        if (!behaviorRecordRepository.existsById(id)) {
            throw new IllegalStateException("Bunday xulq yozuvi topilmadi: " + id);
        }
        behaviorRecordRepository.deleteById(id);
    }

    private BehaviorRecordResponseDto toResponseDto(BehaviorRecord record) {
        BehaviorRecordResponseDto dto = new BehaviorRecordResponseDto();
        dto.setId(record.getId());
        dto.setStudentId(record.getStudent().getId());
        dto.setStudentName(record.getStudent().getFirstName() + " " + record.getStudent().getLastName());
        dto.setRecordDate(record.getRecordDate());
        dto.setType(record.getType());
        dto.setDescription(record.getDescription());
        Student s = record.getStudent();
        if (s.getSchoolClass() != null) {
            dto.setSchoolClassId(s.getSchoolClass().getId());
            dto.setClassName(s.getSchoolClass().getGradeNumber() + "-" + s.getSchoolClass().getSectionLetter());
        }
        dto.setGuardianName(s.getGuardianName());
        dto.setGuardianPhone(s.getGuardianPhone());
        dto.setCreatedBy(record.getCreatedBy());
        dto.setCreatedDate(record.getCreatedDate());
        return dto;
    }
}
