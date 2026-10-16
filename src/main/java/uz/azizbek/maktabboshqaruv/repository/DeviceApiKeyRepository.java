package uz.azizbek.maktabboshqaruv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.azizbek.maktabboshqaruv.entity.DeviceApiKey;

import java.util.List;
import java.util.Optional;

public interface DeviceApiKeyRepository extends JpaRepository<DeviceApiKey, Long> {

    Optional<DeviceApiKey> findByKeyHashAndRevokedAtIsNull(String keyHash);

    List<DeviceApiKey> findBySchoolIdOrderByCreatedAtDesc(Long schoolId);
}
