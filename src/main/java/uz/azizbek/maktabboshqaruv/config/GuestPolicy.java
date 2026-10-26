package uz.azizbek.maktabboshqaruv.config;

import org.springframework.http.server.PathContainer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * What the public demo visitor (role GUEST) may do. The guest reads every page an ADMIN
 * reads, but:
 * <ul>
 *   <li>only GET / HEAD / OPTIONS — any other method is 403, except the few in {@link #WRITE_ALLOWED};</li>
 *   <li>the sections in {@link #LOCKED} are 403 even for GET (parents' messages and their files,
 *       Telegram history and recipients, link codes, API keys, calendar feed tokens, visit statistics);</li>
 *   <li>file exports (Excel, PDF, .ics, …) are 403.</li>
 * </ul>
 * The rules are path based and checked in {@link GuestGuardFilter} before any controller, so an
 * endpoint added later is covered without touching it.
 */
public final class GuestPolicy {

    public static final String ROLE = "ROLE_GUEST";

    public static final String READ_ONLY_MESSAGE = "Demo rejimida o'zgartirish mumkin emas";
    public static final String LOCKED_MESSAGE =
            "Uzr, sizga bu bo'limga kirishga ruxsat yo'q. Ota-onalar yozishmalari maxfiy ma'lumot hisoblanadi.";
    public static final String EXPORT_MESSAGE = "Demo rejimida eksport o'chirilgan";

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    /** The only writes a guest makes: signing in and out, and counting their own visit. */
    static final List<PathPattern> WRITE_ALLOWED = patterns(
            "/api/auth/guest-login",
            "/api/auth/login",
            "/api/auth/logout",
            "/api/demo/visit/ping"
    );

    /** Closed for the guest even to read. */
    static final List<PathPattern> LOCKED = patterns(
            // "Maktabga xat": parents' appeals, the messages inside and their files
            "/api/appeals", "/api/appeals/**",
            "/api/dashboard/my-appeals",
            // absence requests: a parent's own words and photos
            "/api/absence-requests", "/api/absence-requests/**",
            // Telegram outbox (NotificationLog), sent messages and their recipients, the send page
            "/api/notifications", "/api/notifications/**",
            "/api/broadcasts", "/api/broadcasts/**",
            // link codes (whoever holds one subscribes to a child), files parents sent, the mock chat
            "/api/telegram/students/**", "/api/telegram/classes/**",
            "/api/telegram/files/**", "/api/telegram/mock/**",
            // a parent's reading card is looked up by their Telegram chat id
            "/api/library/stats/parents/**",
            // rights holders' signed permission scans
            "/api/library/permissions/*/document",
            // outgoing requests to Wikisource
            "/api/library/wikisource/**",
            // secrets: device API keys, calendar subscription tokens
            "/api/device-keys", "/api/device-keys/**",
            "/api/pages/calendar/feeds", "/api/pages/calendar/feeds/**",
            "/api/auth/hash/**",
            // the demo's own visit statistics are for the administrator
            "/api/demo/visits", "/api/demo/visits/**"
    );

    /** Downloads of files made from the data: /export, /export.xlsx, timetable.pdf, export.ics, … */
    private static final Pattern EXPORT = Pattern.compile(
            "(?i).*/export([./].*)?|.*\\.(xlsx|xls|csv|pdf|docx|ics|zip)$|/api/library/permissions/template");

    private GuestPolicy() {
    }

    /** Why a guest may not make this request, or null when it is allowed. */
    public static Denial check(String method, String path) {
        PathContainer p = PathContainer.parsePath(path);
        if (!SAFE_METHODS.contains(method.toUpperCase())) {
            return matches(WRITE_ALLOWED, p) ? null : Denial.READ_ONLY;
        }
        if (matches(LOCKED, p)) return Denial.LOCKED;
        if (EXPORT.matcher(path).matches()) return Denial.EXPORT;
        return null;
    }

    public enum Denial {
        READ_ONLY(READ_ONLY_MESSAGE), LOCKED(LOCKED_MESSAGE), EXPORT(EXPORT_MESSAGE);

        public final String message;

        Denial(String message) {
            this.message = message;
        }
    }

    public static boolean isGuest(Authentication auth) {
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> ROLE.equals(a.getAuthority()));
    }

    public static boolean currentIsGuest() {
        return isGuest(SecurityContextHolder.getContext().getAuthentication());
    }

    private static boolean matches(List<PathPattern> list, PathContainer path) {
        for (PathPattern pattern : list) {
            if (pattern.matches(path)) return true;
        }
        return false;
    }

    private static List<PathPattern> patterns(String... raw) {
        PathPatternParser parser = new PathPatternParser();
        return java.util.Arrays.stream(raw).map(parser::parse).toList();
    }
}
