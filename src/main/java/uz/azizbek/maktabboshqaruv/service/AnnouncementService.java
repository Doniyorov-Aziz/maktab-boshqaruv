package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.AnnouncementRequestDto;
import uz.azizbek.maktabboshqaruv.dto.AnnouncementResponseDto;
import uz.azizbek.maktabboshqaruv.entity.Announcement;
import uz.azizbek.maktabboshqaruv.entity.AnnouncementAudience;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.entity.SchoolClass;
import uz.azizbek.maktabboshqaruv.event.AnnouncementCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import uz.azizbek.maktabboshqaruv.repository.AnnouncementRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolClassRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnnouncementService {

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public Page<AnnouncementResponseDto> getAllAnnouncements(Long schoolId, Pageable pageable) {
        return announcementRepository.findBySchoolId(schoolId, pageable).map(this::toResponseDto);
    }

    public List<AnnouncementResponseDto> getLatest(Long schoolId, int limit) {
        return announcementRepository.findLatestBySchoolId(schoolId, PageRequest.of(0, limit)).stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    public AnnouncementResponseDto getAnnouncementById(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday e'lon topilmadi: " + id));
        return toResponseDto(announcement);
    }

    @Transactional
    public AnnouncementResponseDto createAnnouncement(AnnouncementRequestDto request) {
        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new IllegalStateException("Bunday maktab mavjud emas"));

        Announcement announcement = new Announcement();
        announcement.setSchool(school);
        applyFields(announcement, request);

        Announcement saved = announcementRepository.save(announcement);
        activityLogService.record(school, "campaign", "\"" + announcement.getTitle() + "\" e'loni joylandi");
        eventPublisher.publishEvent(new AnnouncementCreatedEvent(saved.getId()));
        return toResponseDto(saved);
    }

    @Transactional
    public AnnouncementResponseDto updateAnnouncement(Long id, AnnouncementRequestDto request) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday e'lon topilmadi: " + id));

        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new IllegalStateException("Bunday maktab mavjud emas"));

        announcement.setSchool(school);
        applyFields(announcement, request);

        Announcement updated = announcementRepository.save(announcement);
        return toResponseDto(updated);
    }

    @Transactional
    public void deleteAnnouncement(Long id) {
        if (!announcementRepository.existsById(id)) {
            throw new IllegalStateException("Bunday e'lon topilmadi: " + id);
        }
        announcementRepository.deleteById(id);
    }

    private void applyFields(Announcement announcement, AnnouncementRequestDto request) {
        announcement.setTitle(request.getTitle());
        announcement.setContent(request.getContent());
        announcement.setAudience(request.getAudience());
        announcement.setPriority(request.getPriority());
        announcement.setDeadline(request.getDeadline());

        if (request.getAudience() == AnnouncementAudience.CLASS) {
            if (request.getSchoolClassId() == null) {
                throw new IllegalStateException("Sinf tanlanishi shart");
            }
            SchoolClass schoolClass = schoolClassRepository.findById(request.getSchoolClassId())
                    .orElseThrow(() -> new IllegalStateException("Bunday sinf mavjud emas"));
            announcement.setSchoolClass(schoolClass);
        } else {
            announcement.setSchoolClass(null);
        }
    }

    private AnnouncementResponseDto toResponseDto(Announcement announcement) {
        AnnouncementResponseDto dto = new AnnouncementResponseDto();
        dto.setId(announcement.getId());
        dto.setSchoolId(announcement.getSchool().getId());
        dto.setTitle(announcement.getTitle());
        dto.setContent(announcement.getContent());
        dto.setAudience(announcement.getAudience());
        if (announcement.getSchoolClass() != null) {
            dto.setSchoolClassId(announcement.getSchoolClass().getId());
            dto.setSchoolClassName(announcement.getSchoolClass().getGradeNumber() + "-" + announcement.getSchoolClass().getSectionLetter());
        }
        dto.setPriority(announcement.getPriority());
        dto.setDeadline(announcement.getDeadline());
        dto.setCreatedDate(announcement.getCreatedDate());
        return dto;
    }
}
