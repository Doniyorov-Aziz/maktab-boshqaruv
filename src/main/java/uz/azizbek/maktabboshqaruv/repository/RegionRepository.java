package uz.azizbek.maktabboshqaruv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.azizbek.maktabboshqaruv.entity.Region;

import java.util.List;
import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Long> {

    List<Region> findAllByStateOrderByNameUz(String state);

    Optional<Region> findByIdAndState(Long id, String state);

    /** Deleted regions keep their code too — the code stays unique across the whole table. */
    boolean existsByCode(String code);
}
