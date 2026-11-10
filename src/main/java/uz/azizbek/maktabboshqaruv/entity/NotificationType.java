package uz.azizbek.maktabboshqaruv.entity;

public enum NotificationType {
    ATTENDANCE_ABSENT, ATTENDANCE_LATE, GRADE_NEW, GRADE_UPDATED, ANNOUNCEMENT,
    GRADE_LOW, TOMORROW_SCHEDULE, WEEKLY_REPORT, EVENT_REMINDER, MESSAGE_REPLY, ABSENCE_DECISION, BROADCAST, MORNING_DIGEST,
    /** a PDF the parent asked for in "📚 Kitob mutolaasi" */
    BOOK_DOWNLOAD,
    /** Friday's "📚 Dam olish kunlari uchun 3 ta kitob" */
    LIBRARY_WEEKLY,
    /** "📝 Uy vazifasi" given to the child's class */
    HOMEWORK,
    /** "📝 Ertaga nazorat ishi" — the day before an exam */
    EXAM_REMINDER,
    /** a parent–teacher meeting booked, cancelled, or tomorrow */
    MEETING,
    /** "💳 To'lov eslatmasi" — a month not fully paid */
    PAYMENT_REMINDER,
    /** the quarter report card (tabel), as a PDF */
    REPORT_CARD,
    /** "🏆 Yangi yutuq" */
    ACHIEVEMENT,
    /** a file the parent asked for just now: a homework's attachment, the tabel PDF */
    MODULE_FILE;

    /** Asked for by the parent just now: delivered at once, never held back by quiet hours. */
    public boolean onDemand() {
        return this == BOOK_DOWNLOAD || this == MODULE_FILE;
    }
}
