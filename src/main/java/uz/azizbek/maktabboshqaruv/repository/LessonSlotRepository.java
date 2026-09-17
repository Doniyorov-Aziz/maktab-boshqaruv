package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.LessonSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;

public interface LessonSlotRepository extends JpaRepository<LessonSlot, Long> {

    @Query("select count(l) > 0 from LessonSlot l where l.room.id = :roomId and l.weekday = :weekday " +
            "and l.startTime < :endTime and l.endTime > :startTime and (:excludeId is null or l.id <> :excludeId)")
    boolean existsRoomConflict(@Param("roomId") Long roomId,
                                @Param("weekday") String weekday,
                                @Param("startTime") LocalTime startTime,
                                @Param("endTime") LocalTime endTime,
                                @Param("excludeId") Long excludeId);

    @Query("select count(l) > 0 from LessonSlot l where l.employee.id = :employeeId and l.weekday = :weekday " +
            "and l.startTime < :endTime and l.endTime > :startTime and (:excludeId is null or l.id <> :excludeId)")
    boolean existsEmployeeConflict(@Param("employeeId") Long employeeId,
                                    @Param("weekday") String weekday,
                                    @Param("startTime") LocalTime startTime,
                                    @Param("endTime") LocalTime endTime,
                                    @Param("excludeId") Long excludeId);

    @Query("select count(l) > 0 from LessonSlot l where l.schoolClass.id = :schoolClassId and l.weekday = :weekday " +
            "and l.startTime < :endTime and l.endTime > :startTime and (:excludeId is null or l.id <> :excludeId)")
    boolean existsSchoolClassConflict(@Param("schoolClassId") Long schoolClassId,
                                       @Param("weekday") String weekday,
                                       @Param("startTime") LocalTime startTime,
                                       @Param("endTime") LocalTime endTime,
                                       @Param("excludeId") Long excludeId);
}
