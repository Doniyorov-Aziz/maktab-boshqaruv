package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<Position, Long> {
 boolean existsByTitle(String title);

    /** Positions list with a title search (lower(...) like :q, null = all). */
    @org.springframework.data.jpa.repository.Query(
            "select p from Position p where :q is null or lower(p.title) like :q")
    org.springframework.data.domain.Page<Position> search(@org.springframework.data.repository.query.Param("q") String q,
                                                          org.springframework.data.domain.Pageable pageable);
}