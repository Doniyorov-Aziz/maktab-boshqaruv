package uz.azizbek.maktabboshqaruv.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.azizbek.maktabboshqaruv.entity.DemoExcludedVisitor;
import uz.azizbek.maktabboshqaruv.entity.DemoVisit;
import uz.azizbek.maktabboshqaruv.repository.DemoExcludedVisitorRepository;
import uz.azizbek.maktabboshqaruv.repository.DemoVisitRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Counts the public demo's visitors. The browser keeps a random visitor id for a year and
 * reports the pages it opens ({@link #ping}); a visit (session) ends after 30 minutes without
 * activity. Not counted: crawlers (by User-Agent), and browsers an administrator signed in from
 * ({@link #exclude}). The IP address is kept only as a salted SHA-256.
 */
@Service
public class DemoVisitService {

    private static final Logger log = LoggerFactory.getLogger(DemoVisitService.class);

    static final Duration SESSION_GAP = Duration.ofMinutes(30);
    /** The browser reports at most every 30 s; a little slack for clocks and slow networks. */
    static final Duration MIN_PING_GAP = Duration.ofSeconds(25);
    private static final int MAX_PAGES_KEPT = 300;
    private static final int MAX_PATHS_PER_PING = 30;

    private static final Pattern VISITOR_ID = Pattern.compile("^[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$");
    private static final Pattern BOT = Pattern.compile(
            "(?i)bot|crawl|spider|slurp|scrap|facebookexternalhit|embedly|preview|headless|lighthouse|pingdom|"
                    + "monitor|python|curl|wget|httpclient|okhttp|java/|go-http|axios/|node-fetch|phantom|selenium|puppeteer");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    public enum Result { COUNTED, THROTTLED, IGNORED }

    private final DemoVisitRepository visits;
    private final DemoExcludedVisitorRepository excluded;
    private final Clock clock;
    private final byte[] salt;

    public DemoVisitService(DemoVisitRepository visits, DemoExcludedVisitorRepository excluded, Clock clock,
                            @Value("${app.demo.visit-hash-salt:}") String salt) {
        this.visits = visits;
        this.excluded = excluded;
        this.clock = clock;
        if (salt == null || salt.isBlank()) {
            // without a configured salt the hashes cannot be compared across restarts — fine, never reversible
            byte[] random = new byte[32];
            new SecureRandom().nextBytes(random);
            this.salt = random;
            log.warn("VISIT_HASH_SALT berilmagan: IP xeshlari uchun tasodifiy tuz ishlatiladi (qayta ishga tushganda o'zgaradi)");
        } else {
            this.salt = salt.getBytes(StandardCharsets.UTF_8);
        }
    }

    public static boolean validVisitorId(String id) {
        return id != null && VISITOR_ID.matcher(id).matches();
    }

    public static boolean isBot(String userAgent) {
        return userAgent == null || userAgent.isBlank() || BOT.matcher(userAgent).find();
    }

    /** A page (or several) a guest opened; starts a new visit after 30 minutes of silence. */
    @Transactional
    public synchronized Result ping(String visitorId, String ref, List<String> paths, String userAgent, String ip) {
        if (!validVisitorId(visitorId) || isBot(userAgent) || excluded.existsById(visitorId.toLowerCase(Locale.ROOT))) {
            return Result.IGNORED;
        }
        String id = visitorId.toLowerCase(Locale.ROOT);
        LocalDateTime now = LocalDateTime.now(clock).withNano(0);
        DemoVisit visit = visits.findFirstByVisitorIdOrderByLastSeenDesc(id)
                .filter(v -> v.getLastSeen().isAfter(now.minus(SESSION_GAP)))
                .orElse(null);
        if (visit != null && visit.getLastSeen().isAfter(now.minus(MIN_PING_GAP))) {
            return Result.THROTTLED;
        }
        if (visit == null) {
            visit = new DemoVisit();
            visit.setVisitorId(id);
            visit.setSessionId(UUID.randomUUID().toString());
            visit.setRef(cleanRef(ref));
            visit.setFirstSeen(now);
            visit.setDevice(device(userAgent));
            visit.setBrowser(browser(userAgent));
            visit.setIpHash(ip == null ? null : hash(ip));
            visit.setPages("");
        }
        List<String> clean = paths == null ? List.of()
                : paths.stream().map(DemoVisitService::cleanPath).filter(Objects::nonNull).limit(MAX_PATHS_PER_PING).toList();
        if (!clean.isEmpty()) {
            StringBuilder sb = new StringBuilder(visit.getPages() == null ? "" : visit.getPages());
            for (String p : clean) sb.append(TIME.format(now)).append(' ').append(p).append('\n');
            visit.setPages(lastLines(sb.toString()));
            visit.setPageCount(visit.getPageCount() + clean.size());
        }
        visit.setLastSeen(now);
        visits.save(visit);
        return Result.COUNTED;
    }

    /** An administrator signed in from this browser: forget its visits and never count it again. */
    @Transactional
    public void exclude(String visitorId, String username) {
        if (!validVisitorId(visitorId)) return;
        String id = visitorId.toLowerCase(Locale.ROOT);
        if (!excluded.existsById(id)) {
            DemoExcludedVisitor e = new DemoExcludedVisitor();
            e.setVisitorId(id);
            e.setUsername(username);
            e.setCreatedAt(LocalDateTime.now(clock));
            excluded.save(e);
        }
        visits.deleteByVisitorId(id);
    }

    public record Summary(long visitors, long today) {
    }

    @Transactional(readOnly = true)
    public Summary summary() {
        return new Summary(visits.countVisitors(), visits.countVisitorsSince(LocalDate.now(clock).atStartOfDay()));
    }

    public record DayRow(LocalDate date, long visits, long visitors) {
    }

    public record Count(String key, long count) {
    }

    public record Page(String time, String path) {
    }

    public record Row(long id, LocalDateTime firstSeen, LocalDateTime lastSeen, String ref, String device, String browser,
                      long durationSeconds, int pageCount, List<Page> pages) {
    }

    public record Stats(long visitors, long today, long week, long visits, long avgDurationSeconds,
                        List<DayRow> perDay, List<Count> sources, List<Count> topPages, List<Count> devices,
                        List<Count> browsers, List<Row> recent) {
    }

    /** "Tashriflar" page: KPIs, the last 30 days, sources, pages, devices and the latest 50 visits. */
    @Transactional(readOnly = true)
    public Stats stats() {
        LocalDate today = LocalDate.now(clock);
        LocalDate from = today.minusDays(29);
        List<DemoVisit> recent = visits.findByFirstSeenGreaterThanEqualOrderByFirstSeenDesc(today.minusDays(89).atStartOfDay());
        List<DemoVisit> all = visits.findAll();

        Map<LocalDate, long[]> days = new LinkedHashMap<>();
        Map<LocalDate, Set<String>> dayVisitors = new HashMap<>();
        for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) days.put(d, new long[1]);
        Map<String, Long> sources = new HashMap<>();
        Map<String, Long> pages = new HashMap<>();
        Map<String, Long> devices = new HashMap<>();
        Map<String, Long> browsers = new HashMap<>();
        for (DemoVisit v : recent) {
            LocalDate d = v.getFirstSeen().toLocalDate();
            if (days.containsKey(d)) {
                days.get(d)[0]++;
                dayVisitors.computeIfAbsent(d, k -> new HashSet<>()).add(v.getVisitorId());
            }
            sources.merge(v.getRef() == null ? "direct" : v.getRef(), 1L, Long::sum);
            devices.merge(v.getDevice() == null ? "DESKTOP" : v.getDevice(), 1L, Long::sum);
            browsers.merge(v.getBrowser() == null ? "Boshqa" : v.getBrowser(), 1L, Long::sum);
            for (Page p : pagesOf(v)) pages.merge(p.path(), 1L, Long::sum);
        }
        long avg = Math.round(all.stream().mapToLong(DemoVisitService::seconds).average().orElse(0));
        LocalDateTime monday = today.with(DayOfWeek.MONDAY).atStartOfDay();

        List<DayRow> perDay = days.entrySet().stream()
                .map(e -> new DayRow(e.getKey(), e.getValue()[0], dayVisitors.getOrDefault(e.getKey(), Set.of()).size()))
                .toList();
        List<Row> rows = recent.stream().limit(50)
                .map(v -> new Row(v.getId(), v.getFirstSeen(), v.getLastSeen(), v.getRef() == null ? "direct" : v.getRef(),
                        v.getDevice(), v.getBrowser(), seconds(v), v.getPageCount(), pagesOf(v)))
                .toList();
        return new Stats(visits.countVisitors(), visits.countVisitorsSince(today.atStartOfDay()), visits.countVisitorsSince(monday),
                all.size(), avg, perDay, sorted(sources, 20), sorted(pages, 10), sorted(devices, 5), sorted(browsers, 8), rows);
    }

    private static long seconds(DemoVisit v) {
        return Math.max(0, Duration.between(v.getFirstSeen(), v.getLastSeen()).getSeconds());
    }

    private static List<Page> pagesOf(DemoVisit v) {
        if (v.getPages() == null || v.getPages().isBlank()) return List.of();
        List<Page> out = new ArrayList<>();
        for (String line : v.getPages().split("\n")) {
            int sp = line.indexOf(' ');
            if (sp > 0) out.add(new Page(line.substring(0, sp), line.substring(sp + 1)));
        }
        return out;
    }

    private static List<Count> sorted(Map<String, Long> map, int limit) {
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .map(e -> new Count(e.getKey(), e.getValue()))
                .toList();
    }

    private static String lastLines(String text) {
        String[] lines = text.split("\n");
        if (lines.length <= MAX_PAGES_KEPT) return text;
        return String.join("\n", Arrays.copyOfRange(lines, lines.length - MAX_PAGES_KEPT, lines.length)) + "\n";
    }

    /** "LinkedIn" → "linkedin"; letters, digits, '-' and '_' only; empty → "direct". */
    static String cleanRef(String ref) {
        if (ref == null) return "direct";
        String r = ref.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        if (r.length() > 40) r = r.substring(0, 40);
        return r.isEmpty() ? "direct" : r;
    }

    /** A route of the app ("/app/students"), without its query; anything else is dropped. */
    static String cleanPath(String path) {
        if (path == null) return null;
        int q = path.indexOf('?');
        String p = q >= 0 ? path.substring(0, q) : path;
        if (!p.startsWith("/") || p.length() > 120 || !p.matches("[A-Za-z0-9/_.:-]*")) return null;
        return p;
    }

    static String device(String ua) {
        String u = ua == null ? "" : ua.toLowerCase(Locale.ROOT);
        if (u.contains("ipad") || u.contains("tablet") || (u.contains("android") && !u.contains("mobile"))) return "TABLET";
        if (u.contains("mobi") || u.contains("iphone") || u.contains("android")) return "MOBILE";
        return "DESKTOP";
    }

    static String browser(String ua) {
        String u = ua == null ? "" : ua;
        if (u.contains("Edg/")) return "Edge";
        if (u.contains("OPR/") || u.contains("Opera")) return "Opera";
        if (u.contains("YaBrowser")) return "Yandex";
        if (u.contains("SamsungBrowser")) return "Samsung";
        if (u.contains("Telegram")) return "Telegram";
        if (u.contains("Firefox/")) return "Firefox";
        if (u.contains("Chrome/")) return "Chrome";
        if (u.contains("Safari/")) return "Safari";
        return "Boshqa";
    }

    String hash(String ip) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update(salt);
            sha.update((byte) ':');
            byte[] digest = sha.digest(ip.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
