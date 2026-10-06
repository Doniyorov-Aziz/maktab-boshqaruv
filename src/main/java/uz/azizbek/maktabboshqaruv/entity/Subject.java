package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "subject")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "school_id")
    private School school;

    @Column(nullable = false)
    private String name;

    /**
     * Subjects are never deleted: an old subject becomes inactive, so grades and
     * timetables of past years keep showing its name. Null (rows from before) = active.
     */
    private Boolean active;

    public boolean isActive() {
        return active == null || active;
    }

    public void setActive(boolean active) {
        this.active = active;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
