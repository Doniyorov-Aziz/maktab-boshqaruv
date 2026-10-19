package uz.azizbek.maktabboshqaruv.service.pages;

import uz.azizbek.maktabboshqaruv.entity.CalendarEvent;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * The school calendar as an iCalendar file (RFC 5545): CRLF lines folded at 75 octets, text
 * escaped, the Asia/Tashkent zone (UTC+5, no daylight saving) declared once. A whole-day event is
 * DTSTART;VALUE=DATE with an exclusive DTEND (the day after its last day); an event with a time is
 * local time in that zone.
 */
public final class IcsWriter {

    public static final String ZONE = "Asia/Tashkent";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter LOCAL = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    static final Map<String, String> TYPE_NAMES = Map.of("HOLIDAY", "Bayram", "EXAM", "Imtihon / nazorat",
            "PARENT_MEETING", "Ota-onalar yig'ilishi", "VACATION", "Ta'til", "SCHOOL_EVENT", "Maktab tadbiri",
            "OTHER", "Boshqa");

    private IcsWriter() {
    }

    public static byte[] write(String calendarName, List<CalendarEvent> events, LocalDateTime nowUtc, String host) {
        StringBuilder out = new StringBuilder();
        line(out, "BEGIN:VCALENDAR");
        line(out, "VERSION:2.0");
        line(out, "PRODID:-//Maktab Boshqaruv//Taqvim//UZ");
        line(out, "CALSCALE:GREGORIAN");
        line(out, "METHOD:PUBLISH");
        line(out, "X-WR-CALNAME:" + text(calendarName));
        line(out, "X-WR-TIMEZONE:" + ZONE);
        line(out, "BEGIN:VTIMEZONE");
        line(out, "TZID:" + ZONE);
        line(out, "BEGIN:STANDARD");
        line(out, "DTSTART:19700101T000000");
        line(out, "TZOFFSETFROM:+0500");
        line(out, "TZOFFSETTO:+0500");
        line(out, "TZNAME:+05");
        line(out, "END:STANDARD");
        line(out, "END:VTIMEZONE");
        String stamp = nowUtc.format(UTC);
        for (CalendarEvent e : events) {
            line(out, "BEGIN:VEVENT");
            line(out, "UID:event-" + e.getId() + "@" + host);
            line(out, "DTSTAMP:" + stamp);
            if (e.getStartTime() != null && e.getEndTime() != null) {
                line(out, "DTSTART;TZID=" + ZONE + ":" + e.getStartDate().atTime(e.getStartTime()).format(LOCAL));
                line(out, "DTEND;TZID=" + ZONE + ":" + e.getEndDate().atTime(e.getEndTime()).format(LOCAL));
            } else {
                line(out, "DTSTART;VALUE=DATE:" + e.getStartDate().format(DATE));
                line(out, "DTEND;VALUE=DATE:" + e.getEndDate().plusDays(1).format(DATE));
            }
            line(out, "SUMMARY:" + text(e.getTitle()));
            if (e.getDescription() != null && !e.getDescription().isBlank()) {
                line(out, "DESCRIPTION:" + text(e.getDescription()));
            }
            String place = e.getRoom() != null ? TimetablePageService.roomLabel(e.getRoom().getRoomNumber()) : e.getLocation();
            if (place != null && !place.isBlank()) line(out, "LOCATION:" + text(place));
            line(out, "CATEGORIES:" + text(TYPE_NAMES.getOrDefault(e.getType().name(), "Boshqa")));
            if (e.getType().name().equals("VACATION") || e.getType().name().equals("HOLIDAY")) {
                line(out, "TRANSP:TRANSPARENT");
            }
            line(out, "END:VEVENT");
        }
        line(out, "END:VCALENDAR");
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** TEXT value: backslash, semicolon, comma and new lines escaped (RFC 5545 3.3.11). */
    static String text(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,")
                .replace("\r\n", "\\n").replace("\n", "\\n").replace("\r", "\\n");
    }

    /** One content line, folded so no line is longer than 75 octets (UTF-8), ending in CRLF. */
    static void line(StringBuilder out, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= 75) {
            out.append(content).append("\r\n");
            return;
        }
        int limit = 75;
        int used = 0;
        StringBuilder current = new StringBuilder();
        for (int k = 0; k < content.length(); ) {
            int cp = content.codePointAt(k);
            String ch = new String(Character.toChars(cp));
            int size = ch.getBytes(StandardCharsets.UTF_8).length;
            if (used + size > limit) {
                out.append(current).append("\r\n ");
                current.setLength(0);
                used = 0;
                limit = 74; // a folded line starts with one space
            }
            current.append(ch);
            used += size;
            k += Character.charCount(cp);
        }
        out.append(current).append("\r\n");
    }

    /** UTC "now" for DTSTAMP. */
    public static LocalDateTime utc(java.time.Clock clock) {
        return LocalDateTime.now(clock.withZone(ZoneOffset.UTC));
    }

    /** First and last date the file covers: the school year around today. */
    public static LocalDate[] range(LocalDate today) {
        int start = today.getMonthValue() >= 8 ? today.getYear() : today.getYear() - 1;
        return new LocalDate[]{LocalDate.of(start - 1, 8, 1), LocalDate.of(start + 1, 8, 31)};
    }
}
