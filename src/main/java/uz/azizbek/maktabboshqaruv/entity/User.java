package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;   // bu yerda hech qachon "ochiq matn" saqlanmaydi — keyingi qadamda tushuntiraman

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** Optional link to the teacher's own Employee record, so "my lessons" views can filter to just them. */
    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
}