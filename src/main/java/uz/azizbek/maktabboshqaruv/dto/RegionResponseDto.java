package uz.azizbek.maktabboshqaruv.dto;

import uz.azizbek.maktabboshqaruv.entity.Region;

public record RegionResponseDto(Long id, String code, String nameUz) {

    public static RegionResponseDto from(Region r) {
        return new RegionResponseDto(r.getId(), r.getCode(), r.getNameUz());
    }
}
