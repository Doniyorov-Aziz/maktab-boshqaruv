package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * One session of a public demo visitor: from the first page to the last activity, a new session
 * after 30 minutes without any. The visitor is a random id kept by their browser for a year; the
 * IP address is never stored, only its salted SHA-256.
 */
@Entity
@Table(name = "demo_visit", indexes = {
        @Index(name = "idx_demo_visit_visitor", columnList = "visitor_id, last_seen"),
        @Index(name = "idx_demo_visit_first_seen", columnList = "first_seen")
})
public class DemoVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visitor_id", nullable = false, length = 36)
    private String visitorId;

    @Column(name = "session_id", nullable = false, length = 36, unique = true)
    private String sessionId;

    /** Where the link was shared: linkedin, telegram, direct, … (the ?ref= of the demo link). */
    @Column(length = 40)
    private String ref;

    @Column(name = "first_seen", nullable = false)
    private LocalDateTime firstSeen;

    @Column(name = "last_seen", nullable = false)
    private LocalDateTime lastSeen;

    @Column(name = "page_count", nullable = false)
    private int pageCount;

    /** The pages in order, one per line: "HH:mm:ss /path". */
    @Column(columnDefinition = "text")
    private String pages;

    /** DESKTOP, MOBILE or TABLET. */
    @Column(length = 10)
    private String device;

    @Column(length = 20)
    private String browser;

    @Column(name = "ip_hash", length = 64)
    private String ipHash;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVisitorId() {
        return visitorId;
    }

    public void setVisitorId(String visitorId) {
        this.visitorId = visitorId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public LocalDateTime getFirstSeen() {
        return firstSeen;
    }

    public void setFirstSeen(LocalDateTime firstSeen) {
        this.firstSeen = firstSeen;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public String getPages() {
        return pages;
    }

    public void setPages(String pages) {
        this.pages = pages;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getIpHash() {
        return ipHash;
    }

    public void setIpHash(String ipHash) {
        this.ipHash = ipHash;
    }
}
