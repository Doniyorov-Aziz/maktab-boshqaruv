package uz.azizbek.maktabboshqaruv.telegram;

import uz.azizbek.maktabboshqaruv.entity.GradeType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Builds the parent-facing texts (Telegram HTML parse_mode). Every value that
 * came from the database goes through {@link #escape} — a student or subject
 * name containing "<" or "&" must never break the markup or inject tags.
 */
public final class MessageFormatter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final int ANNOUNCEMENT_PREVIEW = 300;

    private MessageFormatter() {
    }

    public static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public static String attendanceAbsent(String studentName, String className, Integer lessonNumber,
                                          String subject, LocalTime start, LocalDate date, LocalDate today,
                                          String schoolName) {
        return "🔴 <b>" + escape(studentName) + "</b> (" + escape(className) + ") "
                + dayPhrase(date, today) + " " + lessonPhrase(lessonNumber, subject, start) + " kelmadi."
                + footer(schoolName);
    }

    public static String attendanceLate(String studentName, String className, Integer lessonNumber,
                                        String subject, LocalTime start, LocalDate date, LocalDate today,
                                        String schoolName) {
        return "🟡 <b>" + escape(studentName) + "</b> (" + escape(className) + ") "
                + dayPhrase(date, today) + " " + lessonPhrase(lessonNumber, subject, start) + " kechikib keldi."
                + footer(schoolName);
    }

    public static String gradeNew(String studentName, String subject, int score, GradeType type,
                                  LocalDate date, String schoolName) {
        return "📘 <b>" + escape(studentName) + "</b> " + escape(subject) + " fanidan <b>" + score
                + "</b> baho oldi (" + gradeTypeLabel(type) + ", " + DATE.format(date) + ")."
                + footer(schoolName);
    }

    public static String gradeUpdated(String studentName, String subject, int score, GradeType type,
                                      LocalDate date, String schoolName) {
        return "✏️ <b>" + escape(studentName) + "</b>ning " + escape(subject) + " fanidan bahosi <b>" + score
                + "</b> ga o'zgartirildi (" + gradeTypeLabel(type) + ", " + DATE.format(date) + ")."
                + footer(schoolName);
    }

    /** @param classLabel null for a school-wide announcement, e.g. "5-A" for a class one. */
    public static String announcement(String title, String content, String classLabel, String schoolName) {
        String heading = classLabel == null ? "Maktab e'loni" : escape(classLabel) + " sinf e'loni";
        return "📢 <b>" + heading + "</b>: " + escape(title) + "\n" + escape(shorten(content))
                + footer(schoolName);
    }

    public static String gradeTypeLabel(GradeType type) {
        if (type == null) return "baho";
        return switch (type) {
            case CURRENT -> "joriy baho";
            case EXAM -> "imtihon bahosi";
            case QUARTERLY -> "chorak bahosi";
        };
    }

    static String shorten(String text) {
        if (text == null) return "";
        String t = text.strip();
        if (t.length() <= ANNOUNCEMENT_PREVIEW) return t;
        int cut = t.lastIndexOf(' ', ANNOUNCEMENT_PREVIEW);
        if (cut < ANNOUNCEMENT_PREVIEW / 2) cut = ANNOUNCEMENT_PREVIEW;
        return t.substring(0, cut).stripTrailing() + "…";
    }

    private static String dayPhrase(LocalDate date, LocalDate today) {
        if (date == null || date.equals(today)) return "bugun";
        if (date.equals(today.minusDays(1))) return "kecha";
        return DATE.format(date) + " kuni";
    }

    private static String lessonPhrase(Integer lessonNumber, String subject, LocalTime start) {
        String inner = escape(subject) + (start != null ? ", " + TIME.format(start) : "");
        String number = lessonNumber != null ? lessonNumber + "-darsga" : "darsga";
        return number + " (" + inner + ")";
    }

    private static String footer(String schoolName) {
        return "\n\n🏫 <i>" + escape(schoolName) + "</i>";
    }
}
