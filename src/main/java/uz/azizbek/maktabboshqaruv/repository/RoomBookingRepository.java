package uz.azizbek.maktabboshqaruv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.azizbek.maktabboshqaruv.entity.RoomBooking;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface RoomBookingRepository extends JpaRepository<RoomBooking, Long> {

    @Query("select b from RoomBooking b join fetch b.room r join fetch r.building where b.school.id = :schoolId "
            + "and b.date between :from and :to order by b.date, b.startTime")
    List<RoomBooking> findInRange(@Param("schoolId") Long schoolId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Bookings of the room that overlap [start, end) on the date, except those of one event. */
    @Query("select b from RoomBooking b where b.room.id = :roomId and b.date = :date and b.startTime < :end "
            + "and b.endTime > :start and (:exceptEvent is null or b.calendarEventId is null or b.calendarEventId <> :exceptEvent) "
            + "and (:exceptId is null or b.id <> :exceptId)")
    List<RoomBooking> overlapping(@Param("roomId") Long roomId, @Param("date") LocalDate date, @Param("start") LocalTime start,
                                  @Param("end") LocalTime end, @Param("exceptEvent") Long exceptEvent,
                                  @Param("exceptId") Long exceptId);

    List<RoomBooking> findByCalendarEventId(Long calendarEventId);

    @Modifying
    @Query("delete from RoomBooking b where b.calendarEventId = :eventId")
    void deleteByCalendarEventId(@Param("eventId") Long eventId);
}
