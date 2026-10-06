package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.LessonSlot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;

public interface LessonSlotRepository extends JpaRepository<LessonSlot, Long> {

    Page<LessonSlot> findBySchoolClassAcademicYearSchoolId(Long schoolId, Pageable pageable);
    long countBySchoolClassAcademicYearSchoolId(Long schoolId);
    List<LessonSlot> findBySchoolClassAcademicYearSchoolId(Long schoolId);
    List<LessonSlot> findBySchoolClassId(Long schoolClassId);
    List<LessonSlot> findByEmployeeId(Long employeeId);

    /** [employeeId, subjectName, gradeNumber, sectionLetter] — what each listed employee teaches, one query. */
    @Query("select distinct l.employee.id, l.subject.name, l.schoolClass.gradeNumber, l.schoolClass.sectionLetter " +
            "from LessonSlot l where l.employee.id in :employeeIds")
    List<Object[]> teachingOf(@Param("employeeIds") java.util.Collection<Long> employeeIds);
    List<LessonSlot> findByRoomId(Long roomId);
    long countByEmployeeId(Long employeeId);

    @Query("select l from LessonSlot l where l.schoolClass.academicYear.school.id = :schoolId " +
            "and l.weekday = :weekday and l.startTime <= :time and l.endTime > :time order by l.schoolClass.gradeNumber, l.schoolClass.sectionLetter")
    List<LessonSlot> findCurrentlyInSession(@Param("schoolId") Long schoolId, @Param("weekday") String weekday, @Param("time") LocalTime time);

    @Query("select l from LessonSlot l where l.schoolClass.academicYear.school.id = :schoolId " +
            "and l.weekday = :weekday and l.startTime > :time order by l.startTime")
    List<LessonSlot> findUpcomingToday(@Param("schoolId") Long schoolId, @Param("weekday") String weekday, @Param("time") LocalTime time);

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

    @Query("select l from LessonSlot l where l.schoolClass.academicYear.school.id = :schoolId " +
            "and l.weekday = :weekday order by l.schoolClass.gradeNumber, l.schoolClass.sectionLetter, l.startTime")
    List<LessonSlot> findBySchoolIdAndWeekday(@Param("schoolId") Long schoolId, @Param("weekday") String weekday);

    @Query("select l from LessonSlot l where l.room.id = :roomId " +
            "and l.weekday = :weekday and l.startTime <= :time and l.endTime > :time")
    java.util.Optional<LessonSlot> findCurrentlyInSessionByRoom(@Param("roomId") Long roomId, @Param("weekday") String weekday, @Param("time") LocalTime time);

    @Query("select l from LessonSlot l where l.room.id = :roomId and l.weekday = :weekday order by l.startTime")
    List<LessonSlot> findByRoomIdAndWeekday(@Param("roomId") Long roomId, @Param("weekday") String weekday);

    @Query("select distinct l.startTime, l.endTime from LessonSlot l where l.schoolClass.academicYear.school.id = :schoolId " +
            "and l.weekday = :weekday order by l.startTime")
    List<Object[]> findDistinctPeriodsForWeekday(@Param("schoolId") Long schoolId, @Param("weekday") String weekday);

    @Query("select count(l) > 0 from LessonSlot l where l.schoolClass.id = :schoolClassId and l.weekday = :weekday " +
            "and l.startTime < :endTime and l.endTime > :startTime and (:excludeId is null or l.id <> :excludeId)")
    boolean existsSchoolClassConflict(@Param("schoolClassId") Long schoolClassId,
                                       @Param("weekday") String weekday,
                                       @Param("startTime") LocalTime startTime,
                                       @Param("endTime") LocalTime endTime,
                                       @Param("excludeId") Long excludeId);
}
