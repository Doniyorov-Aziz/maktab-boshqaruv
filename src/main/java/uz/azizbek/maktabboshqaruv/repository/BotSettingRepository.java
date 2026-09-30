package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.BotSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BotSettingRepository extends JpaRepository<BotSetting, Long> {
    Optional<BotSetting> findBySchoolId(Long schoolId);
}
