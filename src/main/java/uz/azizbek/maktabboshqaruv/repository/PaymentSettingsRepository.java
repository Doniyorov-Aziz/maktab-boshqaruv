package uz.azizbek.maktabboshqaruv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.azizbek.maktabboshqaruv.entity.PaymentSettings;

import java.util.List;
import java.util.Optional;

public interface PaymentSettingsRepository extends JpaRepository<PaymentSettings, Long> {

    Optional<PaymentSettings> findBySchoolId(Long schoolId);

    List<PaymentSettings> findByEnabledTrue();
}
