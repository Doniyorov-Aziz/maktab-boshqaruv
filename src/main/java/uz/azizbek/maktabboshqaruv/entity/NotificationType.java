package uz.azizbek.maktabboshqaruv.entity;

public enum NotificationType {
    ATTENDANCE_ABSENT, ATTENDANCE_LATE, GRADE_NEW, GRADE_UPDATED, ANNOUNCEMENT,
    GRADE_LOW, TOMORROW_SCHEDULE, WEEKLY_REPORT, EVENT_REMINDER, MESSAGE_REPLY, ABSENCE_DECISION, BROADCAST, MORNING_DIGEST,
    /** a PDF the parent asked for in "📚 Kitob mutolaasi" */
    BOOK_DOWNLOAD,
    /** Friday's "📚 Dam olish kunlari uchun 3 ta kitob" */
    LIBRARY_WEEKLY;

    /** Asked for by the parent just now: delivered at once, never held back by quiet hours. */
    public boolean onDemand() {
        return this == BOOK_DOWNLOAD;
    }
}
