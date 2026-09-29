package uz.azizbek.maktabboshqaruv.dto;

import java.time.LocalDate;

public class HeatmapDayDto {

    private LocalDate date;
    private String status;

    public HeatmapDayDto() {
    }

    public HeatmapDayDto(LocalDate date, String status) {
        this.date = date;
        this.status = status;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
