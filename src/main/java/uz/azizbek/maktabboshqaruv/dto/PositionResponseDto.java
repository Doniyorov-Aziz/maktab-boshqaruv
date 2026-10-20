package uz.azizbek.maktabboshqaruv.dto;

public class PositionResponseDto {

    private Long id;
    private String title;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    // list page numbers (ListStatsService); null where not computed
    private Integer employeeCount;

    public Integer getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(Integer employeeCount) {
        this.employeeCount = employeeCount;
    }

    @jakarta.validation.constraints.Size(max = 500, message = "Tavsif 500 belgidan oshmasin")
    private String description;
    /** ADMIN / EDITOR / VIEWER — offered to a new user of an employee in this position. */
    private String defaultRole;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDefaultRole() {
        return defaultRole;
    }

    public void setDefaultRole(String defaultRole) {
        this.defaultRole = defaultRole;
    }

    /** The first few employees (avatars on the card). */
    private java.util.List<uz.azizbek.maktabboshqaruv.service.pages.ListStatsService.Person> employees;

    public java.util.List<uz.azizbek.maktabboshqaruv.service.pages.ListStatsService.Person> getEmployees() {
        return employees;
    }

    public void setEmployees(java.util.List<uz.azizbek.maktabboshqaruv.service.pages.ListStatsService.Person> employees) {
        this.employees = employees;
    }
}
