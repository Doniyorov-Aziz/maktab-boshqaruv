package uz.azizbek.maktabboshqaruv.controller;

import uz.azizbek.maktabboshqaruv.dto.PositionRequestDto;
import uz.azizbek.maktabboshqaruv.dto.PositionResponseDto;
import uz.azizbek.maktabboshqaruv.service.PositionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/positions")
public class PositionController {

    @Autowired
    private PositionService positionService;

    @GetMapping
    public List<PositionResponseDto> getAllPositions() {
        return positionService.getAllPositions();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PositionResponseDto> getPositionById(@PathVariable Long id) {
        return ResponseEntity.ok(positionService.getPositionById(id));
    }

    @PostMapping
    public ResponseEntity<PositionResponseDto> createPosition(@Valid @RequestBody PositionRequestDto request) {
        PositionResponseDto created = positionService.createPosition(request);
        return ResponseEntity.status(201).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PositionResponseDto> updatePosition(@PathVariable Long id, @Valid @RequestBody PositionRequestDto request) {
        PositionResponseDto updated = positionService.updatePosition(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePosition(@PathVariable Long id) {
        positionService.deletePosition(id);
        return ResponseEntity.ok("ID " + id + " bilan position muvaffaqiyatli o'chirildi");
    }
}