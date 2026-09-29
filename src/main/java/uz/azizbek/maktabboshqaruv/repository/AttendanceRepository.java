package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    @Query("select a from Attendance a where a.lessonSlot.schoolClass.academicYear.school.id = :schoolId")
    Page<Attendance> findBySchoolId(@Param("schoolId") Long schoolId, Pageable pageable);

    @Query("select count(a) from Attendance a where a.lessonSlot.schoolClass.academicYear.school.id = :schoolId")
    long countBySchoolId(@Param("schoolId") Long schoolId);

    List<Attendance> findByLessonSlotIdAndRecordDate(Long lessonSlotId, LocalDate recordDate);

    List<Attendance> findByStudentIdAndRecordDateBetweenOrderByRecordDate(Long studentId, LocalDate from, LocalDate to);

    boolean existsByLessonSlotIdAndStudentIdAndRecordDate(Long lessonSlotId, Long studentId, LocalDate recordDate);

    boolean existsByLessonSlotIdAndRecordDate(Long lessonSlotId, LocalDate recordDate);

    @Query("select count(a) from Attendance a where a.lessonSlot.schoolClass.academicYear.school.id = :schoolId and a.recordDate = :date")
    long countBySchoolIdAndRecordDate(@Param("schoolId") Long schoolId, @Param("date") LocalDate date);

    @Query("select count(a) from Attendance a where a.lessonSlot.schoolClass.academicYear.school.id = :schoolId and a.recordDate = :date and a.status = uz.azizbek.maktabboshqaruv.entity.AttendanceStatus.ABSENT")
    long countAbsentBySchoolIdAndRecordDate(@Param("schoolId") Long schoolId, @Param("date") LocalDate date);

    @Query("select count(a) from Attendance a where a.lessonSlot.schoolClass.academicYear.school.id = :schoolId and a.recordDate = :date and a.status in (uz.azizbek.maktabboshqaruv.entity.AttendanceStatus.PRESENT, uz.azizbek.maktabboshqaruv.entity.AttendanceStatus.LATE)")
    long countPresentBySchoolIdAndRecordDate(@Param("schoolId") Long schoolId, @Param("date") LocalDate date);

    @Query("select a.recordDate as recordDate, " +
            "sum(case when a.status in (uz.azizbek.maktabboshqaruv.entity.AttendanceStatus.PRESENT, uz.azizbek.maktabboshqaruv.entity.AttendanceStatus.LATE) then 1 else 0 end) as present, " +
            "count(a) as total " +
            "from Attendance a " +
            "where a.lessonSlot.schoolClass.academicYear.school.id = :schoolId and a.recordDate between :from and :to " +
            "group by a.recordDate order by a.recordDate")
    List<Object[]> dailyAttendanceTrend(@Param("schoolId") Long schoolId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select sc.id as classId, " +
            "sum(case when a.status in (uz.azizbek.maktabboshqaruv.entity.AttendanceStatus.PRESENT, uz.azizbek.maktabboshqaruv.entity.AttendanceStatus.LATE) then 1 else 0 end) as present, " +
            "count(a) as total " +
            "from Attendance a join a.lessonSlot.schoolClass sc " +
            "where sc.academicYear.school.id = :schoolId and a.recordDate between :from and :to " +
            "group by sc.id")
    List<Object[]> attendanceRateByClass(@Param("schoolId") Long schoolId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select a.student.id as studentId, a.recordDate as recordDate, " +
            "sum(case when a.status = uz.azizbek.maktabboshqaruv.entity.AttendanceStatus.ABSENT then 1 else 0 end) as absentCount, " +
            "count(a) as total " +
            "from Attendance a " +
            "where a.lessonSlot.schoolClass.academicYear.school.id = :schoolId and a.recordDate between :from and :to " +
            "group by a.student.id, a.recordDate order by a.student.id, a.recordDate")
    List<Object[]> studentDailyAbsence(@Param("schoolId") Long schoolId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select a from Attendance a where a.lessonSlot.schoolClass.academicYear.school.id = :schoolId " +
            "and a.recordDate = :date and a.status = uz.azizbek.maktabboshqaruv.entity.AttendanceStatus.ABSENT " +
            "order by a.lessonSlot.schoolClass.gradeNumber, a.lessonSlot.schoolClass.sectionLetter, a.student.lastName")
    List<Attendance> findAbsentBySchoolIdAndRecordDate(@Param("schoolId") Long schoolId, @Param("date") LocalDate date);

    @Query("select count(distinct a.lessonSlot.schoolClass.id) from Attendance a " +
            "where a.lessonSlot.schoolClass.academicYear.school.id = :schoolId and a.recordDate = :date")
    long countDistinctClassesBySchoolIdAndRecordDate(@Param("schoolId") Long schoolId, @Param("date") LocalDate date);

    @Query("select " +
            "sum(case when a.status in (uz.azizbek.maktabboshqaruv.entity.AttendanceStatus.PRESENT, uz.azizbek.maktabboshqaruv.entity.AttendanceStatus.LATE) then 1 else 0 end) as present, " +
            "count(a) as total " +
            "from Attendance a " +
            "where a.lessonSlot.schoolClass.academicYear.school.id = :schoolId and a.recordDate = :date and a.createdDate <= :cutoff")
    List<Object[]> attendanceRateAsOf(@Param("schoolId") Long schoolId, @Param("date") LocalDate date, @Param("cutoff") LocalDateTime cutoff);

}
