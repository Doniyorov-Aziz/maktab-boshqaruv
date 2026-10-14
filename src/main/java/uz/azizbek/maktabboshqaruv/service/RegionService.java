package uz.azizbek.maktabboshqaruv.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.dto.RegionRequestDto;
import uz.azizbek.maktabboshqaruv.dto.RegionResponseDto;
import uz.azizbek.maktabboshqaruv.entity.Region;
import uz.azizbek.maktabboshqaruv.repository.RegionRepository;

import java.util.List;

/** Hududlar ma'lumotnomasi: only active ('A') regions are listed or edited; delete is soft ('P'). */
@Service
public class RegionService {

    private final RegionRepository regions;

    public RegionService(RegionRepository regions) {
        this.regions = regions;
    }

    @Transactional(readOnly = true)
    public List<RegionResponseDto> getAll() {
        return regions.findAllByStateOrderByNameUz(Region.ACTIVE).stream().map(RegionResponseDto::from).toList();
    }

    @Transactional(readOnly = true)
    public RegionResponseDto getById(Long id) {
        return RegionResponseDto.from(findActive(id));
    }

    @Transactional
    public RegionResponseDto create(RegionRequestDto request) {
        String code = request.code().trim();
        if (regions.existsByCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu kod allaqachon mavjud: " + code);
        }
        Region r = new Region();
        r.setCode(code);
        r.setNameUz(request.nameUz().trim());
        return RegionResponseDto.from(regions.save(r));
    }

    @Transactional
    public RegionResponseDto update(Long id, RegionRequestDto request) {
        Region r = findActive(id);
        String code = request.code().trim();
        if (!r.getCode().equals(code) && regions.existsByCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu kod allaqachon mavjud: " + code);
        }
        r.setCode(code);
        r.setNameUz(request.nameUz().trim());
        // no save(): the entity is managed, Hibernate writes the change at commit (dirty checking)
        return RegionResponseDto.from(r);
    }

    /** Soft delete: districts and students that point at the region keep a valid reference. */
    @Transactional
    public void delete(Long id) {
        findActive(id).setState(Region.DELETED);
    }

    private Region findActive(Long id) {
        return regions.findByIdAndState(id, Region.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hudud topilmadi: " + id));
    }
}
