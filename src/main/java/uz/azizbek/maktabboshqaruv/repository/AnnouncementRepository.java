package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    Page<Announcement> findBySchoolId(Long schoolId, Pageable pageable);

    @Query("select a from Announcement a where a.school.id = :schoolId order by a.createdDate desc")
    List<Announcement> findLatestBySchoolId(@Param("schoolId") Long schoolId, Pageable pageable);

    /** What a parent of a pupil in {@code classId} may see: school-wide ones plus their own class's. */
    @Query("select a from Announcement a where a.school.id = :schoolId and " +
            "(a.audience = uz.azizbek.maktabboshqaruv.entity.AnnouncementAudience.ALL or " +
            "(a.audience = uz.azizbek.maktabboshqaruv.entity.AnnouncementAudience.CLASS and a.schoolClass.id = :classId)) " +
            "order by a.createdDate desc, a.id desc")
    List<Announcement> findForParents(@Param("schoolId") Long schoolId, @Param("classId") Long classId);
}
