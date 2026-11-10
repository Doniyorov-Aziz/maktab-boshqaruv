package uz.azizbek.maktabboshqaruv.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.entity.NotificationType;
import uz.azizbek.maktabboshqaruv.entity.SchoolClass;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.bot.BotI18n;
import uz.azizbek.maktabboshqaruv.repository.SchoolClassRepository;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import uz.azizbek.maktabboshqaruv.telegram.MessageFormatter;
import uz.azizbek.maktabboshqaruv.telegram.TelegramClient.MediaKind;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * The tabel in the parents' bot: "Ota-onalarga yuborish" queues one message per parent with the pupil's
 * PDF (outbox media "reportcard:&lt;studentId&gt;:&lt;termId&gt;", drawn when it is sent, so it is always current).
 */
@Component
public class ReportCardMedia implements OutboxMedia.Resolver {

    static final String PREFIX = "reportcard:";

    private final ReportCardService cards;
    private final ReportCardPdf pdf;
    private final StudentRepository students;
    private final SchoolClassRepository classes;
    private final NotificationService notifications;
    private final Clock clock;

    public ReportCardMedia(ReportCardService cards, ReportCardPdf pdf, StudentRepository students,
                           SchoolClassRepository classes, NotificationService notifications, Clock clock) {
        this.cards = cards;
        this.pdf = pdf;
        this.students = students;
        this.classes = classes;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Override
    public boolean supports(String media) {
        return media.startsWith(PREFIX);
    }

    @Override
    public List<OutboxMedia.File> files(String media) {
        String[] parts = media.substring(PREFIX.length()).split(":");
        Long studentId = Long.valueOf(parts[0]);
        Long termId = parts.length > 1 && !parts[1].isBlank() ? Long.valueOf(parts[1]) : null;
        return List.of(new OutboxMedia.File(MediaKind.DOCUMENT, null,
                () -> pdf.render(List.of(cards.studentYear(studentId, termId))), "tabel.pdf", id -> {
        }));
    }

    /** Queues the tabel for every linked parent of the class; returns how many messages were queued. */
    @Transactional
    public int send(Long classId, Long termId) {
        SchoolClass c = classes.findById(classId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday sinf topilmadi"));
        ReportCardService.Term term = cards.terms(classId).stream().filter(t -> t.id().equals(termId)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chorak tanlanmagan"));
        LocalDate today = LocalDate.now(clock);
        String className = ReportCardService.className(c);
        int queued = 0;
        for (Student s : students.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(classId)) {
            List<NotificationService.Recipient> to = notifications.recipientsOf(s);
            if (to.isEmpty()) continue;
            String name = ReportCardService.name(s);
            // one per parent and pupil a day (a second press the same day sends nothing new)
            for (NotificationService.Recipient r : to) {
                queued += notifications.enqueueDirect(c.getAcademicYear().getSchool(), s, r.chatId(), NotificationType.REPORT_CARD,
                        s.getId(), today,
                        lang -> BotI18n.get().t(lang, "notif.report_card", "name", MessageFormatter.escape(name),
                                "class", MessageFormatter.escape(className), "term", MessageFormatter.escape(term.name()))
                                + MessageFormatter.footer(lang, c.getAcademicYear().getSchool().getName()),
                        null, PREFIX + s.getId() + ":" + termId);
            }
        }
        return queued;
    }
}
