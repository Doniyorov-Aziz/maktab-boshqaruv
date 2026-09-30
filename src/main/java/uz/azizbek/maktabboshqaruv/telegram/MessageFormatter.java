package uz.azizbek.maktabboshqaruv.telegram;

import uz.azizbek.maktabboshqaruv.bot.BotI18n;
import uz.azizbek.maktabboshqaruv.entity.GradeType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Builds the automatic parent notifications (Telegram HTML parse_mode) from
 * the bot language files. Every value that came from the database goes
 * through {@link #escape} — a student or subject name containing "<" or "&"
 * must never break the markup or inject tags. The overloads without a
 * language produce Uzbek (Latin).
 */
public final class MessageFormatter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final int ANNOUNCEMENT_PREVIEW = 300;
    private static final BotI18n I18N = BotI18n.get();

    private MessageFormatter() {
    }

    public static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public static String attendanceAbsent(String studentName, String className, Integer lessonNumber,
                                          String subject, LocalTime start, LocalDate date, LocalDate today,
                                          String schoolName) {
        return attendanceAbsent("uz", studentName, className, lessonNumber, subject, start, date, today, schoolName);
    }

    public static String attendanceAbsent(String lang, String studentName, String className, Integer lessonNumber,
                                          String subject, LocalTime start, LocalDate date, LocalDate today,
                                          String schoolName) {
        return I18N.t(lang, "notif.absent", "name", escape(studentName), "class", escape(className),
                "day", dayPhrase(lang, date, today), "lesson", lessonPhrase(lang, lessonNumber, subject, start))
                + footer(lang, schoolName);
    }

    public static String attendanceLate(String studentName, String className, Integer lessonNumber,
                                        String subject, LocalTime start, LocalDate date, LocalDate today,
                                        String schoolName) {
        return attendanceLate("uz", studentName, className, lessonNumber, subject, start, date, today, schoolName);
    }

    public static String attendanceLate(String lang, String studentName, String className, Integer lessonNumber,
                                        String subject, LocalTime start, LocalDate date, LocalDate today,
                                        String schoolName) {
        return I18N.t(lang, "notif.late", "name", escape(studentName), "class", escape(className),
                "day", dayPhrase(lang, date, today), "lesson", lessonPhrase(lang, lessonNumber, subject, start))
                + footer(lang, schoolName);
    }

    public static String gradeNew(String studentName, String subject, int score, GradeType type,
                                  LocalDate date, String schoolName) {
        return gradeNew("uz", studentName, subject, score, type, date, schoolName);
    }

    public static String gradeNew(String lang, String studentName, String subject, int score, GradeType type,
                                  LocalDate date, String schoolName) {
        return I18N.t(lang, "notif.grade_new", "name", escape(studentName), "subject", escape(subject),
                "score", score, "type", gradeTypeLabel(lang, type), "date", DATE.format(date))
                + footer(lang, schoolName);
    }

    public static String gradeUpdated(String studentName, String subject, int score, GradeType type,
                                      LocalDate date, String schoolName) {
        return gradeUpdated("uz", studentName, subject, score, type, date, schoolName);
    }

    public static String gradeUpdated(String lang, String studentName, String subject, int score, GradeType type,
                                      LocalDate date, String schoolName) {
        return I18N.t(lang, "notif.grade_updated", "name", escape(studentName), "subject", escape(subject),
                "score", score, "type", gradeTypeLabel(lang, type), "date", DATE.format(date))
                + footer(lang, schoolName);
    }

    /** The gentle "past baho" message — same facts, supportive tone, and a button to write to the teacher. */
    public static String gradeLow(String lang, String studentName, String subject, int score,
                                  LocalDate date, String schoolName) {
        return I18N.t(lang, "notif.grade_low", "name", escape(studentName), "subject", escape(subject),
                "score", score, "date", DATE.format(date)) + footer(lang, schoolName);
    }

    /** @param classLabel null for a school-wide announcement, e.g. "5-A" for a class one. */
    public static String announcement(String title, String content, String classLabel, String schoolName) {
        return announcement("uz", title, content, classLabel, schoolName);
    }

    public static String announcement(String lang, String title, String content, String classLabel, String schoolName) {
        String heading = classLabel == null ? I18N.t(lang, "notif.announcement.school")
                : I18N.t(lang, "notif.announcement.class", "class", escape(classLabel));
        return I18N.t(lang, "notif.announcement", "heading", heading, "title", escape(title),
                "content", escape(shorten(content))) + footer(lang, schoolName);
    }

    public static String gradeTypeLabel(GradeType type) {
        return gradeTypeLabel("uz", type);
    }

    public static String gradeTypeLabel(String lang, GradeType type) {
        return I18N.t(lang, "notif.grade_type." + (type == null ? "NONE" : type.name()));
    }

    public static String footer(String lang, String schoolName) {
        return I18N.t(lang, "notif.footer", "school", escape(schoolName));
    }

    static String shorten(String text) {
        if (text == null) return "";
        String t = text.strip();
        if (t.length() <= ANNOUNCEMENT_PREVIEW) return t;
        int cut = t.lastIndexOf(' ', ANNOUNCEMENT_PREVIEW);
        if (cut < ANNOUNCEMENT_PREVIEW / 2) cut = ANNOUNCEMENT_PREVIEW;
        return t.substring(0, cut).stripTrailing() + "…";
    }

    private static String dayPhrase(String lang, LocalDate date, LocalDate today) {
        if (date == null || date.equals(today)) return I18N.t(lang, "notif.day.today");
        if (date.equals(today.minusDays(1))) return I18N.t(lang, "notif.day.yesterday");
        return I18N.t(lang, "notif.day.date", "date", DATE.format(date));
    }

    private static String lessonPhrase(String lang, Integer lessonNumber, String subject, LocalTime start) {
        String inner = escape(subject) + (start != null ? ", " + TIME.format(start) : "");
        String number = lessonNumber != null ? I18N.t(lang, "notif.lesson_n", "n", lessonNumber)
                : I18N.t(lang, "notif.lesson");
        return number + " (" + inner + ")";
    }
}
