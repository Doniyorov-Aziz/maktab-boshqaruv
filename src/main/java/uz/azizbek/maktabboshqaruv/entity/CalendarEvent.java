package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "calendar_event")
@EntityListeners(AuditingEntityListener.class)
public class CalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CalendarEventType type;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** True for events the demo seeder created (so a reseed can safely replace only those, never a user's own). */
    @Column
    private boolean seeded = false;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdDate;

    // ---- pages v2 (all nullable: older events are whole-day, whole-school, without a room)

    /** Time of day (both or neither); null = a whole-day event. */
    private java.time.LocalTime startTime;
    private java.time.LocalTime endTime;

    /** The room it takes (booked automatically, conflicts are refused). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    /** A place outside the rooms ("Hovli", "Stadion"). */
    @Column(length = 120)
    private String location;

    /** For whom: class ids, comma separated; null = the whole school. */
    @Column(length = 1000)
    private String classIds;

    /**
     * Tell the parents (Telegram) when it is created and the day before. null (events from before)
     * keeps the old behaviour: every parent of the school gets the day-before reminder.
     */
    private Boolean notifyParents;

    /** Events of one repeating series ("har hafta", "har oy") share this id. */
    @Column(length = 36)
    private String seriesId;

    public java.time.LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(java.time.LocalTime startTime) {
        this.startTime = startTime;
    }

    public java.time.LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(java.time.LocalTime endTime) {
        this.endTime = endTime;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getClassIds() {
        return classIds;
    }

    public void setClassIds(String classIds) {
        this.classIds = classIds;
    }

    /** The audience's class ids; empty = the whole school. */
    public java.util.List<Long> classIdList() {
        if (classIds == null || classIds.isBlank()) return java.util.List.of();
        return java.util.Arrays.stream(classIds.split(",")).map(String::trim).filter(x -> !x.isEmpty())
                .map(Long::valueOf).toList();
    }

    public Boolean getNotifyParents() {
        return notifyParents;
    }

    public void setNotifyParents(Boolean notifyParents) {
        this.notifyParents = notifyParents;
    }

    public String getSeriesId() {
        return seriesId;
    }

    public void setSeriesId(String seriesId) {
        this.seriesId = seriesId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public School getSchool() {
        return school;
    }

    public void setSchool(School school) {
        this.school = school;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CalendarEventType getType() {
        return type;
    }

    public void setType(CalendarEventType type) {
        this.type = type;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public boolean isSeeded() {
        return seeded;
    }

    public void setSeeded(boolean seeded) {
        this.seeded = seeded;
    }
}
