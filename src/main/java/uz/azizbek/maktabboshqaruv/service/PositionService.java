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

@Service
public class PositionService {

    @Autowired
    private PositionRepository positionRepository;

    public Page<PositionResponseDto> getAllPositions(Pageable pageable) {
        return positionRepository.findAll(pageable)
                .map(this::toResponseDto);
    }

    public PositionResponseDto getPositionById(Long id) {
        Position position = positionRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday positsiya topilmadi: " + id));
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
                .orElseThrow(() -> new IllegalStateException("Bunday position topilmadi: " + id));

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
            throw new IllegalStateException("Bunday position topilmadi: " + id);
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
