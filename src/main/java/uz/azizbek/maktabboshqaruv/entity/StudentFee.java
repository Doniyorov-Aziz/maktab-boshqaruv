package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;

/** A pupil's own monthly fee (a discount, a sibling rate, 0 = exempt) instead of the school's. */
@Entity
@Table(name = "student_fee")
public class StudentFee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private Student student;

    @Column(name = "monthly_fee", nullable = false)
    private long monthlyFee;

    @Column(length = 200)
    private String note;

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public long getMonthlyFee() { return monthlyFee; }
    public void setMonthlyFee(long monthlyFee) { this.monthlyFee = monthlyFee; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
