package uz.azizbek.maktabboshqaruv.controller;

import uz.azizbek.maktabboshqaruv.dto.SchoolRequestDto;
import uz.azizbek.maktabboshqaruv.dto.SchoolResponseDto;
import uz.azizbek.maktabboshqaruv.service.SchoolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/schools")
public class SchoolController {

    @Autowired
    private SchoolService schoolService;

    @GetMapping
    public List<SchoolResponseDto> getAllSchools() {
        return schoolService.getAllSchools();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SchoolResponseDto> getSchoolById(@PathVariable Long id) {
        return ResponseEntity.ok(schoolService.getSchoolById(id));
    }

    @PostMapping
    public ResponseEntity<SchoolResponseDto> createSchool(@Valid @RequestBody SchoolRequestDto request) {
        SchoolResponseDto created = schoolService.createSchool(request);
        return ResponseEntity.status(201).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SchoolResponseDto> updateSchool(@PathVariable Long id, @Valid @RequestBody SchoolRequestDto request) {
        SchoolResponseDto updated = schoolService.updateSchool(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSchool(@PathVariable Long id) {
        schoolService.deleteSchool(id);
        return ResponseEntity.ok("ID " + id + " bilan maktab muvaffaqiyatli o'chirildi");
    }
}