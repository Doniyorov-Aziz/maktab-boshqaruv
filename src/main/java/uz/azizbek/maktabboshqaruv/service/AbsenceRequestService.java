package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.bot.BotI18n;
import uz.azizbek.maktabboshqaruv.dto.AbsenceRequestDto;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.AbsenceRequestRepository;
import uz.azizbek.maktabboshqaruv.repository.AttendanceRepository;
import uz.azizbek.maktabboshqaruv.repository.LessonSlotRepository;
import uz.azizbek.maktabboshqaruv.service.parent.ParentStats;
import uz.azizbek.maktabboshqaruv.telegram.MessageFormatter;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * "🤒 Sababli ariza": created by a parent in the bot, decided by the class
 * teacher or administration in the admin panel. Approval marks every lesson
 * of the requested days EXCUSED — existing attendance records are updated,
 * missing ones (future days) are created in advance.
 */
@Service
public class AbsenceRequestService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    static final int MAX_DAYS = 14;

    @Autowired
    private AbsenceRequestRepository repository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private LessonSlotRepository lessonSlotRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private Clock clock;

    @Transactional
    public AbsenceRequest create(Student student, long chatId, TelegramModels.User from, LocalDate dateFrom, int days,
                                 AbsenceReason reason, String comment, String photoFileId) {
        int span = Math.max(1, Math.min(days, MAX_DAYS));
        AbsenceRequest r = new AbsenceRequest();
        r.setSchool(NotificationService.schoolOf(student));
        r.setStudent(student);
        r.setChatId(chatId);
        r.setParentName(from == null ? null : from.firstName());
        r.setDateFrom(dateFrom);
        r.setDateTo(dateFrom.plusDays(span - 1));
        r.setReason(reason);
        r.setComment(comment == null || comment.isBlank() ? null : comment.strip());
        r.setPhotoFileId(photoFileId);
        r.setStatus(AbsenceStatus.PENDING);
        r.setCreatedAt(LocalDateTime.now(clock));
        return repository.save(r);
    }

    public Page<AbsenceRequestDto> list(Long schoolId, AbsenceStatus status, Pageable pageable) {
        Page<AbsenceRequest> page = status == null
                ? repository.findBySchoolIdOrderByCreatedAtDesc(schoolId, pageable)
                : repository.findBySchoolIdAndStatusOrderByCreatedAtDesc(schoolId, status, pageable);
        return page.map(this::toDto);
    }

    public long countPending(Long schoolId) {
        return repository.countBySchoolIdAndStatus(schoolId, AbsenceStatus.PENDING);
    }

    public AbsenceRequest get(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalStateException("Bunday ariza topilmadi: " + id));
    }

    @Transactional
    public AbsenceRequestDto approve(Long id, String username) {
        AbsenceRequest r = get(id);
        requirePending(r);
        int excused = excuse(r);
        r.setStatus(AbsenceStatus.APPROVED);
        r.setDecidedBy(username);
        r.setDecidedAt(LocalDateTime.now(clock));
        r.setExcusedLessons(excused);
        repository.save(r);
        notify(r, "abs.approved", null);
        return toDto(r);
    }

    @Transactional
    public AbsenceRequestDto reject(Long id, String reason, String username) {
        AbsenceRequest r = get(id);
        requirePending(r);
        if (reason == null || reason.isBlank()) {
            throw new IllegalStateException("Rad etish sababini yozing");
        }
        r.setStatus(AbsenceStatus.REJECTED);
        r.setDecisionNote(reason.strip());
        r.setDecidedBy(username);
        r.setDecidedAt(LocalDateTime.now(clock));
        repository.save(r);
        notify(r, "abs.rejected", r.getDecisionNote());
        return toDto(r);
    }

    /** Sets every lesson of the requested days to EXCUSED; returns how many lesson records it touched. */
    int excuse(AbsenceRequest r) {
        Student student = r.getStudent();
        List<LessonSlot> slots = lessonSlotRepository.findBySchoolClassId(student.getSchoolClass().getId());
        int count = 0;
        for (LocalDate d = r.getDateFrom(); !d.isAfter(r.getDateTo()); d = d.plusDays(1)) {
            String weekday = ParentStats.WEEKDAY_UZ.get(d.getDayOfWeek());
            for (LessonSlot slot : slots) {
                if (!slot.getWeekday().equals(weekday)) continue;
                LocalDate day = d;
                Attendance a = attendanceRepository.findByLessonSlotIdAndStudentIdAndRecordDate(slot.getId(), student.getId(), d)
                        .orElseGet(() -> {
                            Attendance fresh = new Attendance();
                            fresh.setLessonSlot(slot);
                            fresh.setStudent(student);
                            fresh.setRecordDate(day);
                            return fresh;
                        });
                a.setStatus(AttendanceStatus.EXCUSED);
                if (a.getComment() == null) a.setComment("Sababli ariza #" + r.getId());
                attendanceRepository.save(a);
                count++;
            }
        }
        return count;
    }

    private void notify(AbsenceRequest r, String key, String reason) {
        notificationService.enqueueDirect(r.getSchool(), r.getStudent(), r.getChatId(), NotificationType.ABSENCE_DECISION,
                r.getId(), LocalDate.now(clock),
                lang -> BotI18n.get().t(lang, key, "name", MessageFormatter.escape(NotificationService.fullName(r.getStudent())),
                        "from", DATE.format(r.getDateFrom()), "to", DATE.format(r.getDateTo()),
                        "reason", MessageFormatter.escape(reason))
                        + MessageFormatter.footer(lang, r.getSchool().getName()),
                null);
    }

    private static void requirePending(AbsenceRequest r) {
        if (r.getStatus() != AbsenceStatus.PENDING) {
            throw new IllegalStateException("Bu ariza allaqachon ko'rib chiqilgan");
        }
    }

    public AbsenceRequestDto toDto(AbsenceRequest r) {
        AbsenceRequestDto dto = new AbsenceRequestDto();
        dto.setId(r.getId());
        dto.setStudentId(r.getStudent().getId());
        dto.setStudentName(NotificationService.fullName(r.getStudent()));
        dto.setClassName(NotificationService.className(r.getStudent()));
        dto.setParentName(r.getParentName());
        dto.setDateFrom(r.getDateFrom());
        dto.setDateTo(r.getDateTo());
        dto.setReason(r.getReason().name());
        dto.setComment(r.getComment());
        dto.setHasPhoto(r.getPhotoFileId() != null);
        dto.setStatus(r.getStatus().name());
        dto.setDecisionNote(r.getDecisionNote());
        dto.setDecidedBy(r.getDecidedBy());
        dto.setCreatedAt(r.getCreatedAt());
        dto.setDecidedAt(r.getDecidedAt());
        dto.setExcusedLessons(r.getExcusedLessons());
        return dto;
    }
}
