package uz.azizbek.maktabboshqaruv.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uz.azizbek.maktabboshqaruv.bot.SubjectIcons;
import uz.azizbek.maktabboshqaruv.bot.BotI18n;
import uz.azizbek.maktabboshqaruv.entity.CalendarEvent;
import uz.azizbek.maktabboshqaruv.entity.NotificationType;
import uz.azizbek.maktabboshqaruv.entity.ParentTelegramLink;
import uz.azizbek.maktabboshqaruv.repository.ParentTelegramLinkRepository;
import uz.azizbek.maktabboshqaruv.service.attendance.DemoMode;
import uz.azizbek.maktabboshqaruv.telegram.MessageFormatter;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * "Ota-onalarga xabar berish": a new event goes to the parents of its classes (or of the whole
 * school) through the Telegram outbox, once per chat. In demo mode nothing is queued — only a log
 * line says who would have got it. The day-before reminder is BotScheduledJobs#runEventReminders.
 */
@Component
public class EventNotifier {

    private static final Logger log = LoggerFactory.getLogger(EventNotifier.class);

    private final ParentTelegramLinkRepository links;
    private final NotificationService notifications;
    private final DemoMode demo;
    private final Clock clock;

    public EventNotifier(ParentTelegramLinkRepository links, NotificationService notifications, DemoMode demo, Clock clock) {
        this.links = links;
        this.notifications = notifications;
        this.demo = demo;
        this.clock = clock;
    }

    /** The parents an event is for: its classes' parents, or every parent of the school. */
    public List<ParentTelegramLink> audience(CalendarEvent e) {
        List<Long> classIds = e.classIdList();
        return classIds.isEmpty() ? links.findActiveBySchoolId(e.getSchool().getId()) : links.findActiveByClassIds(classIds);
    }

    /** @return how many messages were queued (0 in demo mode or when the event does not ask for it) */
    @Transactional
    public int notifyCreated(CalendarEvent e) {
        if (!Boolean.TRUE.equals(e.getNotifyParents())) return 0;
        List<NotificationService.Recipient> recipients = NotificationService.oncePerChat(audience(e));
        if (demo.enabled()) {
            log.info("DEMO: «{}» tadbiri haqida {} ta ota-onaga Telegram xabari yuborilmadi (demo rejim, faqat log)",
                    e.getTitle(), recipients.size());
            return 0;
        }
        LocalDate today = LocalDate.now(clock);
        BotI18n i18n = BotI18n.get();
        int queued = 0;
        for (NotificationService.Recipient r : recipients) {
            queued += notifications.enqueueDirect(e.getSchool(), r.student(), r.chatId(), NotificationType.EVENT_REMINDER,
                    e.getId(), today, lang -> text(i18n, lang, e) + MessageFormatter.footer(lang, e.getSchool().getName()), null);
        }
        if (queued > 0) log.info("Yangi tadbir «{}»: {} ta xabar navbatga qo'shildi", e.getTitle(), queued);
        return queued;
    }

    static String text(BotI18n i18n, String lang, CalendarEvent e) {
        String time = e.getStartTime() == null ? ""
                : " · " + RoomBookingService.hm(e.getStartTime()) + (e.getEndTime() == null ? "" : "–" + RoomBookingService.hm(e.getEndTime()));
        String place = e.getRoom() != null ? "\n📍 " + MessageFormatter.escape(LessonSlotService.roomLabel(e.getRoom().getRoomNumber()))
                : e.getLocation() != null && !e.getLocation().isBlank() ? "\n📍 " + MessageFormatter.escape(e.getLocation()) : "";
        String description = e.getDescription() == null || e.getDescription().isBlank() ? ""
                : "\n<blockquote>" + MessageFormatter.escape(e.getDescription()) + "</blockquote>";
        return i18n.t(lang, "notif.event_new", "emoji", SubjectIcons.event(e.getType().name()),
                "title", MessageFormatter.escape(e.getTitle()), "date", i18n.dateFull(lang, e.getStartDate()),
                "time", time, "place", place, "description", description);
    }
}
