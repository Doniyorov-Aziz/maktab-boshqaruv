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
}
