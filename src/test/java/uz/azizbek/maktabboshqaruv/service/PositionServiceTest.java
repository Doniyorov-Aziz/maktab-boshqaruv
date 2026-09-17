package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.PositionRequestDto;
import uz.azizbek.maktabboshqaruv.dto.PositionResponseDto;
import uz.azizbek.maktabboshqaruv.entity.Position;
import uz.azizbek.maktabboshqaruv.repository.PositionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PositionServiceTest {

    @Mock
    private PositionRepository positionRepository;

    @InjectMocks
    private PositionService positionService;

    @Test
    void createPosition_duplicateTitle_throws() {
        PositionRequestDto request = new PositionRequestDto();
        request.setTitle("Direktor");
        when(positionRepository.existsByTitle("Direktor")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> positionService.createPosition(request));
        verify(positionRepository, never()).save(any());
    }

    @Test
    void createPosition_uniqueTitle_saves() {
        PositionRequestDto request = new PositionRequestDto();
        request.setTitle("Direktor");
        when(positionRepository.existsByTitle("Direktor")).thenReturn(false);

        Position saved = new Position();
        saved.setId(1L);
        saved.setTitle("Direktor");
        when(positionRepository.save(any(Position.class))).thenReturn(saved);

        PositionResponseDto result = positionService.createPosition(request);

        assertEquals("Direktor", result.getTitle());
    }

    @Test
    void updatePosition_notFound_throws() {
        when(positionRepository.findById(1L)).thenReturn(Optional.empty());

        PositionRequestDto request = new PositionRequestDto();
        request.setTitle("Direktor");

        assertThrows(IllegalStateException.class, () -> positionService.updatePosition(1L, request));
    }

    @Test
    void updatePosition_sameTitle_skipsDuplicateCheck() {
        Position existing = new Position();
        existing.setId(1L);
        existing.setTitle("Direktor");
        when(positionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(positionRepository.save(any(Position.class))).thenReturn(existing);

        PositionRequestDto request = new PositionRequestDto();
        request.setTitle("Direktor");

        PositionResponseDto result = positionService.updatePosition(1L, request);

        assertEquals("Direktor", result.getTitle());
        verify(positionRepository, never()).existsByTitle(any());
    }

    @Test
    void updatePosition_changedToDuplicateTitle_throws() {
        Position existing = new Position();
        existing.setId(1L);
        existing.setTitle("Direktor");
        when(positionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(positionRepository.existsByTitle("O'qituvchi")).thenReturn(true);

        PositionRequestDto request = new PositionRequestDto();
        request.setTitle("O'qituvchi");

        assertThrows(IllegalStateException.class, () -> positionService.updatePosition(1L, request));
        verify(positionRepository, never()).save(any());
    }

    @Test
    void deletePosition_notFound_throws() {
        when(positionRepository.existsById(1L)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> positionService.deletePosition(1L));
    }
}
