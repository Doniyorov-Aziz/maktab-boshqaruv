package uz.azizbek.maktabboshqaruv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.azizbek.maktabboshqaruv.entity.MeetingSlot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface MeetingSlotRepository extends JpaRepository<MeetingSlot, Long> {

    @Query("select m from MeetingSlot m join fetch m.employee e left join fetch m.student s left join fetch s.schoolClass " +
            "where m.school.id = :schoolId and (:employeeId is null or e.id = :employeeId) " +
            "and m.date between :from and :to order by m.date, m.startTime, e.lastName")
    List<MeetingSlot> search(@Param("schoolId") Long schoolId, @Param("employeeId") Long employeeId,
                             @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select m from MeetingSlot m where m.employee.id = :employeeId and m.date = :date order by m.startTime")
    List<MeetingSlot> ofTeacherOn(@Param("employeeId") Long employeeId, @Param("date") LocalDate date);

    /** Free future times of these teachers (the parents' bot). */
    @Query("select m from MeetingSlot m join fetch m.employee where m.employee.id in :teacherIds and m.chatId is null " +
            "and m.date between :from and :to order by m.date, m.startTime")
    List<MeetingSlot> freeOf(@Param("teacherIds") Collection<Long> teacherIds, @Param("from") LocalDate from,
                             @Param("to") LocalDate to);

    /** This parent's bookings from a day on. */
    @Query("select m from MeetingSlot m join fetch m.employee left join fetch m.student where m.chatId = :chatId " +
            "and m.date >= :from order by m.date, m.startTime")
    List<MeetingSlot> bookedBy(@Param("chatId") Long chatId, @Param("from") LocalDate from);

    @Query("select m from MeetingSlot m join fetch m.employee join fetch m.student s join fetch s.schoolClass c " +
            "join fetch m.school where m.date = :date and m.chatId is not null")
    List<MeetingSlot> bookedOn(@Param("date") LocalDate date);

    /** Takes a free slot; 0 when someone else was faster (or it was removed). */
    @Modifying
    @Query("update MeetingSlot m set m.chatId = :chatId, m.student = :student, m.parentName = :name, " +
            "m.parentPhone = :phone, m.topic = :topic, m.bookedAt = :at where m.id = :id and m.chatId is null")
    int book(@Param("id") Long id, @Param("chatId") Long chatId, @Param("student") uz.azizbek.maktabboshqaruv.entity.Student student,
             @Param("name") String name, @Param("phone") String phone, @Param("topic") String topic,
             @Param("at") LocalDateTime at);
}
