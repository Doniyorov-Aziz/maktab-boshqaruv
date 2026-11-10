package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** O'quvchi yutug'i: an olympiad, a competition, a certificate, with its level, place and a scan of the diploma. */
@Entity
@Table(name = "achievement", indexes = {
        @Index(name = "idx_achievement_student", columnList = "student_id, achieved_on"),
        @Index(name = "idx_achievement_school", columnList = "school_id, achieved_on")
})
public class Achievement {

    public enum Category { OLYMPIAD, COMPETITION, SPORT, ART, CERTIFICATE, PROJECT, OTHER }

    public enum Level { SCHOOL, DISTRICT, REGION, NATIONAL, INTERNATIONAL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /** The subject of an olympiad (optional). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Level level;

    /** 1, 2, 3 = the place; null = took part / a certificate without a place. */
    private Integer place;

    @Column(name = "achieved_on", nullable = false)
    private LocalDate achievedOn;

    @Column(length = 1000)
    private String description;

    @Column(name = "file_path", length = 300)
    private String filePath;

    @Column(name = "file_name", length = 160)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public School getSchool() { return school; }
    public void setSchool(School school) { this.school = school; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public Level getLevel() { return level; }
    public void setLevel(Level level) { this.level = level; }
    public Integer getPlace() { return place; }
    public void setPlace(Integer place) { this.place = place; }
    public LocalDate getAchievedOn() { return achievedOn; }
    public void setAchievedOn(LocalDate achievedOn) { this.achievedOn = achievedOn; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
