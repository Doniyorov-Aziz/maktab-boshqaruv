package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.bot.BotI18n;
import uz.azizbek.maktabboshqaruv.dto.ParentMessageDto;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.ParentMessageRepository;
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

/** "💬 Maktabga yozish": parent messages from the bot, and the school's replies delivered back through the outbox. */
@Service
public class ParentMessageService {

    @Autowired
    private ParentMessageRepository repository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private Clock clock;

    @Transactional
    public ParentMessage createFromParent(Student student, long chatId, TelegramModels.User from,
                                          ParentMessageRecipient recipient, String text, String photoFileId) {
        ParentMessage m = new ParentMessage();
        m.setSchool(NotificationService.schoolOf(student));
        m.setStudent(student);
        m.setChatId(chatId);
        m.setParentName(from == null ? null : from.firstName());
        m.setParentUsername(from == null ? null : from.username());
        m.setRecipient(recipient);
        m.setText(text == null || text.isBlank() ? "📎" : text.strip());
        m.setPhotoFileId(photoFileId);
        m.setStatus(ParentMessageStatus.NEW);
        m.setCreatedAt(LocalDateTime.now(clock));
        return repository.save(m);
    }

    public Page<ParentMessageDto> list(Long schoolId, ParentMessageStatus status, Pageable pageable) {
        Page<ParentMessage> page = status == null
                ? repository.findBySchoolIdOrderByCreatedAtDesc(schoolId, pageable)
                : repository.findBySchoolIdAndStatusOrderByCreatedAtDesc(schoolId, status, pageable);
        return page.map(this::toDto);
    }

    public long countNew(Long schoolId) {
        return repository.countBySchoolIdAndStatus(schoolId, ParentMessageStatus.NEW);
    }

    public ParentMessage get(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalStateException("Bunday murojaat topilmadi: " + id));
    }

    /** Saves the reply and queues it to the parent in their language, quoting the question. */
    @Transactional
    public ParentMessageDto reply(Long id, String replyText, String username) {
        ParentMessage m = get(id);
        if (replyText == null || replyText.isBlank()) {
            throw new IllegalStateException("Javob matni bo'sh bo'lmasligi kerak");
        }
        m.setReplyText(replyText.strip());
        m.setRepliedBy(username);
        m.setRepliedAt(LocalDateTime.now(clock));
        m.setStatus(ParentMessageStatus.ANSWERED);
        repository.save(m);

        String question = m.getText().length() > 200 ? m.getText().substring(0, 199) + "…" : m.getText();
        notificationService.enqueueDirect(m.getSchool(), m.getStudent(), m.getChatId(), NotificationType.MESSAGE_REPLY,
                m.getId(), LocalDate.now(clock),
                lang -> BotI18n.get().t(lang, "msg.reply_notification", "question", MessageFormatter.escape(question),
                        "answer", MessageFormatter.escape(m.getReplyText()))
                        + MessageFormatter.footer(lang, m.getSchool().getName()),
                null);
        return toDto(m);
    }

    public ParentMessageDto toDto(ParentMessage m) {
        ParentMessageDto dto = new ParentMessageDto();
        dto.setId(m.getId());
        dto.setStudentId(m.getStudent().getId());
        dto.setStudentName(NotificationService.fullName(m.getStudent()));
        dto.setClassName(NotificationService.className(m.getStudent()));
        dto.setParentName(m.getParentName());
        dto.setParentUsername(m.getParentUsername());
        dto.setRecipient(m.getRecipient().name());
        dto.setText(m.getText());
        dto.setHasPhoto(m.getPhotoFileId() != null);
        dto.setStatus(m.getStatus().name());
        dto.setReplyText(m.getReplyText());
        dto.setRepliedBy(m.getRepliedBy());
        dto.setCreatedAt(m.getCreatedAt());
        dto.setRepliedAt(m.getRepliedAt());
        return dto;
    }
}
