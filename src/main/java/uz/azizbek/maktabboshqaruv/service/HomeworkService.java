package uz.azizbek.maktabboshqaruv.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.bot.BotI18n;
import uz.azizbek.maktabboshqaruv.bot.SubjectIcons;
import uz.azizbek.maktabboshqaruv.dto.HomeworkDtos.Request;
import uz.azizbek.maktabboshqaruv.dto.HomeworkDtos.Response;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.exception.ForbiddenException;
import uz.azizbek.maktabboshqaruv.repository.*;
import uz.azizbek.maktabboshqaruv.telegram.MessageFormatter;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineButton;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineKeyboardMarkup;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/** Uy vazifalari: the staff list and form, the attached file, and the message to the class's parents. */
@Service
public class HomeworkService {

    private final HomeworkRepository homework;
    private final SchoolClassRepository classes;
    private final SubjectRepository subjects;
    private final EmployeeRepository employees;
    private final ParentTelegramLinkRepository links;
    private final NotificationService notifications;
    private final SchoolAccessService access;
    private final ModuleFiles files;
    private final Clock clock;

    public HomeworkService(HomeworkRepository homework, SchoolClassRepository classes, SubjectRepository subjects,
                           EmployeeRepository employees, ParentTelegramLinkRepository links,
                           NotificationService notifications, SchoolAccessService access, ModuleFiles files, Clock clock) {
        this.homework = homework;
        this.classes = classes;
        this.subjects = subjects;
        this.employees = employees;
        this.links = links;
        this.notifications = notifications;
        this.access = access;
        this.files = files;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<Response> list(Long schoolId, Long classId, Long subjectId, Long employeeId, LocalDate from, LocalDate to,
                               Pageable pageable) {
        access.requireSchool(schoolId);
        return homework.search(schoolId, classId, subjectId, employeeId, from, to, pageable).map(HomeworkService::toDto);
    }

    @Transactional(readOnly = true)
    public Response get(Long id) {
        return toDto(find(id));
    }

    @Transactional
    public Response create(Request r) {
        SchoolAccessService.Caller caller = access.caller();
        Homework h = new Homework();
        apply(h, r, caller);
        h.setCreatedBy(caller.username());
        h.setCreatedAt(LocalDateTime.now(clock));
        homework.save(h);
        if (Boolean.TRUE.equals(r.notifyParents())) notifyParents(h);
        return toDto(h);
    }

    @Transactional
    public Response update(Long id, Request r) {
        Homework h = find(id);
        apply(h, r, access.caller());
        return toDto(h);
    }

    @Transactional
    public void delete(Long id) {
        Homework h = find(id);
        requireOwnerOrAdmin(h);
        String path = h.getFilePath();
        homework.delete(h);
        files.delete(path);
    }

    @Transactional
    public Response attach(Long id, MultipartFile file) {
        Homework h = find(id);
        ModuleFiles.Stored stored = files.save("homework", h.getSchool().getId(), file);
        String old = h.getFilePath();
        h.setFilePath(stored.path());
        h.setFileName(stored.name());
        h.setFileSize(stored.size());
        h.setFileTelegramId(null);
        files.delete(old);
        return toDto(h);
    }

    @Transactional
    public Response detach(Long id) {
        Homework h = find(id);
        files.delete(h.getFilePath());
        h.setFilePath(null);
        h.setFileName(null);
        h.setFileSize(null);
        h.setFileTelegramId(null);
        return toDto(h);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> download(Long id) {
        Homework h = find(id);
        return files.download(h.getFilePath(), h.getFileName());
    }

    /** The "Ota-onalarga yuborish" button (after the file is attached): the message once more is refused. */
    @Transactional
    public int notify(Long id) {
        Homework h = find(id);
        if (h.getNotifiedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu vazifa ota-onalarga allaqachon yuborilgan");
        }
        return notifyParents(h);
    }

    /** "📝 Uy vazifasi" to every parent of the class (one message per chat), with the file when there is one. */
    @Transactional
    public int notifyParents(Homework h) {
        School school = h.getSchool();
        String subject = h.getSubject().getName();
        String className = className(h.getSchoolClass());
        String title = h.getTitle();
        String description = h.getDescription();
        LocalDate due = h.getDueDate();
        boolean hasFile = h.getFilePath() != null;
        int queued = notifications.createRows(school, NotificationType.HOMEWORK, h.getId(), h.getAssignedDate(),
                NotificationService.oncePerChat(links.findActiveByClassId(h.getSchoolClass().getId())),
                lang -> {
                    BotI18n i18n = BotI18n.get();
                    return i18n.t(lang, "notif.homework", "emoji", SubjectIcons.subject(subject),
                            "subject", MessageFormatter.escape(subject), "class", MessageFormatter.escape(className),
                            "title", MessageFormatter.escape(title),
                            "description", description == null || description.isBlank() ? ""
                                    : "\n<blockquote>" + MessageFormatter.escape(description) + "</blockquote>",
                            "due", i18n.dateFull(lang, due),
                            "file", hasFile ? "\n" + i18n.t(lang, "notif.homework_file") : "")
                            + MessageFormatter.footer(lang, school.getName());
                },
                lang -> InlineKeyboardMarkup.builder()
                        .row(InlineButton.callback(BotI18n.get().t(lang, "notif.btn.homework"), "hw:n:1").styled("primary"))
                        .build(),
                hasFile ? HomeworkMedia.ref(h.getId()) : null);
        h.setNotifiedAt(LocalDateTime.now(clock));
        return queued;
    }

    // ------------------------------------------------------------------ helpers

    private void apply(Homework h, Request r, SchoolAccessService.Caller caller) {
        SchoolClass c = classes.findById(r.schoolClassId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday sinf topilmadi"));
        Subject s = subjects.findById(r.subjectId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday fan topilmadi"));
        School school = c.getAcademicYear().getSchool();
        if (s.getSchool() != null && !s.getSchool().getId().equals(school.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fan boshqa maktabniki");
        }
        Long employeeId = r.employeeId() != null ? r.employeeId() : (h.getId() == null ? caller.employeeId() : null);
        Employee e = null;
        if (employeeId != null) {
            e = employees.findById(employeeId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday xodim topilmadi"));
            if (e.getSchool() != null && !e.getSchool().getId().equals(school.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O'qituvchi boshqa maktabniki");
            }
        }
        LocalDate assigned = r.assignedDate() != null ? r.assignedDate()
                : h.getAssignedDate() != null ? h.getAssignedDate() : LocalDate.now(clock);
        if (r.dueDate().isBefore(assigned)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topshirish sanasi berilgan kundan oldin bo'lmasin");
        }
        h.setSchool(school);
        h.setSchoolClass(c);
        h.setSubject(s);
        h.setEmployee(e);
        h.setAssignedDate(assigned);
        h.setDueDate(r.dueDate());
        h.setTitle(r.title().strip());
        h.setDescription(r.description() == null || r.description().isBlank() ? null : r.description().strip());
    }

    private void requireOwnerOrAdmin(Homework h) {
        SchoolAccessService.Caller c = access.caller();
        if (c.isAdmin() || Objects.equals(c.username(), h.getCreatedBy())) return;
        throw new ForbiddenException("Faqat vazifani bergan o'qituvchi yoki administrator o'chira oladi");
    }

    private Homework find(Long id) {
        return homework.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday uy vazifasi topilmadi: " + id));
    }

    static String className(SchoolClass c) {
        return c.getGradeNumber() + "-" + c.getSectionLetter();
    }

    static String personName(Employee e) {
        return e == null ? null : (e.getLastName() + " " + e.getFirstName()).strip();
    }

    static Response toDto(Homework h) {
        return new Response(h.getId(), h.getSchoolClass().getId(), className(h.getSchoolClass()), h.getSubject().getId(),
                h.getSubject().getName(), h.getSubject().getColor(),
                h.getEmployee() == null ? null : h.getEmployee().getId(), personName(h.getEmployee()),
                h.getAssignedDate(), h.getDueDate(), h.getTitle(), h.getDescription(), h.getFileName(), h.getFileSize(),
                h.getNotifiedAt(), h.getCreatedBy(), h.getCreatedAt());
    }
}
