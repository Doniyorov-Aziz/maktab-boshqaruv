package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.*;
import uz.azizbek.maktabboshqaruv.entity.ParentTelegramLink;
import uz.azizbek.maktabboshqaruv.entity.SchoolClass;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.repository.ParentTelegramLinkRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolClassRepository;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import uz.azizbek.maktabboshqaruv.telegram.LinkCodeGenerator;
import uz.azizbek.maktabboshqaruv.telegram.TelegramProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Admin-side management of parent links and link codes. */
@Service
public class TelegramLinkService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private BotCache botCache;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private ParentTelegramLinkRepository linkRepository;

    @Autowired
    private TelegramProperties telegramProperties;

    @Autowired
    private TelegramUpdatePoller updatePoller;

    public TelegramStatusDto getStatus() {
        TelegramStatusDto dto = new TelegramStatusDto();
        dto.setMode(telegramProperties.mode().name());
        dto.setEnabled(telegramProperties.isEnabled());
        dto.setMock(telegramProperties.isMock());
        dto.setTokenConfigured(telegramProperties.hasToken());
        dto.setActive(telegramProperties.isActive());
        dto.setBotUsername(telegramProperties.getBotUsername().isBlank() ? null : telegramProperties.getBotUsername());
        dto.setLastError(updatePoller.getLastError());
        return dto;
    }

    @Transactional
    public StudentTelegramDto getStudentTelegram(Long studentId) {
        Student student = findStudent(studentId);
        ensureCode(student);
        List<ParentTelegramLink> links = linkRepository.findByStudentIdAndActiveTrueOrderByLinkedAtAsc(studentId);
        StudentTelegramDto dto = toDto(student, links.size());
        dto.setLinks(links.stream().map(this::toLinkDto).collect(Collectors.toList()));
        return dto;
    }

    /** New code; the old one stops working immediately, existing parent links stay. */
    @Transactional
    public StudentTelegramDto regenerateCode(Long studentId) {
        Student student = findStudent(studentId);
        student.setTelegramLinkCode(uniqueCode());
        studentRepository.save(student);
        return getStudentTelegram(studentId);
    }

    @Transactional
    public void unlink(Long linkId) {
        ParentTelegramLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> new IllegalStateException("Bunday bog'lanish topilmadi: " + linkId));
        link.setActive(false);
        linkRepository.save(link);
        botCache.evictChat(link.getChatId());
    }

    @Transactional
    public ClassTelegramCodesDto getClassCodes(Long schoolClassId) {
        SchoolClass schoolClass = schoolClassRepository.findById(schoolClassId)
                .orElseThrow(() -> new IllegalStateException("Bunday sinf mavjud emas"));
        Map<Long, Integer> counts = new HashMap<>();
        for (Object[] row : linkRepository.countActiveByStudentInClass(schoolClassId)) {
            counts.put((Long) row[0], ((Long) row[1]).intValue());
        }

        ClassTelegramCodesDto dto = new ClassTelegramCodesDto();
        dto.setSchoolClassId(schoolClass.getId());
        dto.setClassName(schoolClass.getGradeNumber() + "-" + schoolClass.getSectionLetter());
        dto.setSchoolName(schoolClass.getAcademicYear().getSchool().getName());
        dto.setBotUsername(telegramProperties.getBotUsername().isBlank() ? null : telegramProperties.getBotUsername());
        dto.setStudents(studentRepository.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(schoolClassId).stream()
                .map(s -> {
                    ensureCode(s);
                    return toDto(s, counts.getOrDefault(s.getId(), 0));
                })
                .collect(Collectors.toList()));
        return dto;
    }

    private void ensureCode(Student student) {
        if (student.getTelegramLinkCode() == null) {
            student.setTelegramLinkCode(uniqueCode());
            studentRepository.save(student);
        }
    }

    String uniqueCode() {
        String code;
        do {
            code = LinkCodeGenerator.generate();
        } while (studentRepository.existsByTelegramLinkCode(code));
        return code;
    }

    private Student findStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday o'quvchi topilmadi: " + id));
    }

    private StudentTelegramDto toDto(Student s, int linkedCount) {
        StudentTelegramDto dto = new StudentTelegramDto();
        dto.setStudentId(s.getId());
        dto.setStudentName(s.getFirstName() + " " + s.getLastName());
        dto.setClassName(s.getSchoolClass().getGradeNumber() + "-" + s.getSchoolClass().getSectionLetter());
        dto.setLinkCode(s.getTelegramLinkCode());
        dto.setDeepLink(telegramProperties.deepLink(s.getTelegramLinkCode()));
        dto.setLinkedCount(linkedCount);
        return dto;
    }

    private ParentLinkDto toLinkDto(ParentTelegramLink link) {
        ParentLinkDto dto = new ParentLinkDto();
        dto.setId(link.getId());
        dto.setFirstName(link.getFirstName());
        dto.setTelegramUsername(link.getTelegramUsername());
        dto.setLinkedAt(link.getLinkedAt());
        return dto;
    }
}
