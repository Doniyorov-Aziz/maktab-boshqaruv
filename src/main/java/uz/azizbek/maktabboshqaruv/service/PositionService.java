package uz.azizbek.maktabboshqaruv.service;
import uz.azizbek.maktabboshqaruv.dto.PositionRequestDto;
import uz.azizbek.maktabboshqaruv.dto.PositionResponseDto;
import uz.azizbek.maktabboshqaruv.entity.Position;
import uz.azizbek.maktabboshqaruv.repository.PositionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.service.pages.ListStatsService;
import uz.azizbek.maktabboshqaruv.util.PageSort;

import java.util.List;
import java.util.Map;

@Service
public class PositionService {

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private uz.azizbek.maktabboshqaruv.repository.EmployeeRepository employeeRepository;

    @Autowired
    private ListStatsService listStats;

    public Page<PositionResponseDto> getAllPositions(Pageable pageable) {
        return getAllPositions(null, null, pageable);
    }

    /**
     * Positions with a title search and how many employees each has: in the given school, or in
     * all schools when schoolId is null (positions are shared by the schools).
     */
    public Page<PositionResponseDto> getAllPositions(Long schoolId, String q, Pageable pageable) {
        Page<PositionResponseDto> page = positionRepository.search(PageSort.like(q),
                PageSort.map(pageable, SORTABLE)).map(this::toResponseDto);
        Map<Long, Integer> counts = listStats.positionEmployees(schoolId);
        for (PositionResponseDto dto : page.getContent()) dto.setEmployeeCount(counts.getOrDefault(dto.getId(), 0));
        return page;
    }

    private static final Map<String, List<String>> SORTABLE = Map.of("id", List.of("id"), "title", List.of("title"));

    public PositionResponseDto getPositionById(Long id) {
        Position position = positionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday positsiya topilmadi: " + id));
        return toResponseDto(position);
    }

    @Transactional
    public PositionResponseDto createPosition(PositionRequestDto request) {
        if (positionRepository.existsByTitle(request.getTitle())) {
            throw new IllegalStateException("Bu position allaqachon mavjud");
        }

        Position position = new Position();
        position.setTitle(request.getTitle());

        Position saved = positionRepository.save(position);
        return toResponseDto(saved);
    }

    @Transactional
    public PositionResponseDto updatePosition(Long id, PositionRequestDto request) {
        Position position = positionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday position topilmadi: " + id));

        if (!position.getTitle().equals(request.getTitle()) && positionRepository.existsByTitle(request.getTitle())) {
            throw new IllegalStateException("Bunday position allaqachon mavjud");
        }

        position.setTitle(request.getTitle());

        Position updated = positionRepository.save(position);
        return toResponseDto(updated);
    }

    @Transactional
    public void deletePosition(Long id) {
        if (!positionRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday position topilmadi: " + id);
        }
        // employees keep their position: one that still has them cannot go (409, with how many)
        long employees = employeeRepository.countByPositionId(id);
        if (employees > 0) {
            throw new IllegalStateException("Bu lavozimda " + employees + " ta xodim bor — o'chirib bo'lmaydi. "
                    + "Avval xodimlarni boshqa lavozimga o'tkazing.");
        }
        positionRepository.deleteById(id);
    }

    private PositionResponseDto toResponseDto(Position position) {
        PositionResponseDto dto = new PositionResponseDto();
        dto.setId(position.getId());
        dto.setTitle(position.getTitle());
        return dto;
    }
}
