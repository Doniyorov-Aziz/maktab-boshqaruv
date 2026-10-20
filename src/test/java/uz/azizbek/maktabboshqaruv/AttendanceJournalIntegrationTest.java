package uz.azizbek.maktabboshqaruv;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.RecordApplicationEvents;
import uz.azizbek.maktabboshqaruv.entity.AttendanceStatus;
import uz.azizbek.maktabboshqaruv.entity.LessonSlot;
import uz.azizbek.maktabboshqaruv.service.pages.AttendancePageService;
import uz.azizbek.maktabboshqaruv.service.pages.JournalPageService;

import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * "Davomat olish" and "Baholar jurnali" in a school of their own (Monday 2031-03-03): a date shows
 * only its own weekday's lessons, a save refuses another weekday / the future / Sunday and keeps
 * the minutes late, only the sent rows change (automatic ones stay), the roster's history and
 * warnings, and the journal's summary, day attendance, student card and exports.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "telegram.mock=true",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000",
        "app.demo.enabled=false"
})
@ActiveProfiles("local")
@Import(AttendanceAutomationTestBase.FakeClock.class)
@RecordApplicationEvents
class AttendanceJournalIntegrationTest extends AttendanceAutomationTestBase {

    static final LocalDate WEDNESDAY = MONDAY.plusDays(2);

    @Autowired
    AttendancePageService page;
    @Autowired
    JournalPageService journalPage;

    LessonSlot wednesdayLesson() {
        LessonSlot ls = new LessonSlot();
        ls.setSchoolClass(school.classes.get(0));
        ls.setSubject(school.subject);
        ls.setEmployee(school.teacher);
        ls.setRoom(school.room);
        ls.setWeekday("Chorshanba");
        ls.setStartTime(LocalTime.of(8, 30));
        ls.setEndTime(LocalTime.of(9, 15));
        return lessons.save(ls);
    }

    @Test
    void weekdayMapping_everyDayOfTheWeek() {
        assertEquals("Dushanba", AttendancePageService.weekdayOf(MONDAY));
        assertEquals("Seshanba", AttendancePageService.weekdayOf(MONDAY.plusDays(1)));
        assertEquals("Chorshanba", AttendancePageService.weekdayOf(MONDAY.plusDays(2)));
        assertEquals("Payshanba", AttendancePageService.weekdayOf(MONDAY.plusDays(3)));
        assertEquals("Juma", AttendancePageService.weekdayOf(MONDAY.plusDays(4)));
        assertEquals("Shanba", AttendancePageService.weekdayOf(MONDAY.plusDays(5)));
        assertNull(AttendancePageService.weekdayOf(MONDAY.plusDays(6)), "Sunday has no lessons");
        assertEquals(DayOfWeek.MONDAY, MONDAY.getDayOfWeek());
    }

    @Test
    void aDate_showsOnlyItsOwnWeekdaysLessons() {
        LessonSlot wed = wednesdayLesson();
        long classId = school.classes.get(0).getId();

        AttendancePageService.Day monday = page.day(school.id(), classId, MONDAY);
        assertEquals("Dushanba", monday.weekday());
        assertEquals(3, monday.lessons().size(), "the three Monday lessons");
        assertTrue(monday.lessons().stream().noneMatch(l -> l.lessonSlotId() == wed.getId()),
                "Wednesday's lesson is not on Monday");
        assertEquals(List.of(1, 2, 3), monday.lessons().stream().map(AttendancePageService.Lesson::n).toList());

        AttendancePageService.Day wednesday = page.day(school.id(), classId, WEDNESDAY);
        assertEquals(1, wednesday.lessons().size());
        assertEquals(wed.getId(), wednesday.lessons().get(0).lessonSlotId());

        AttendancePageService.Day sunday = page.day(school.id(), classId, MONDAY.minusDays(1));
        assertFalse(sunday.schoolDay());
        assertEquals("Yakshanba", sunday.dayOff());
        assertTrue(sunday.lessons().isEmpty());
    }

    @Test
    void save_refusesAnotherWeekday_theFuture_andSunday() {
        LessonSlot mondayLesson = lesson(0, 0);
        long sid = student(0, 0).getId();
        var entry = List.of(new AttendancePageService.Entry(sid, AttendanceStatus.ABSENT, null, null));
        assertThrows(IllegalStateException.class, () -> page.save(school.id(),
                new AttendancePageService.SaveRequest(mondayLesson.getId(), WEDNESDAY, entry)), "a Monday lesson on Wednesday");
        assertThrows(IllegalStateException.class, () -> page.save(school.id(),
                new AttendancePageService.SaveRequest(mondayLesson.getId(), MONDAY.plusWeeks(1), entry)), "next Monday is the future");
        assertThrows(IllegalStateException.class, () -> page.save(school.id(),
                new AttendancePageService.SaveRequest(mondayLesson.getId(), MONDAY.minusDays(1), entry)), "Sunday");
    }

    @Test
    void save_writesOnlyTheSentRows_keepsMinutesLate_andTheRosterTellsTheHistory() {
        at(9, 0);
        LessonSlot first = lesson(0, 0);
        // an automatic mark that the save must leave alone
        jdbc.update("insert into attendance (lesson_slot_id, student_id, record_date, status, source, created_date) values (?, ?, ?, 'PRESENT', 'AUTO_CARRY', now())",
                first.getId(), student(0, 5).getId(), Date.valueOf(MONDAY));
        // student 1 was absent the three school days before (Thursday, Friday, Saturday)
        for (LocalDate d : List.of(MONDAY.minusDays(4), MONDAY.minusDays(3), MONDAY.minusDays(2))) {
            jdbc.update("insert into attendance (lesson_slot_id, student_id, record_date, status, source, created_date) values (?, ?, ?, 'ABSENT', 'MANUAL', now())",
                    first.getId(), student(0, 1).getId(), Date.valueOf(d));
        }

        AttendancePageService.Saved saved = page.save(school.id(), new AttendancePageService.SaveRequest(first.getId(), MONDAY, List.of(
                new AttendancePageService.Entry(student(0, 0).getId(), AttendanceStatus.LATE, null, 10),
                new AttendancePageService.Entry(student(0, 1).getId(), AttendanceStatus.ABSENT, null, null))));
        assertEquals(2, saved.saved());
        assertArrayEquals(new String[]{"LATE", "MANUAL"}, mark(0, 0, 0));
        assertEquals(10, jdbc.queryForObject("select late_minutes from attendance where lesson_slot_id = ? and student_id = ? and record_date = ?",
                Integer.class, first.getId(), student(0, 0).getId(), Date.valueOf(MONDAY)));
        assertArrayEquals(new String[]{"PRESENT", "AUTO_CARRY"}, mark(0, 0, 5), "the automatic mark was not sent: unchanged");

        AttendancePageService.Roster r = page.roster(first.getId(), MONDAY, false);
        assertEquals(6, r.rows().size());
        AttendancePageService.Row absentOne = r.rows().stream().filter(x -> x.studentId() == student(0, 1).getId()).findFirst().orElseThrow();
        assertEquals(3, absentOne.absentStreak(), "three school days in a row (Sunday is no break)");
        assertEquals(AttendancePageService.HISTORY_DAYS, absentOne.history().size());
        assertEquals("ABSENT", absentOne.history().get(absentOne.history().size() - 1).status());
        AttendancePageService.Row late = r.rows().stream().filter(x -> x.studentId() == student(0, 0).getId()).findFirst().orElseThrow();
        assertEquals(10, late.lateMinutes());
        assertTrue(r.rows().stream().allMatch(x -> x.guardianPhone() == null), "no phones without the right");

        // the picker's dot: at 09:00 only the first lesson has started; 3 of 6 marked (half) counts as taken
        AttendancePageService.Day day = page.day(school.id(), school.classes.get(0).getId(), MONDAY);
        assertEquals("FULL", day.classStates().get(school.classes.get(0).getId()));
        assertEquals("NONE", day.classStates().get(school.classes.get(1).getId()), "class 1 has nothing yet");
        assertEquals("PARTIAL", day.lessons().get(0).state(), "two manual marks of six");
    }

    @Test
    void journal_summary_dayAttendance_studentCard_andExports() {
        long classId = school.classes.get(0).getId();
        long subjectId = school.subject.getId();
        LocalDate lastMonth = MONDAY.minusMonths(1);
        int[] scores = {5, 4, 3, 2};
        for (int k = 0; k < scores.length; k++) {
            jdbc.update("insert into grade (student_id, subject_id, grade_date, score, type, created_date) values (?, ?, ?, ?, 'CURRENT', now())",
                    student(0, k).getId(), subjectId, Date.valueOf(lastMonth), scores[k]);
        }
        jdbc.update("insert into grade (student_id, subject_id, grade_date, score, type, created_date) values (?, ?, ?, 5, 'CURRENT', now())",
                student(0, 0).getId(), subjectId, Date.valueOf(MONDAY));

        JournalPageService.Summary s = journalPage.summary(classId, subjectId, YearMonth.from(MONDAY));
        assertEquals(3.5, s.previous().average());
        assertEquals(75.0, s.previous().mastery());
        assertEquals(50.0, s.previous().quality());
        assertEquals(4, s.previous().count());

        manual(0, 0, AttendanceStatus.ABSENT, 2);
        assertEquals("ABSENT", journalPage.dayAttendance(classId, subjectId, MONDAY).get(student(0, 2).getId()));

        JournalPageService.StudentCard card = journalPage.student(student(0, 0).getId(), subjectId, false);
        assertEquals(2, card.grades().size());
        assertEquals(5.0, card.average());
        assertNull(card.guardianPhone());

        assertTrue(journalPage.xlsx(classId, subjectId, YearMonth.from(MONDAY)).length > 1000);
        byte[] pdf = journalPage.pdf(classId, subjectId, YearMonth.from(MONDAY));
        assertEquals("%PDF", new String(pdf, 0, 4));
    }

    @Test
    void homework_isAGradeTypeTheDatabaseTakes() {
        // the old CHECK (type in CURRENT, EXAM, QUARTERLY) is dropped at startup
        assertEquals(0, jdbc.queryForObject(
                "select count(*) from pg_constraint where conname = 'grade_type_check'", Integer.class));
        long subjectId = school.subject.getId();
        long sid = student(0, 3).getId();
        jdbc.update("insert into grade (student_id, subject_id, grade_date, score, type, created_date) values (?, ?, ?, 4, 'HOMEWORK', now())",
                sid, subjectId, Date.valueOf(MONDAY));

        JournalPageService.StudentCard card = journalPage.student(sid, subjectId, false);
        assertEquals(1, card.grades().size());
        assertEquals("HOMEWORK", String.valueOf(card.grades().get(0).type()));
    }
}
