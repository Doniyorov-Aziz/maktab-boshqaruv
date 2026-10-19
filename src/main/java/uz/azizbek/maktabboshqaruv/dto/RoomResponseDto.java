package uz.azizbek.maktabboshqaruv.dto;

public class RoomResponseDto {

    private Long id;
    private String roomNumber;
    private Integer capacity;
    private Integer floor;
    private String type;
    private Long buildingId;
    private String buildingName;
    private String currentStatus;

    /** ACTIVE / REPAIR (null = ACTIVE) */
    private String status;
    /** equipment tags (PROJECTOR, SMARTBOARD, AC) */
    private java.util.List<String> equipment;
    private Integer computers;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public java.util.List<String> getEquipment() {
        return equipment;
    }

    public void setEquipment(java.util.List<String> equipment) {
        this.equipment = equipment;
    }

    public Integer getComputers() {
        return computers;
    }

    public void setComputers(Integer computers) {
        this.computers = computers;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public Integer getFloor() {
        return floor;
    }

    public void setFloor(Integer floor) {
        this.floor = floor;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }
}
