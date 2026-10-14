package uz.azizbek.maktabboshqaruv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegionRequestDto(
        @NotBlank(message = "SOATO kodi kiritilishi shart") @Size(max = 10, message = "Kod 10 belgidan oshmasin") String code,
        @NotBlank(message = "Hudud nomi kiritilishi shart") @Size(max = 100, message = "Nom 100 belgidan oshmasin") String nameUz) {
}
