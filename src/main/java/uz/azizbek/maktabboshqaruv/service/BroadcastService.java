package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.bot.BotI18n;
import uz.azizbek.maktabboshqaruv.dto.BroadcastDto;
import uz.azizbek.maktabboshqaruv.dto.BroadcastRequestDto;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.BroadcastRepository;
import uz.azizbek.maktabboshqaruv.repository.NotificationLogRepository;
import uz.azizbek.maktabboshqaruv.repository.ParentTelegramLinkRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolClassRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import uz.azizbek.maktabboshqaruv.service.parent.ParentDataService;
import uz.azizbek.maktabboshqaruv.telegram.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * "Ota-onalarga xabar" (ADMIN): one message to the parents of the whole school
 * or of selected classes. Delivery goes through the outbox like everything
 * else; the result statistics are read back from it.
 */
@Service
public class BroadcastService {

    @Autowired
    private BroadcastRepository broadcastRepository;
    @Autowired
    private SchoolRepository schoolRepository;
    @Autowired
    private SchoolClassRepository schoolClassRepository;
    @Autowired
    private ParentTelegramLinkRepository linkRepository;
    @Autowired
    private NotificationLogRepository notificationLogRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private Clock clock;

    /** What parents will see, and how many chats would receive it — without sending anything. */
    public BroadcastDto preview(BroadcastRequestDto request) {
        School school = school(request.getSchoolId());
        BroadcastDto dto = new BroadcastDto();
        dto.setHtml(render("uz", request.getText(), school));
        dto.setRecipientCount(recipients(request).size());
        dto.setAudience(request.getAudience());
        dto.setClassNames(classNames(request.getClassIds()));
        return dto;
    }

    @Transactional
    public BroadcastDto send(BroadcastRequestDto request, String username) {
        School school = school(request.getSchoolId());
        List<NotificationService.Recipient> recipients = recipients(request);
        if (recipients.isEmpty()) {
            throw new IllegalStateException("Tanlangan auditoriyada botga ulangan ota-ona yo'q");
        }
        Broadcast b = new Broadcast();
        b.setSchool(school);
        b.setText(request.getText().strip());
        b.setAudience(BroadcastAudience.valueOf(request.getAudience()));
        b.setClassIds(request.getClassIds() == null ? null
                : request.getClassIds().stream().map(String::valueOf).collect(Collectors.joining(",")));
        b.setCreatedBy(username);
        b.setCreatedAt(LocalDateTime.now(clock));
        b.setRecipientCount(recipients.size());
        broadcastRepository.save(b);

        notificationService.createRows(school, NotificationType.BROADCAST, b.getId(), LocalDate.now(clock), recipients,
                lang -> render(lang, b.getText(), school), null);
        return toDto(b);
    }

    public Page<BroadcastDto> list(Long schoolId, Pageable pageable) {
        return broadcastRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId, pageable).map(this::toDto);
    }

    List<NotificationService.Recipient> recipients(BroadcastRequestDto request) {
        List<ParentTelegramLink> links;
        if ("CLASSES".equals(request.getAudience())) {
            if (request.getClassIds() == null || request.getClassIds().isEmpty()) {
                throw new IllegalStateException("Kamida bitta sinfni tanlang");
            }
            links = linkRepository.findActiveByClassIds(request.getClassIds());
        } else {
            links = linkRepository.findActiveBySchoolId(request.getSchoolId());
        }
        return NotificationService.oncePerChat(links);
    }

    private String render(String lang, String text, School school) {
        return BotI18n.get().t(lang, "notif.broadcast", "text", MessageFormatter.escape(text.strip()))
                + MessageFormatter.footer(lang, school.getName());
    }

    private School school(Long schoolId) {
        return schoolRepository.findById(schoolId).orElseThrow(() -> new IllegalStateException("Bunday maktab mavjud emas"));
    }

    private List<String> classNames(List<Long> ids) {
        if (ids == null) return List.of();
        List<String> names = new ArrayList<>();
        for (SchoolClass c : schoolClassRepository.findAllById(ids)) names.add(ParentDataService.className(c));
        names.sort(String::compareTo);
        return names;
    }

    private BroadcastDto toDto(Broadcast b) {
        BroadcastDto dto = new BroadcastDto();
        dto.setId(b.getId());
        dto.setText(b.getText());
        dto.setAudience(b.getAudience().name());
        dto.setClassNames(b.getClassIds() == null ? List.of()
                : classNames(Arrays.stream(b.getClassIds().split(",")).map(Long::valueOf).toList()));
        dto.setCreatedBy(b.getCreatedBy());
        dto.setCreatedAt(b.getCreatedAt());
        dto.setRecipientCount(b.getRecipientCount() == null ? 0 : b.getRecipientCount());
        dto.setSent(notificationLogRepository.countByTypeAndReferenceIdAndStatus(NotificationType.BROADCAST, b.getId(), NotificationStatus.SENT));
        dto.setPending(notificationLogRepository.countByTypeAndReferenceIdAndStatus(NotificationType.BROADCAST, b.getId(), NotificationStatus.PENDING));
        dto.setFailed(notificationLogRepository.countByTypeAndReferenceIdAndStatus(NotificationType.BROADCAST, b.getId(), NotificationStatus.FAILED));
        dto.setSkipped(notificationLogRepository.countByTypeAndReferenceIdAndStatus(NotificationType.BROADCAST, b.getId(), NotificationStatus.SKIPPED));
        return dto;
    }
}
