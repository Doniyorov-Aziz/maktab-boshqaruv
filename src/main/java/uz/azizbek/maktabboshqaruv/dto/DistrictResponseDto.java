package uz.azizbek.maktabboshqaruv.dto;

import uz.azizbek.maktabboshqaruv.entity.District;

public record DistrictResponseDto(Long id, Long regionId, String regionName, String code, String nameUz) {

    public static DistrictResponseDto from(District d) {
        return new DistrictResponseDto(d.getId(), d.getRegion().getId(), d.getRegion().getNameUz(), d.getCode(),
                d.getNameUz());
    }
}
