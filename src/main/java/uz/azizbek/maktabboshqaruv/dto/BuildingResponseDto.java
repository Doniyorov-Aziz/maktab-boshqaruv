package uz.azizbek.maktabboshqaruv.dto;

public class BuildingResponseDto {

    private Long id;
    private String name;
    private Long schoolId;
    private String schoolName;
    private Integer floorCount;
    private Long roomCount;
    private Double occupiedPercentage;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(Long schoolId) {
        this.schoolId = schoolId;
    }

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
    }

    public Integer getFloorCount() {
        return floorCount;
    }

    public void setFloorCount(Integer floorCount) {
        this.floorCount = floorCount;
    }

    public Long getRoomCount() {
        return roomCount;
    }

    public void setRoomCount(Long roomCount) {
        this.roomCount = roomCount;
    }

    public Double getOccupiedPercentage() {
        return occupiedPercentage;
    }

    public void setOccupiedPercentage(Double occupiedPercentage) {
        this.occupiedPercentage = occupiedPercentage;
    }
}
