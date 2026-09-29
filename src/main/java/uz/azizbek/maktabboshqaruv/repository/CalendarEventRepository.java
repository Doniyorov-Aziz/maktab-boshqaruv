package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.CalendarEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    Page<CalendarEvent> findBySchoolId(Long schoolId, Pageable pageable);

    @Query("select e from CalendarEvent e where e.school.id = :schoolId and e.startDate <= :to and e.endDate >= :from order by e.startDate")
    List<CalendarEvent> findInRange(@Param("schoolId") Long schoolId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select e from CalendarEvent e where e.school.id = :schoolId and e.startDate >= :from order by e.startDate")
    List<CalendarEvent> findUpcoming(@Param("schoolId") Long schoolId, @Param("from") LocalDate from, Pageable pageable);
}
