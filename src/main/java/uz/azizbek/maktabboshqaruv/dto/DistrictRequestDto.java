package uz.azizbek.maktabboshqaruv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DistrictRequestDto(
        @NotNull(message = "Hudud tanlanishi shart") Long regionId,
        @Size(max = 10, message = "Kod 10 belgidan oshmasin") String code,
        @NotBlank(message = "Tuman nomi kiritilishi shart") @Size(max = 100, message = "Nom 100 belgidan oshmasin") String nameUz) {
}
