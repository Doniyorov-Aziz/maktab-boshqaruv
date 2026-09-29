package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByBuildingId(Long buildingId);

    @Query("select r from Room r where r.building.school.id = :schoolId " +
            "and (:buildingId is null or r.building.id = :buildingId) " +
            "and (:type is null or r.type = :type)")
    Page<Room> findFiltered(@Param("schoolId") Long schoolId, @Param("buildingId") Long buildingId,
                             @Param("type") uz.azizbek.maktabboshqaruv.entity.RoomType type, Pageable pageable);
}
