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

    boolean existsBySubjectId(Long subjectId);

    /** A class's whole week with subject, teacher and room in one query (the bot's cache loader). */
    @Query("select distinct l from LessonSlot l join fetch l.subject join fetch l.employee e join fetch e.position " +
            "left join fetch l.room r left join fetch r.building where l.schoolClass.id = :classId")
    List<LessonSlot> findWeekOfClass(@Param("classId") Long classId);

    /** A renamed subject: today's timetable moves to the new subject row; grade history stays on the old one. */
    @org.springframework.data.jpa.repository.Modifying
    @Query("update LessonSlot l set l.subject.id = :to where l.subject.id = :from")
    int moveSubject(@Param("from") Long from, @Param("to") Long to);

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

    /**
     * A class's week, columns only (no entities → no eager loads): [weekday, startTime, endTime,
     * subjectId, subjectName, subjectActive, teacherId, teacherLastName, teacherFirstName, teacherPhone].
     */
    @Query("select l.weekday, l.startTime, l.endTime, sub.id, sub.name, sub.active, e.id, e.lastName, e.firstName, e.phone " +
            "from LessonSlot l join l.subject sub join l.employee e where l.schoolClass.id = :classId")
    List<Object[]> weekOfClass(@Param("classId") Long classId);

    /**
     * A teacher's lessons on one weekday, columns only: [startTime, endTime, classId, gradeNumber,
     * sectionLetter, subjectId, subjectName] (the teacher's "current lesson").
     */
    @Query("select l.startTime, l.endTime, c.id, c.gradeNumber, c.sectionLetter, sub.id, sub.name " +
            "from LessonSlot l join l.schoolClass c join l.subject sub " +
            "where l.employee.id = :employeeId and l.weekday = :weekday order by l.startTime")
    List<Object[]> ofTeacherOnDay(@Param("employeeId") Long employeeId, @Param("weekday") String weekday);

    /** The class's lesson start times, in order: their position is the lesson number ("3-dars"). */
    @Query("select distinct l.startTime from LessonSlot l where l.schoolClass.id = :classId order by l.startTime")
    List<java.time.LocalTime> startTimesOfClass(@Param("classId") Long classId);
}
