package uz.azizbek.maktabboshqaruv.entity;
import jakarta.persistence.*;

@Entity
@Table(name = "school_class")
public class SchoolClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id ;

    @ManyToOne
    @JoinColumn(name = "academic_year_id" , nullable = false)
    private AcademicYear academicYear;

    @Column(nullable = false)
    private Integer gradeNumber;

    @Column(nullable = false)
    private String sectionLetter;

    @Column(nullable = false)
    private Integer maxStudents;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AcademicYear getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(AcademicYear academicYear) {
        this.academicYear = academicYear;
    }

    public Integer getGradeNumber() {
        return gradeNumber;
    }

    public void setGradeNumber(Integer gradeNumber) {
        this.gradeNumber = gradeNumber;
    }

    public String getSectionLetter() {
        return sectionLetter;
    }

    public void setSectionLetter(String sectionLetter) {
        this.sectionLetter = sectionLetter;
    }

    public Integer getMaxStudents() {
        return maxStudents;
    }

    public void setMaxStudents(Integer maxStudents) {
        this.maxStudents = maxStudents;
    }
}
