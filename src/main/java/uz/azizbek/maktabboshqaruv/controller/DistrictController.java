package uz.azizbek.maktabboshqaruv.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.azizbek.maktabboshqaruv.dto.DistrictRequestDto;
import uz.azizbek.maktabboshqaruv.dto.DistrictResponseDto;
import uz.azizbek.maktabboshqaruv.service.DistrictService;

import java.util.List;

@RestController
@RequestMapping("/api/districts")
public class DistrictController {

    private final DistrictService districtService;

    public DistrictController(DistrictService districtService) {
        this.districtService = districtService;
    }

    /** every active district with its region — the region → district pickers filter it on the page */
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping
    public List<DistrictResponseDto> getAll() {
        return districtService.getAll();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/{id}")
    public DistrictResponseDto getById(@PathVariable Long id) {
        return districtService.getById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DistrictResponseDto create(@Valid @RequestBody DistrictRequestDto request) {
        return districtService.create(request);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PutMapping("/{id}")
    public DistrictResponseDto update(@PathVariable Long id, @Valid @RequestBody DistrictRequestDto request) {
        return districtService.update(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        districtService.delete(id);
    }
}
