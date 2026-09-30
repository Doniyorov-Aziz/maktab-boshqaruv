package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.ParentTelegramLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ParentTelegramLinkRepository extends JpaRepository<ParentTelegramLink, Long> {

    Optional<ParentTelegramLink> findByStudentIdAndChatId(Long studentId, Long chatId);

    List<ParentTelegramLink> findByStudentIdAndActiveTrueOrderByLinkedAtAsc(Long studentId);

    List<ParentTelegramLink> findByChatIdAndActiveTrue(Long chatId);

    @Query("select l from ParentTelegramLink l where l.active = true " +
            "and l.student.schoolClass.academicYear.school.id = :schoolId order by l.chatId, l.id")
    List<ParentTelegramLink> findActiveBySchoolId(@Param("schoolId") Long schoolId);

    @Query("select l from ParentTelegramLink l where l.active = true " +
            "and l.student.schoolClass.id = :classId order by l.chatId, l.id")
    List<ParentTelegramLink> findActiveByClassId(@Param("classId") Long classId);

    @Query("select l.student.id, count(l) from ParentTelegramLink l where l.active = true " +
            "and l.student.schoolClass.id = :classId group by l.student.id")
    List<Object[]> countActiveByStudentInClass(@Param("classId") Long classId);

    @Query("select count(distinct l.student.id) from ParentTelegramLink l where l.active = true " +
            "and l.student.schoolClass.academicYear.school.id = :schoolId")
    long countLinkedStudentsBySchoolId(@Param("schoolId") Long schoolId);

    @Query("select count(distinct l.chatId) from ParentTelegramLink l where l.active = true " +
            "and l.student.schoolClass.academicYear.school.id = :schoolId")
    long countParentsBySchoolId(@Param("schoolId") Long schoolId);

    @Transactional
    @Modifying
    @Query("update ParentTelegramLink l set l.active = false where l.chatId = :chatId and l.active = true")
    int deactivateByChatId(@Param("chatId") Long chatId);
}
