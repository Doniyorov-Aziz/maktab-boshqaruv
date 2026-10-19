package uz.azizbek.maktabboshqaruv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.azizbek.maktabboshqaruv.entity.CalendarFeedToken;

import java.util.List;
import java.util.Optional;

public interface CalendarFeedTokenRepository extends JpaRepository<CalendarFeedToken, Long> {

    Optional<CalendarFeedToken> findByTokenAndActiveTrue(String token);

    List<CalendarFeedToken> findBySchoolIdAndActiveTrueOrderByCreatedAtDesc(Long schoolId);
}
