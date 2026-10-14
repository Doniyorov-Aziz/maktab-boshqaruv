package uz.azizbek.maktabboshqaruv.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.dto.DistrictRequestDto;
import uz.azizbek.maktabboshqaruv.dto.DistrictResponseDto;
import uz.azizbek.maktabboshqaruv.entity.District;
import uz.azizbek.maktabboshqaruv.entity.Region;
import uz.azizbek.maktabboshqaruv.repository.DistrictRepository;
import uz.azizbek.maktabboshqaruv.repository.RegionRepository;

import java.util.List;

/** Tumanlar ma'lumotnomasi: the same rules as regions — active rows only, soft delete. */
@Service
public class DistrictService {

    private final DistrictRepository districts;
    private final RegionRepository regions;

    public DistrictService(DistrictRepository districts, RegionRepository regions) {
        this.districts = districts;
        this.regions = regions;
    }

    @Transactional(readOnly = true)
    public List<DistrictResponseDto> getAll() {
        return districts.findAllByStateAndRegionStateOrderByRegionNameUzAscNameUzAsc(Region.ACTIVE, Region.ACTIVE)
                .stream().map(DistrictResponseDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<DistrictResponseDto> getByRegion(Long regionId) {
        activeRegion(regionId);
        return districts.findAllByRegionIdAndStateOrderByNameUz(regionId, Region.ACTIVE).stream()
                .map(DistrictResponseDto::from).toList();
    }

    @Transactional(readOnly = true)
    public DistrictResponseDto getById(Long id) {
        return DistrictResponseDto.from(findActive(id));
    }

    @Transactional
    public DistrictResponseDto create(DistrictRequestDto request) {
        Region region = activeRegion(request.regionId());
        String name = request.nameUz().trim();
        String code = blankToNull(request.code());
        if (districts.existsByRegionIdAndNameUzIgnoreCase(region.getId(), name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu hududda «" + name + "» allaqachon bor");
        }
        if (code != null && districts.existsByCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu kod allaqachon mavjud: " + code);
        }
        District d = new District();
        d.setRegion(region);
        d.setCode(code);
        d.setNameUz(name);
        return DistrictResponseDto.from(districts.save(d));
    }

    @Transactional
    public DistrictResponseDto update(Long id, DistrictRequestDto request) {
        District d = findActive(id);
        Region region = activeRegion(request.regionId());
        String name = request.nameUz().trim();
        String code = blankToNull(request.code());
        boolean renamed = !d.getRegion().getId().equals(region.getId()) || !d.getNameUz().equalsIgnoreCase(name);
        if (renamed && districts.existsByRegionIdAndNameUzIgnoreCase(region.getId(), name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu hududda «" + name + "» allaqachon bor");
        }
        if (code != null && !code.equals(d.getCode()) && districts.existsByCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu kod allaqachon mavjud: " + code);
        }
        d.setRegion(region);
        d.setCode(code);
        d.setNameUz(name);
        return DistrictResponseDto.from(d);
    }

    @Transactional
    public void delete(Long id) {
        findActive(id).setState(Region.DELETED);
    }

    /** An active district for a student's address; 400 when it is missing or not in the given region. */
    @Transactional(readOnly = true)
    public District forAddress(Long districtId, Long regionId, String what) {
        if (districtId == null) return null;
        District d = districts.findByIdAndState(districtId, Region.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, what + ": tuman topilmadi"));
        if (regionId != null && !d.getRegion().getId().equals(regionId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, what + ": tuman tanlangan hududga tegishli emas");
        }
        return d;
    }

    private District findActive(Long id) {
        return districts.findByIdAndState(id, Region.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tuman topilmadi: " + id));
    }

    private Region activeRegion(Long id) {
        return regions.findByIdAndState(id, Region.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hudud topilmadi: " + id));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
