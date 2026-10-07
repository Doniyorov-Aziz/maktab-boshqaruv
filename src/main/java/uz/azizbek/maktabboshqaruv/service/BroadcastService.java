package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.bot.BotI18n;
import uz.azizbek.maktabboshqaruv.dto.BroadcastDto;
import uz.azizbek.maktabboshqaruv.dto.BroadcastRecipientDto;
import uz.azizbek.maktabboshqaruv.dto.BroadcastRequestDto;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.BroadcastAttachmentRepository;
import uz.azizbek.maktabboshqaruv.repository.BroadcastRepository;
import uz.azizbek.maktabboshqaruv.repository.NotificationLogRepository;
import uz.azizbek.maktabboshqaruv.repository.ParentTelegramLinkRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolClassRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import uz.azizbek.maktabboshqaruv.service.appeal.AppealService;
import uz.azizbek.maktabboshqaruv.service.appeal.AttachmentPolicy;
import uz.azizbek.maktabboshqaruv.service.appeal.FileStorage;
import uz.azizbek.maktabboshqaruv.service.parent.ParentDataService;
import uz.azizbek.maktabboshqaruv.telegram.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * "Ota-onalarga xabar" (ADMIN): one message — text and/or up to 10 photos, videos or
 * files — to the parents of the whole school, of selected classes or to chosen parents.
 * Delivery goes through the outbox like everything else (rate limits, 429, retries);
 * each file is uploaded to Telegram once and then sent by its file_id. The result
 * statistics ("1250/3000 yuborildi") are read back from the outbox.
 */
@Service
public class BroadcastService {

    /** Telegram's limits: an album holds 10 items; a photo over 10 MB must go as a document. */
    public static final int MAX_FILES = 10;
    private static final long MAX_PHOTO_BYTES = 10L * 1024 * 1024;

    @Autowired
    private BroadcastRepository broadcastRepository;

    @Autowired
    private BroadcastAttachmentRepository attachmentRepository;

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
    private FileStorage storage;

    @Autowired
    private Clock clock;

    /** What parents will see, and how many chats would receive it — without sending anything. */
    public BroadcastDto preview(BroadcastRequestDto request) {
        School school = school(request.getSchoolId());
        BroadcastDto dto = new BroadcastDto();
        dto.setHtml(render("uz", text(request), school));
        dto.setRecipientCount(recipients(request).size());
        dto.setAudience(request.getAudience());
        dto.setClassNames(classNames(request.getClassIds()));
        dto.setChosenParents(request.getLinkIds() == null ? 0 : request.getLinkIds().size());
        return dto;
    }

    @Transactional
    public BroadcastDto send(BroadcastRequestDto request, String username) {
        return send(request, List.of(), username);
    }

    @Transactional
    public BroadcastDto send(BroadcastRequestDto request, List<MultipartFile> files, String username) {
        List<MultipartFile> parts = files == null ? List.of() : files.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (text(request).isEmpty() && parts.isEmpty()) {
            throw new IllegalStateException("Xabar matnini yozing yoki fayl biriktiring");
        }
        if (parts.size() > MAX_FILES) {
            throw new IllegalStateException("Ko'pi bilan " + MAX_FILES + " ta fayl biriktirish mumkin");
        }
        for (MultipartFile f : parts) {
            String reason = AttachmentPolicy.reject(kindOf(f), f.getOriginalFilename(), f.getContentType(), f.getSize());
            if (reason != null) throw new IllegalStateException((f.getOriginalFilename() == null ? "Fayl" : f.getOriginalFilename()) + ": " + reason);
        }
        School school = school(request.getSchoolId());
        List<NotificationService.Recipient> recipients = recipients(request);
        if (recipients.isEmpty()) {
            throw new IllegalStateException("Tanlangan auditoriyada botga ulangan ota-ona yo'q");
        }
        Broadcast b = new Broadcast();
        b.setSchool(school);
        b.setText(text(request));
        b.setAudience(BroadcastAudience.valueOf(request.getAudience()));
        b.setClassIds(joined("CLASSES".equals(request.getAudience()) ? request.getClassIds() : null));
        b.setChatIds(joined("PARENTS".equals(request.getAudience())
                ? recipients.stream().map(NotificationService.Recipient::chatId).toList() : null));
        b.setCreatedBy(username);
        b.setCreatedAt(LocalDateTime.now(clock));
        b.setRecipientCount(recipients.size());
        b.setMediaCount(parts.size());
        broadcastRepository.save(b);

        List<BroadcastAttachment> saved = new ArrayList<>();
        int position = 0;
        for (MultipartFile f : parts) {
            AppealEnums.Kind kind = kindOf(f);
            String ext = AttachmentPolicy.extensionOf(kind, f.getOriginalFilename(), f.getContentType());
            BroadcastAttachment a = new BroadcastAttachment();
            a.setBroadcast(b);
            a.setPosition(position++);
            a.setKind(kind);
            a.setMimeType(AttachmentPolicy.contentType(ext));
            a.setOriginalName(f.getOriginalFilename());
            a.setFileSize(f.getSize());
            try {
                a.setStoragePath(storage.save(school.getId(), ext, f.getBytes()));
            } catch (IOException e) {
                throw new IllegalStateException("Faylni saqlab bo'lmadi: " + f.getOriginalFilename());
            }
            saved.add(attachmentRepository.save(a));
        }

        notificationService.createRows(school, NotificationType.BROADCAST, b.getId(), LocalDate.now(clock), recipients,
                lang -> render(lang, b.getText(), school), null, saved.isEmpty() ? null : BroadcastMedia.ref(b.getId()));
        return toDto(b, counts(List.of(b.getId())), saved);
    }

    /** History, newest first; optional date range and text search. Counts and files come in two batch queries. */
    @Transactional(readOnly = true)
    public Page<BroadcastDto> list(Long schoolId, LocalDate from, LocalDate to, String q, Long classId, String status,
                                   Pageable pageable) {
        LocalDateTime since = (from == null ? LocalDate.of(2000, 1, 1) : from).atStartOfDay();
        LocalDateTime until = (to == null ? LocalDate.of(9999, 1, 1) : to.plusDays(1)).atStartOfDay();
        String like = q == null || q.isBlank() ? "" : "%" + q.strip().toLowerCase(Locale.ROOT) + "%";
        Page<Broadcast> page = broadcastRepository.search(schoolId, since, until, like, classId,
                status == null ? "" : status, pageable);
        List<Long> ids = page.getContent().stream().map(Broadcast::getId).toList();
        Map<Long, Map<NotificationStatus, Long>> counts = counts(ids);
        Map<Long, List<BroadcastAttachment>> files = new HashMap<>();
        if (!ids.isEmpty()) {
            for (BroadcastAttachment a : attachmentRepository.findByBroadcastIds(ids)) {
                files.computeIfAbsent(a.getBroadcast().getId(), k -> new ArrayList<>()).add(a);
            }
        }
        return page.map(b -> toDto(b, counts, files.getOrDefault(b.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public BroadcastDto get(Long id) {
        Broadcast b = find(id);
        return toDto(b, counts(List.of(id)), attachmentRepository.findByBroadcastIdOrderByPosition(id));
    }

    /** Who it went to — filter by class, status (SENT / PENDING / FAILED / SKIPPED) and name. */
    @Transactional(readOnly = true)
    public Page<BroadcastRecipientDto> recipients(Long broadcastId, String status, Long classId, String q, Pageable pageable) {
        find(broadcastId);
        String like = q == null || q.isBlank() ? "" : "%" + q.strip().toLowerCase(Locale.ROOT) + "%";
        return notificationLogRepository.broadcastRecipients(broadcastId, status == null ? "" : status, classId, like, pageable);
    }

    /** The school a broadcast belongs to (for the caller's school check). */
    public Long schoolOf(Long broadcastId) {
        return find(broadcastId).getSchool().getId();
    }

    List<NotificationService.Recipient> recipients(BroadcastRequestDto request) {
        List<ParentTelegramLink> links;
        switch (request.getAudience()) {
            case "CLASSES" -> {
                if (request.getClassIds() == null || request.getClassIds().isEmpty()) {
                    throw new IllegalStateException("Kamida bitta sinfni tanlang");
                }
                // only classes of this school
                Set<Long> own = schoolClassRepository.findByAcademicYearSchoolId(request.getSchoolId()).stream()
                        .map(SchoolClass::getId).collect(Collectors.toSet());
                List<Long> ids = request.getClassIds().stream().filter(own::contains).toList();
                if (ids.isEmpty()) throw new IllegalStateException("Kamida bitta sinfni tanlang");
                links = linkRepository.findActiveByClassIds(ids);
            }
            case "PARENTS" -> {
                if (request.getLinkIds() == null || request.getLinkIds().isEmpty()) {
                    throw new IllegalStateException("Kamida bitta ota-onani tanlang");
                }
                Set<Long> chosen = new HashSet<>(request.getLinkIds());
                // only active links to a child of this school
                links = linkRepository.findActiveBySchoolId(request.getSchoolId()).stream()
                        .filter(l -> chosen.contains(l.getId())).toList();
            }
            default -> links = linkRepository.findActiveBySchoolId(request.getSchoolId());
        }
        return NotificationService.oncePerChat(links);
    }

    private Map<Long, Map<NotificationStatus, Long>> counts(Collection<Long> ids) {
        Map<Long, Map<NotificationStatus, Long>> result = new HashMap<>();
        if (ids.isEmpty()) return result;
        for (Object[] row : notificationLogRepository.broadcastStatusCounts(ids)) {
            result.computeIfAbsent((Long) row[0], k -> new EnumMap<>(NotificationStatus.class))
                    .put((NotificationStatus) row[1], ((Number) row[2]).longValue());
        }
        return result;
    }

    private static AppealEnums.Kind kindOf(MultipartFile f) {
        AppealEnums.Kind kind = AppealService.kindOf(f.getContentType(), f.getOriginalFilename());
        // Telegram refuses photos over 10 MB; such a picture goes as a file
        return kind == AppealEnums.Kind.PHOTO && f.getSize() > MAX_PHOTO_BYTES ? AppealEnums.Kind.DOCUMENT : kind;
    }

    private static String text(BroadcastRequestDto request) {
        return request.getText() == null ? "" : request.getText().strip();
    }

    private static String joined(List<Long> ids) {
        return ids == null || ids.isEmpty() ? null : ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private String render(String lang, String text, School school) {
        return BotI18n.get().t(lang, "notif.broadcast", "text", MessageFormatter.escape(text.strip()))
                + MessageFormatter.footer(lang, school.getName());
    }

    private School school(Long schoolId) {
        return schoolRepository.findById(schoolId).orElseThrow(() -> new IllegalStateException("Bunday maktab mavjud emas"));
    }

    private Broadcast find(Long id) {
        return broadcastRepository.findById(id).orElseThrow(() -> new IllegalStateException("Xabar topilmadi"));
    }

    private List<String> classNames(List<Long> ids) {
        if (ids == null) return List.of();
        List<String> names = new ArrayList<>();
        for (SchoolClass c : schoolClassRepository.findAllById(ids)) names.add(ParentDataService.className(c));
        names.sort(String::compareTo);
        return names;
    }

    private BroadcastDto toDto(Broadcast b, Map<Long, Map<NotificationStatus, Long>> counts, List<BroadcastAttachment> files) {
        BroadcastDto dto = new BroadcastDto();
        dto.setId(b.getId());
        dto.setText(b.getText());
        dto.setAudience(b.getAudience().name());
        dto.setClassNames(b.getClassIds() == null ? List.of()
                : classNames(Arrays.stream(b.getClassIds().split(",")).map(Long::valueOf).toList()));
        dto.setChosenParents(b.getChatIds() == null ? 0 : b.getChatIds().split(",").length);
        dto.setCreatedBy(b.getCreatedBy());
        dto.setCreatedAt(b.getCreatedAt());
        dto.setRecipientCount(b.getRecipientCount() == null ? 0 : b.getRecipientCount());
        Map<NotificationStatus, Long> c = counts.getOrDefault(b.getId(), Map.of());
        dto.setSent(c.getOrDefault(NotificationStatus.SENT, 0L));
        dto.setPending(c.getOrDefault(NotificationStatus.PENDING, 0L));
        dto.setFailed(c.getOrDefault(NotificationStatus.FAILED, 0L));
        dto.setSkipped(c.getOrDefault(NotificationStatus.SKIPPED, 0L));
        dto.setAttachments(files.stream()
                .map(a -> new BroadcastDto.Attachment(a.getKind().name(), a.getOriginalName(), a.getFileSize(), a.getFileId() != null))
                .toList());
        return dto;
    }
}
