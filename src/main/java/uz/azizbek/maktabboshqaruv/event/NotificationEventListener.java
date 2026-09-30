package uz.azizbek.maktabboshqaruv.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.azizbek.maktabboshqaruv.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges committed domain events to the outbox. AFTER_COMMIT means a save
 * that rolls back never notifies anyone. Failures are logged and swallowed:
 * the user's save has already committed, so an outbox hiccup must not turn
 * their successful request into an error response.
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    @Autowired
    private NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAttendance(AttendanceMarkedEvent event) {
        try {
            notificationService.enqueueAttendance(event);
        } catch (Exception e) {
            log.error("Davomat xabarnomasini navbatga qo'shib bo'lmadi (attendance={})", event.attendanceId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGrade(GradeSavedEvent event) {
        try {
            notificationService.enqueueGrade(event);
        } catch (Exception e) {
            log.error("Baho xabarnomasini navbatga qo'shib bo'lmadi (grade={})", event.gradeId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAnnouncement(AnnouncementCreatedEvent event) {
        try {
            notificationService.enqueueAnnouncement(event);
        } catch (Exception e) {
            log.error("E'lon xabarnomasini navbatga qo'shib bo'lmadi (announcement={})", event.announcementId(), e);
        }
    }
}
