package uz.azizbek.maktabboshqaruv.service.appeal;

import uz.azizbek.maktabboshqaruv.bot.BotI18n;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.entity.AppealEnums.*;
import uz.azizbek.maktabboshqaruv.exception.ForbiddenException;
import uz.azizbek.maktabboshqaruv.repository.AppealMessageRepository;
import uz.azizbek.maktabboshqaruv.repository.AppealRepository;
import uz.azizbek.maktabboshqaruv.repository.ParentSessionRepository;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import uz.azizbek.maktabboshqaruv.service.NotificationService;
import uz.azizbek.maktabboshqaruv.service.SchoolAccessService;
import uz.azizbek.maktabboshqaruv.service.SchoolAccessService.Caller;
import uz.azizbek.maktabboshqaruv.telegram.MessageFormatter;
import uz.azizbek.maktabboshqaruv.telegram.TelegramClient;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineButton;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineKeyboardMarkup;
import uz.azizbek.maktabboshqaruv.telegram.TokenMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Parent appeals ("✉️ Ma'muriyatga xat").
 *
 * Bot side: a parent opens a draft, sends any mix of text, photos, videos, voice
 * notes, audio, documents, round videos and albums, then taps "✅ Yuborish" — the
 * messages become visible to the school as one appeal with a number. Limits: 10
 * appeals a day, 20 messages per sending, 20 MB per file, a whitelist of file types.
 * A parent who goes quiet for 30 minutes has the collected messages sent anyway.
 *
 * School side: a list with filters, the chat, replies (text + files, delivered through
 * the outbox), closing. A class-teacher appeal is visible to that class's teacher and
 * to admins only; an appeal to the administration — to admins only.
 */
@Service
public class AppealService {

    private static final Logger log = LoggerFactory.getLogger(AppealService.class);
    public static final int DAILY_LIMIT = 10;
    public static final int MESSAGES_PER_SENDING = 20;
    static final int IDLE_MINUTES = 30;
    private static final List<Long> NO_CLASSES = List.of(-1L);

    private final AppealRepository appeals;
    private final AppealMessageRepository messages;
    private final StudentRepository students;
    private final ParentSessionRepository sessions;
    private final AppealFiles files;
    private final FileStorage storage;
    private final AttachmentLinks links;
    private final SchoolAccessService access;
    private final NotificationService notifications;
    private final TelegramClient telegram;
    private final Clock clock;

    public AppealService(AppealRepository appeals, AppealMessageRepository messages, StudentRepository students,
                         ParentSessionRepository sessions, AppealFiles files, FileStorage storage, AttachmentLinks links,
                         SchoolAccessService access, NotificationService notifications, TelegramClient telegram,
                         Clock clock) {
        this.appeals = appeals;
        this.messages = messages;
        this.students = students;
        this.sessions = sessions;
        this.files = files;
        this.storage = storage;
        this.links = links;
        this.access = access;
        this.notifications = notifications;
        this.telegram = telegram;
        this.clock = clock;
    }

    /** A refusal the bot turns into a friendly message (i18n key + arguments). */
    public static class Refused extends RuntimeException {
        public final String key;
        public final Object[] args;

        public Refused(String key, Object... args) {
            super(key);
            this.key = key;
            this.args = args;
        }
    }

    // ================================================================= bot side

    /** One incoming Telegram message reduced to what we keep. */
    public record Incoming(Kind kind, String text, String fileId, String fileUniqueId, String mimeType, Long size,
                           Integer duration, String fileName, String mediaGroupId) {

        public static Incoming of(TelegramModels.Message m) {
            String caption = m.caption() != null ? m.caption().strip() : null;
            if (m.photo() != null && !m.photo().isEmpty()) {
                TelegramModels.PhotoSize p = m.largestPhoto();
                return new Incoming(Kind.PHOTO, caption, p.fileId(), p.fileUniqueId(), "image/jpeg", p.fileSize(), null, null, m.mediaGroupId());
            }
            if (m.video() != null) {
                var v = m.video();
                return new Incoming(Kind.VIDEO, caption, v.fileId(), v.fileUniqueId(), v.mimeType(), v.fileSize(), v.duration(), v.fileName(), m.mediaGroupId());
            }
            if (m.voice() != null) {
                var v = m.voice();
                return new Incoming(Kind.VOICE, caption, v.fileId(), v.fileUniqueId(), v.mimeType() == null ? "audio/ogg" : v.mimeType(), v.fileSize(), v.duration(), null, null);
            }
            if (m.audio() != null) {
                var a = m.audio();
                return new Incoming(Kind.AUDIO, caption, a.fileId(), a.fileUniqueId(), a.mimeType(), a.fileSize(), a.duration(), a.fileName(), m.mediaGroupId());
            }
            if (m.videoNote() != null) {
                var v = m.videoNote();
                return new Incoming(Kind.VIDEO_NOTE, null, v.fileId(), v.fileUniqueId(), "video/mp4", v.fileSize(), v.duration(), null, null);
            }
            if (m.document() != null) {
                var d = m.document();
                return new Incoming(Kind.DOCUMENT, caption, d.fileId(), d.fileUniqueId(), d.mimeType(), d.fileSize(), null, d.fileName(), m.mediaGroupId());
            }
            String text = m.text() != null ? m.text().strip() : null;
            return text == null || text.isEmpty() ? null : new Incoming(Kind.TEXT, text, null, null, null, null, null, null, null);
        }
    }

    /** "✉️ Ma'muriyatga xat" → a fresh draft (the daily limit is checked here). */
    @Transactional
    public Appeal startDraft(long chatId, Student student, Target target, TelegramModels.User from) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (appeals.countSentSince(chatId, LocalDate.now(clock).atStartOfDay()) >= DAILY_LIMIT) {
            throw new Refused("appeal.limit_day", "limit", DAILY_LIMIT);
        }
        appeals.findFirstByChatIdAndStatusOrderByIdDesc(chatId, Status.DRAFT).ifPresent(this::discardDraft);
        Appeal a = new Appeal();
        a.setSchool(NotificationService.schoolOf(student));
        a.setStudent(student);
        a.setChatId(chatId);
        a.setParentName(from == null ? null : from.firstName());
        a.setParentUsername(from == null ? null : from.username());
        a.setTarget(target);
        a.setStatus(Status.DRAFT);
        a.setSource(Source.BOT);
        a.setCreatedAt(now);
        a.setLastMessageAt(now);
        return appeals.save(a);
    }

    /** "↩️ Javob yozish" under a reply: continue the same appeal (only the parent's own). */
    @Transactional(readOnly = true)
    public Appeal ownAppeal(long chatId, Long appealId) {
        Appeal a = appeals.findById(appealId).orElseThrow(() -> new Refused("appeal.not_found"));
        if (a.getChatId() == null || a.getChatId() != chatId) throw new Refused("appeal.not_found");
        return a;
    }

    /** One message while composing: kept (not yet visible to the school), its file fetched in the background. */
    @Transactional
    public int accept(long chatId, Long appealId, Incoming in) {
        Appeal a = ownAppeal(chatId, appealId);
        int already = messages.pendingOf(a.getId()).size();
        if (already >= MESSAGES_PER_SENDING) throw new Refused("appeal.limit_messages", "limit", MESSAGES_PER_SENDING);
        if (in.kind() != Kind.TEXT) {
            String reason = AttachmentPolicy.reject(in.kind(), in.fileName(), in.mimeType(), in.size());
            if (reason != null) throw new Refused(reason, "mb", AttachmentPolicy.MAX_BYTES / 1024 / 1024);
        }
        AppealMessage m = new AppealMessage();
        m.setAppeal(a);
        m.setDirection(Direction.IN);
        m.setKind(in.kind());
        m.setText(in.text());
        m.setMediaGroupId(in.mediaGroupId());
        m.setFileId(in.fileId());
        m.setFileUniqueId(in.fileUniqueId());
        m.setMimeType(in.mimeType());
        m.setFileSize(in.size());
        m.setDuration(in.duration());
        m.setOriginalName(in.fileName());
        m.setFileState(in.kind() == Kind.TEXT ? FileState.NONE : FileState.PENDING);
        m.setPending(true);
        m.setCreatedAt(LocalDateTime.now(clock));
        AppealMessage saved = messages.save(m);
        if (a.getStatus() == Status.DRAFT) {
            a.setLastMessageAt(saved.getCreatedAt());
            appeals.save(a);
        }
        if (saved.hasFile()) afterCommit(() -> files.download(saved.getId()));
        return already + 1;
    }

    /** "✅ Yuborish": the collected messages go to the school; a closed appeal opens again. */
    @Transactional
    public Appeal submit(long chatId, Long appealId) {
        Appeal a = ownAppeal(chatId, appealId);
        List<AppealMessage> pending = messages.pendingOf(a.getId());
        if (pending.isEmpty()) throw new Refused("appeal.empty");
        LocalDateTime now = LocalDateTime.now(clock);
        for (AppealMessage m : pending) m.setPending(false);
        messages.saveAll(pending);
        if (a.getStatus() == Status.DRAFT) a.setCreatedAt(now);
        a.setStatus(Status.NEW);
        a.setUnreadCount(a.getUnreadCount() + pending.size());
        a.setLastMessageAt(now);
        a.setLastPreview(preview(pending.get(pending.size() - 1)));
        return appeals.save(a);
    }

    /** "❌ Bekor qilish": what was collected is dropped. */
    @Transactional
    public void cancel(long chatId, Long appealId) {
        Appeal a = ownAppeal(chatId, appealId);
        for (AppealMessage m : messages.pendingOf(a.getId())) deleteMessage(m);
        if (a.getStatus() == Status.DRAFT) appeals.delete(a);
    }

    private void discardDraft(Appeal draft) {
        for (AppealMessage m : messages.findByAppealIdOrderByCreatedAtAscIdAsc(draft.getId())) deleteMessage(m);
        appeals.delete(draft);
    }

    private void deleteMessage(AppealMessage m) {
        if (m.getStoragePath() != null) {
            try {
                java.nio.file.Files.deleteIfExists(storage.resolve(m.getStoragePath()));
            } catch (IOException ignored) {
                // a leftover file in storage is harmless
            }
        }
        messages.delete(m);
    }

    /** Recent appeals of this parent about this child, for the bot page. */
    @Transactional(readOnly = true)
    public List<Appeal> recent(long chatId, Long studentId, int limit) {
        return appeals.recentOfParent(chatId, studentId, PageRequest.of(0, limit));
    }

    /**
     * A parent who stopped mid-way for 30 minutes: what they sent is not lost — it is
     * sent to the school and the parent is told the number.
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void sendIdleDrafts() {
        for (Long id : messages.idleComposing(LocalDateTime.now(clock).minusMinutes(IDLE_MINUTES))) {
            try {
                Appeal a = appeals.findById(id).orElse(null);
                if (a == null || a.getChatId() == null) continue;
                Appeal sent = submit(a.getChatId(), id);
                sessions.findByChatId(a.getChatId()).ifPresent(s -> {
                    if (s.getPendingAction() != null && s.getPendingAction().startsWith("APPEAL")) {
                        s.setPendingAction(null);
                        s.setPendingData(null);
                        sessions.save(s);
                    }
                    telegram.sendMessage(a.getChatId(), BotI18n.get().t(s.lang(), "appeal.auto_sent", "id", sent.getId()), null);
                });
            } catch (Exception e) {
                log.warn("Murojaat #{} avtomatik yuborilmadi: {}", id, TokenMasker.mask(e.getMessage()));
            }
        }
    }

    // ============================================================== school side

    /** Who may see an appeal: admins of its school; for a class-teacher appeal also that class's teacher. */
    void requireVisible(Caller c, Appeal a) {
        access.requireSchool(c, a.getSchool().getId());
        if (c.isAdmin()) return;
        if (a.getTarget() == Target.CLASS_TEACHER
                && access.classesLed(c).contains(a.getStudent().getSchoolClass().getId())) return;
        throw new ForbiddenException("Bu murojaat sizga ochiq emas");
    }

    @Transactional(readOnly = true)
    public Page<AppealViews.Row> list(Caller c, Long schoolId, Status status, Target target, Long classId,
                                      LocalDate from, LocalDate to, String q, Pageable pageable) {
        access.requireSchool(c, schoolId);
        List<Long> led = c.isAdmin() ? NO_CLASSES : access.classesLed(c);
        String like = q == null || q.isBlank() ? "" : "%" + q.strip().toLowerCase(Locale.ROOT).replace("#", "") + "%";
        String idText = q == null ? "" : q.strip().replace("#", "");
        return appeals.search(schoolId, status, target, classId,
                        (from == null ? LocalDate.of(2000, 1, 1) : from).atStartOfDay(),
                        (to == null ? LocalDate.of(9999, 1, 1) : to.plusDays(1)).atStartOfDay(),
                        c.isAdmin(), led.isEmpty() ? NO_CLASSES : led, like, idText, pageable)
                .map(this::row);
    }

    @Transactional(readOnly = true)
    public long unread(Caller c, Long schoolId) {
        access.requireSchool(c, schoolId);
        List<Long> led = c.isAdmin() ? NO_CLASSES : access.classesLed(c);
        if (!c.isAdmin() && led.isEmpty()) return 0;
        return appeals.unreadFor(schoolId, c.isAdmin(), led.isEmpty() ? NO_CLASSES : led);
    }

    /** Opens the chat: marks it seen, gives every file a short-lived signed link. */
    @Transactional
    public AppealViews.Chat chat(Caller c, Long id) {
        Appeal a = appeals.findWithStudent(id).orElseThrow(() -> new IllegalStateException("Bunday murojaat topilmadi"));
        requireVisible(c, a);
        if (a.getUnreadCount() > 0 || a.getStatus() == Status.NEW) {
            a.setUnreadCount(0);
            if (a.getStatus() == Status.NEW) a.setStatus(Status.SEEN);
            appeals.save(a);
        }
        List<AppealViews.Message> list = messages.visibleOf(id).stream().map(m -> message(c, a, m)).toList();
        Student s = a.getStudent();
        return new AppealViews.Chat(row(a), s.getGuardianName(), s.getGuardianPhone(),
                a.getChatId() != null, list);
    }

    /** A reply (text and/or files) — saved, then sent to the parent through the outbox. */
    @Transactional
    public AppealViews.Chat reply(Caller c, Long id, String text, List<MultipartFile> uploads) {
        Appeal a = appeals.findWithStudent(id).orElseThrow(() -> new IllegalStateException("Bunday murojaat topilmadi"));
        requireVisible(c, a);
        String body = text == null ? "" : text.strip();
        List<MultipartFile> fileList = uploads == null ? List.of() : uploads.stream().filter(f -> !f.isEmpty()).toList();
        if (body.isEmpty() && fileList.isEmpty()) throw new IllegalStateException("Javob matni yoki fayl kerak");
        if (fileList.size() > 10) throw new IllegalStateException("Bir javobda ko'pi bilan 10 ta fayl");
        LocalDateTime now = LocalDateTime.now(clock);

        List<AppealMessage> saved = new ArrayList<>();
        String group = fileList.size() > 1 ? "out-" + id + "-" + now.toLocalTime().toNanoOfDay() : null;
        for (MultipartFile f : fileList) {
            Kind kind = kindOf(f.getContentType(), f.getOriginalFilename());
            String reason = AttachmentPolicy.reject(kind, f.getOriginalFilename(), f.getContentType(), f.getSize());
            if (reason != null) {
                throw new IllegalStateException("appeal.file_too_big".equals(reason)
                        ? "Fayl juda katta (ko'pi bilan 20 MB): " + f.getOriginalFilename()
                        : "Bu turdagi fayl yuborilmaydi: " + f.getOriginalFilename());
            }
            byte[] bytes;
            try {
                bytes = f.getBytes();
            } catch (IOException e) {
                throw new IllegalStateException("Faylni o'qib bo'lmadi");
            }
            AppealMessage m = new AppealMessage();
            m.setAppeal(a);
            m.setDirection(Direction.OUT);
            m.setKind(kind);
            m.setMediaGroupId(group);
            m.setMimeType(AttachmentPolicy.contentType(AttachmentPolicy.extensionOf(kind, f.getOriginalFilename(), f.getContentType())));
            m.setFileSize(f.getSize());
            m.setOriginalName(f.getOriginalFilename());
            m.setStoragePath(storage.save(a.getSchool().getId(),
                    AttachmentPolicy.extensionOf(kind, f.getOriginalFilename(), f.getContentType()), bytes));
            m.setFileState(FileState.STORED);
            m.setSentBy(c.username());
            m.setCreatedAt(now);
            saved.add(messages.save(m));
        }
        AppealMessage textMessage = null;
        if (!body.isEmpty()) {
            textMessage = new AppealMessage();
            textMessage.setAppeal(a);
            textMessage.setDirection(Direction.OUT);
            textMessage.setKind(Kind.TEXT);
            textMessage.setText(body);
            textMessage.setSentBy(c.username());
            textMessage.setCreatedAt(now);
            textMessage = messages.save(textMessage);
        }
        a.setStatus(Status.ANSWERED);
        a.setUnreadCount(0);
        a.setLastMessageAt(now);
        a.setLastPreview("↩️ " + (textMessage != null ? preview(textMessage) : preview(saved.get(saved.size() - 1))));
        appeals.save(a);

        if (a.getChatId() != null) {
            Long ref = textMessage != null ? textMessage.getId() : saved.get(0).getId();
            String media = saved.isEmpty() ? null
                    : "appeal:" + saved.stream().map(m -> String.valueOf(m.getId())).collect(Collectors.joining(","));
            String replyText = body;
            Long appealId = a.getId();
            notifications.enqueueDirect(a.getSchool(), a.getStudent(), a.getChatId(), NotificationType.MESSAGE_REPLY,
                    ref, LocalDate.now(clock),
                    lang -> BotI18n.get().t(lang, "appeal.reply_notification", "id", appealId,
                            "text", replyText.isEmpty() ? BotI18n.get().t(lang, "appeal.reply_files") : MessageFormatter.escape(replyText))
                            + MessageFormatter.footer(lang, a.getSchool().getName()),
                    lang -> InlineKeyboardMarkup.builder()
                            .row(InlineButton.callback(BotI18n.get().t(lang, "appeal.btn.reply"), "msg:a:re:ap:" + appealId + ":n:1"))
                            .build(),
                    media);
        }
        return chat(c, id);
    }

    @Transactional
    public AppealViews.Row close(Caller c, Long id) {
        Appeal a = appeals.findWithStudent(id).orElseThrow(() -> new IllegalStateException("Bunday murojaat topilmadi"));
        requireVisible(c, a);
        a.setStatus(Status.CLOSED);
        a.setUnreadCount(0);
        return row(appeals.save(a));
    }

    /**
     * A phone call or a visit written down by staff, with the date and time it really
     * happened (not necessarily "now").
     */
    @Transactional
    public AppealViews.Row createManual(Caller c, Long studentId, Target target, String text, LocalDateTime at,
                                        String parentName) {
        Student s = students.findById(studentId).orElseThrow(() -> new IllegalStateException("Bunday o'quvchi topilmadi"));
        School school = NotificationService.schoolOf(s);
        access.requireSchool(c, school.getId());
        if (text == null || text.isBlank()) throw new IllegalStateException("Murojaat matni kerak");
        LocalDateTime when = at != null ? at : LocalDateTime.now(clock);
        if (when.isAfter(LocalDateTime.now(clock).plusMinutes(5))) throw new IllegalStateException("Kelajak vaqt tanlanmaydi");
        Appeal a = new Appeal();
        a.setSchool(school);
        a.setStudent(s);
        a.setParentName(parentName != null && !parentName.isBlank() ? parentName.strip() : s.getGuardianName());
        a.setTarget(target == null ? Target.ADMINISTRATION : target);
        a.setStatus(Status.SEEN);
        a.setSource(Source.MANUAL);
        a.setCreatedAt(when);
        a.setLastMessageAt(when);
        a.setCreatedBy(c.username());
        AppealMessage m = new AppealMessage();
        m.setDirection(Direction.IN);
        m.setKind(Kind.TEXT);
        m.setText(text.strip());
        m.setCreatedAt(when);
        a.setLastPreview(preview(m));
        a = appeals.save(a);
        m.setAppeal(a);
        messages.save(m);
        requireVisible(c, a);
        return row(a);
    }

    /** The file of one message — after the same visibility check as the chat. */
    @Transactional(readOnly = true)
    public AppealMessage attachment(Caller c, Long appealId, Long messageId) {
        Appeal a = appeals.findWithStudent(appealId).orElseThrow(() -> new IllegalStateException("Bunday murojaat topilmadi"));
        requireVisible(c, a);
        AppealMessage m = messages.findById(messageId)
                .filter(x -> x.getAppeal().getId().equals(appealId) && x.hasFile() && !x.isPending())
                .orElseThrow(() -> new IllegalStateException("Fayl topilmadi"));
        if (m.getFileState() != FileState.STORED || m.getStoragePath() == null) {
            throw new IllegalStateException("Fayl hali yuklanmoqda");
        }
        return m;
    }

    // ================================================================= helpers

    private AppealViews.Row row(Appeal a) {
        Student s = a.getStudent();
        SchoolClass sc = s.getSchoolClass();
        return new AppealViews.Row(a.getId(), a.getParentName(), a.getParentUsername(), s.getId(),
                s.getFirstName() + " " + s.getLastName(), sc.getId(), sc.getGradeNumber() + "-" + sc.getSectionLetter(),
                a.getTarget().name(), a.getStatus().name(), a.getSource().name(), a.getLastPreview(),
                a.getLastMessageAt(), a.getCreatedAt(), a.getUnreadCount());
    }

    private AppealViews.Message message(Caller c, Appeal a, AppealMessage m) {
        String url = m.hasFile() && m.getFileState() == FileState.STORED
                ? "/api/appeals/" + a.getId() + "/attachments/" + m.getId() + "?t=" + links.token(a.getId(), m.getId(), c.username())
                : null;
        return new AppealViews.Message(m.getId(), m.getDirection().name(), m.getKind().name(), m.getText(),
                m.getMediaGroupId(), m.getOriginalName(), m.getMimeType(), m.getFileSize(), m.getDuration(),
                m.getFileState().name(), url, m.getSentBy(), m.getCreatedAt());
    }

    static String preview(AppealMessage m) {
        String icon = switch (m.getKind()) {
            case PHOTO -> "🖼 Rasm";
            case VIDEO -> "🎬 Video";
            case VOICE -> "🎤 Ovozli xabar";
            case AUDIO -> "🎵 Audio";
            case DOCUMENT -> "📎 " + (m.getOriginalName() != null ? m.getOriginalName() : "Fayl");
            case VIDEO_NOTE -> "⭕ Video xabar";
            case TEXT -> null;
        };
        String text = m.getText() == null ? "" : m.getText().replaceAll("\\s+", " ").strip();
        String line = icon == null ? text : text.isEmpty() ? icon : icon + " · " + text;
        return line.length() > 190 ? line.substring(0, 189) + "…" : line;
    }

    static Kind kindOf(String mime, String name) {
        String m = mime == null ? "" : mime.toLowerCase(Locale.ROOT);
        String n = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (m.startsWith("image/") && !n.endsWith(".svg")) return Kind.PHOTO;
        if (m.startsWith("video/")) return Kind.VIDEO;
        if (m.startsWith("audio/")) return Kind.AUDIO;
        return Kind.DOCUMENT;
    }

    private static void afterCommit(Runnable r) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    r.run();
                }
            });
        } else {
            r.run();
        }
    }
}
