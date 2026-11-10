package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * To'lovlar for a private school: whether the section is on (off by default, since a state school has no fees),
 * the monthly fee, the day of the month it is due, and whether the bot reminds parents of an unpaid month.
 */
@Entity
@Table(name = "payment_settings")
public class PaymentSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false, unique = true)
    private School school;

    @Column(nullable = false)
    private boolean enabled;

    /** So'm a month; a pupil's own fee ({@link StudentFee}) wins over it. */
    @Column(name = "monthly_fee", nullable = false)
    private long monthlyFee;

    /** The month's fee is due on this day (1..28); from the next day an unpaid month is a debt. */
    @Column(name = "due_day", nullable = false)
    private int dueDay = 10;

    @Column(name = "remind_parents", nullable = false)
    private boolean remindParents = true;

    /** Card / bank details shown to parents in the bot. */
    @Column(name = "pay_info", length = 500)
    private String payInfo;

    private LocalDateTime updatedAt;

    @Column(length = 64)
    private String updatedBy;

    public Long getId() { return id; }
    public School getSchool() { return school; }
    public void setSchool(School school) { this.school = school; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public long getMonthlyFee() { return monthlyFee; }
    public void setMonthlyFee(long monthlyFee) { this.monthlyFee = monthlyFee; }
    public int getDueDay() { return dueDay; }
    public void setDueDay(int dueDay) { this.dueDay = dueDay; }
    public boolean isRemindParents() { return remindParents; }
    public void setRemindParents(boolean remindParents) { this.remindParents = remindParents; }
    public String getPayInfo() { return payInfo; }
    public void setPayInfo(String payInfo) { this.payInfo = payInfo; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
