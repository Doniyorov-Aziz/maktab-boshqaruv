package uz.azizbek.maktabboshqaruv.service;

import jakarta.persistence.criteria.Predicate;
import uz.azizbek.maktabboshqaruv.dto.NotificationLogDto;
import uz.azizbek.maktabboshqaruv.dto.NotificationStatsDto;
import uz.azizbek.maktabboshqaruv.entity.NotificationLog;
import uz.azizbek.maktabboshqaruv.entity.NotificationStatus;
import uz.azizbek.maktabboshqaruv.entity.NotificationType;
import uz.azizbek.maktabboshqaruv.repository.NotificationLogRepository;
import uz.azizbek.maktabboshqaruv.repository.ParentTelegramLinkRepository;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Read side of the outbox for the "Xabarnomalar" page, plus manual retry. */
@Service
public class NotificationQueryService {

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Autowired
    private ParentTelegramLinkRepository linkRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private Clock clock;

    public Page<NotificationLogDto> search(Long schoolId, NotificationType type, NotificationStatus status,
                                           LocalDate from, LocalDate to, Pageable pageable) {
        Specification<NotificationLog> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("school").get("id"), schoolId));
            if (type != null) p.add(cb.equal(root.get("type"), type));
            if (status != null) p.add(cb.equal(root.get("status"), status));
            if (from != null) p.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
            if (to != null) p.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
            return cb.and(p.toArray(new Predicate[0]));
        };
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return notificationLogRepository.findAll(spec, sorted).map(this::toDto);
    }

    public NotificationStatsDto stats(Long schoolId) {
        LocalDateTime dayStart = LocalDate.now(clock).atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        NotificationStatsDto dto = new NotificationStatsDto();
        dto.setSentToday(notificationLogRepository.countBySchoolIdAndStatusAndSentAtBetween(
                schoolId, NotificationStatus.SENT, dayStart, dayEnd));
        dto.setFailedToday(notificationLogRepository.countBySchoolIdAndStatusAndCreatedAtBetween(
                schoolId, NotificationStatus.FAILED, dayStart, dayEnd));
        dto.setSkippedToday(notificationLogRepository.countBySchoolIdAndStatusAndCreatedAtBetween(
                schoolId, NotificationStatus.SKIPPED, dayStart, dayEnd));
        dto.setFailedTotal(notificationLogRepository.countBySchoolIdAndStatus(schoolId, NotificationStatus.FAILED));
        dto.setPending(notificationLogRepository.countBySchoolIdAndStatus(schoolId, NotificationStatus.PENDING));

        long total = studentRepository.countBySchoolClassAcademicYearSchoolId(schoolId);
        long linked = linkRepository.countLinkedStudentsBySchoolId(schoolId);
        dto.setTotalStudents(total);
        dto.setLinkedStudents(linked);
        dto.setLinkedPercent(total == 0 ? null : Math.round(linked * 1000.0 / total) / 10.0);
        dto.setParentCount(linkRepository.countParentsBySchoolId(schoolId));
        return dto;
    }

    @Transactional
    public NotificationLogDto retry(Long id) {
        NotificationLog n = notificationLogRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday xabarnoma topilmadi: " + id));
        if (n.getStatus() != NotificationStatus.FAILED) {
            throw new IllegalStateException("Faqat xato (FAILED) holatidagi xabarni qayta yuborish mumkin");
        }
        n.setStatus(NotificationStatus.PENDING);
        n.setAttempts(0);
        n.setScheduledAt(LocalDateTime.now(clock));
        n.setLastError(null);
        return toDto(notificationLogRepository.save(n));
    }

    private NotificationLogDto toDto(NotificationLog n) {
        NotificationLogDto dto = new NotificationLogDto();
        dto.setId(n.getId());
        dto.setType(n.getType());
        dto.setStatus(n.getStatus());
        dto.setStudentId(n.getStudent().getId());
        dto.setStudentName(n.getStudent().getFirstName() + " " + n.getStudent().getLastName());
        dto.setClassName(n.getStudent().getSchoolClass().getGradeNumber() + "-" + n.getStudent().getSchoolClass().getSectionLetter());
        dto.setReferenceId(n.getReferenceId());
        dto.setRecordDate(n.getRecordDate());
        dto.setText(n.getText());
        dto.setAttempts(n.getAttempts());
        dto.setLastError(n.getLastError());
        dto.setCreatedAt(n.getCreatedAt());
        dto.setScheduledAt(n.getScheduledAt());
        dto.setSentAt(n.getSentAt());
        return dto;
    }
}
