package uz.azizbek.maktabboshqaruv.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.azizbek.maktabboshqaruv.dto.DistrictResponseDto;
import uz.azizbek.maktabboshqaruv.dto.RegionRequestDto;
import uz.azizbek.maktabboshqaruv.dto.RegionResponseDto;
import uz.azizbek.maktabboshqaruv.service.DistrictService;
import uz.azizbek.maktabboshqaruv.service.RegionService;

import java.util.List;

@RestController
@RequestMapping("/api/regions")
public class RegionController {

    private final RegionService regionService;
    private final DistrictService districtService;

    public RegionController(RegionService regionService, DistrictService districtService) {
        this.regionService = regionService;
        this.districtService = districtService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping
    public List<RegionResponseDto> getAll() {
        return regionService.getAll();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/{id}")
    public RegionResponseDto getById(@PathVariable Long id) {
        return regionService.getById(id);
    }

    /** the region's active districts — what the district picker shows once a region is chosen */
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/{id}/districts")
    public List<DistrictResponseDto> getDistricts(@PathVariable Long id) {
        return districtService.getByRegion(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegionResponseDto create(@Valid @RequestBody RegionRequestDto request) {
        return regionService.create(request);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PutMapping("/{id}")
    public RegionResponseDto update(@PathVariable Long id, @Valid @RequestBody RegionRequestDto request) {
        return regionService.update(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        regionService.delete(id);
    }
}
