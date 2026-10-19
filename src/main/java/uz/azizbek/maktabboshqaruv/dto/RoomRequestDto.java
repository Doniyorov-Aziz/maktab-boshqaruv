package uz.azizbek.maktabboshqaruv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class RoomRequestDto {

    @NotNull(message = "Bino tanlanishi shart")
    private Long buildingId;

    @NotBlank(message = "Xona raqami kiritilishi shart")
    private String roomNumber;

    @NotNull(message = "Sig'im kiritilishi shart")
    @Positive(message = "Sig'im musbat son bo'lishi kerak")
    private Integer capacity;

    private Integer floor;

    private String type;

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

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
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
}
