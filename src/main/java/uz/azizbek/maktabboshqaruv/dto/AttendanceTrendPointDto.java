package uz.azizbek.maktabboshqaruv.dto;

import java.time.LocalDate;

public class AttendanceTrendPointDto {

    private LocalDate date;
    private Double rate;

    public AttendanceTrendPointDto() {
    }

    public AttendanceTrendPointDto(LocalDate date, Double rate) {
        this.date = date;
        this.rate = rate;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }
}
