package uz.azizbek.maktabboshqaruv.telegram;

import uz.azizbek.maktabboshqaruv.entity.GradeType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class MessageFormatterTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 30);

    @Test
    void escape_htmlSpecialCharacters() {
        assertEquals("a &lt;b&gt; &amp; &quot;c&quot;", MessageFormatter.escape("a <b> & \"c\""));
        assertEquals("", MessageFormatter.escape(null));
    }

    @Test
    void attendanceAbsent_matchesSpecSample() {
        String text = MessageFormatter.attendanceAbsent("Alisher Karimov", "5-A", 2, "Matematika",
                LocalTime.of(9, 25), TODAY, TODAY, "1-maktab");
        assertEquals("🔴 <b>Alisher Karimov</b> (5-A) bugun 2-darsga (Matematika, 09:25) kelmadi.\n\n🏫 <i>1-maktab</i>", text);
    }

    @Test
    void attendanceLate_matchesSpecSample() {
        String text = MessageFormatter.attendanceLate("Alisher Karimov", "5-A", 1, "Ona tili",
                LocalTime.of(8, 30), TODAY, TODAY, "1-maktab");
        assertTrue(text.startsWith("🟡 <b>Alisher Karimov</b> (5-A) bugun 1-darsga (Ona tili, 08:30) kechikib keldi."));
        assertTrue(text.endsWith("🏫 <i>1-maktab</i>"));
    }

    @Test
    void attendance_pastDate_usesDateInsteadOfBugun() {
        String text = MessageFormatter.attendanceAbsent("A B", "5-A", 1, "Fizika", LocalTime.of(8, 0),
                LocalDate.of(2026, 9, 25), TODAY, "M");
        assertTrue(text.contains("25.09.2026 kuni 1-darsga"));
        assertTrue(MessageFormatter.attendanceAbsent("A B", "5-A", 1, "Fizika", LocalTime.of(8, 0),
                TODAY.minusDays(1), TODAY, "M").contains("kecha"));
    }

    @Test
    void gradeNew_matchesSpecSample() {
        String text = MessageFormatter.gradeNew("Alisher Karimov", "Fizika", 5, GradeType.CURRENT, TODAY, "1-maktab");
        assertEquals("📘 <b>Alisher Karimov</b> Fizika fanidan <b>5</b> baho oldi (joriy baho, 30.09.2026).\n\n🏫 <i>1-maktab</i>", text);
    }

    @Test
    void gradeUpdated_mentionsChange() {
        String text = MessageFormatter.gradeUpdated("Ali Valiyev", "Kimyo", 4, GradeType.EXAM, TODAY, "M");
        assertTrue(text.contains("bahosi <b>4</b> ga o'zgartirildi (imtihon bahosi, 30.09.2026)"));
    }

    @Test
    void announcement_escapesUserContentAndAddsSchool() {
        String text = MessageFormatter.announcement("Yig'ilish <muhim>", "Ertaga 18:00 da & zalda", null, "Maktab \"№1\"");
        assertEquals("📢 <b>Maktab e'loni</b>: Yig'ilish &lt;muhim&gt;\nErtaga 18:00 da &amp; zalda\n\n🏫 <i>Maktab &quot;№1&quot;</i>", text);
    }

    @Test
    void announcement_classLabelAndLongContentShortened() {
        String longText = "so'z ".repeat(200);
        String text = MessageFormatter.announcement("Sarlavha", longText, "5-A", "M");
        assertTrue(text.startsWith("📢 <b>5-A sinf e'loni</b>: Sarlavha\n"));
        assertTrue(text.contains("…"));
        assertTrue(text.length() < 400);
    }

    @Test
    void injectedNamesCannotBreakMarkup() {
        String text = MessageFormatter.gradeNew("<script>x</script>", "</b><a href=x>", 5, GradeType.CURRENT, TODAY, "M");
        assertFalse(text.contains("<script>"));
        assertFalse(text.contains("<a href"));
    }
}
