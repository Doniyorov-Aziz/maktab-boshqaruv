package uz.azizbek.maktabboshqaruv.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.azizbek.maktabboshqaruv.entity.District;

import java.util.List;
import java.util.Optional;

public interface DistrictRepository extends JpaRepository<District, Long> {

    @EntityGraph(attributePaths = "region")
    List<District> findAllByRegionIdAndStateOrderByNameUz(Long regionId, String state);

    /** every active district with its region, for the pickers (≈200 rows) */
    @EntityGraph(attributePaths = "region")
    List<District> findAllByStateAndRegionStateOrderByRegionNameUzAscNameUzAsc(String state, String regionState);

    @EntityGraph(attributePaths = "region")
    Optional<District> findByIdAndState(Long id, String state);

    boolean existsByRegionIdAndNameUzIgnoreCase(Long regionId, String nameUz);

    boolean existsByRegionId(Long regionId);

    boolean existsByCode(String code);
}
