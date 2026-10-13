package uz.azizbek.maktabboshqaruv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.azizbek.maktabboshqaruv.entity.LibraryPermission;

import java.time.LocalDate;
import java.util.List;

public interface LibraryPermissionRepository extends JpaRepository<LibraryPermission, Long> {

    List<LibraryPermission> findBySchoolIdOrderByCreatedAtDesc(Long schoolId);

    /** Ended (revoked, or past the end date) but still handing out active books — the daily job's work list. */
    @Query("select distinct p from LibraryPermission p join Book b on b.permission = p where b.active = true"
            + " and (p.revoked = true or (p.validUntil is not null and p.validUntil < :today))")
    List<LibraryPermission> endedWithActiveBooks(@Param("today") LocalDate today);

    /** In force but ending within the window and not warned about yet. */
    @Query("select p from LibraryPermission p where p.revoked = false and p.validUntil is not null"
            + " and p.validUntil >= :today and p.validUntil <= :until and p.expiryWarnedAt is null")
    List<LibraryPermission> endingSoonUnwarned(@Param("today") LocalDate today, @Param("until") LocalDate until);

    /** For the attention list: ended or ending soon, of one school. */
    @Query("select p from LibraryPermission p where p.school.id = :schoolId and p.revoked = false"
            + " and p.validUntil is not null and p.validUntil <= :until order by p.validUntil")
    List<LibraryPermission> endingBy(@Param("schoolId") Long schoolId, @Param("until") LocalDate until);
}
