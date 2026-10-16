package uz.azizbek.maktabboshqaruv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.azizbek.maktabboshqaruv.entity.SchoolSettings;

import java.util.Optional;

public interface SchoolSettingsRepository extends JpaRepository<SchoolSettings, Long> {

    Optional<SchoolSettings> findBySchoolId(Long schoolId);
}
