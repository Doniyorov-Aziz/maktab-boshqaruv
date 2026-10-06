package uz.azizbek.maktabboshqaruv.service;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import uz.azizbek.maktabboshqaruv.dto.TimetableEntryDto;
import uz.azizbek.maktabboshqaruv.entity.AcademicYear;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.entity.SchoolClass;
import uz.azizbek.maktabboshqaruv.repository.SchoolClassRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Each class: one A4 landscape page with the whole week, Cyrillic text included. */
class TimetablePdfServiceTest {

    private final LessonSlotService lessons = mock(LessonSlotService.class);
    private final SchoolClassRepository classes = mock(SchoolClassRepository.class);
    private final SchoolRepository schools = mock(SchoolRepository.class);
    private final TimetablePdfService service = new TimetablePdfService(lessons, classes, schools);

    private SchoolClass schoolClass(long id, int grade, String letter, School school) {
        AcademicYear year = new AcademicYear();
        year.setTitle("2026-2027");
        year.setSchool(school);
        SchoolClass c = new SchoolClass();
        c.setId(id);
        c.setGradeNumber(grade);
        c.setSectionLetter(letter);
        c.setAcademicYear(year);
        return c;
    }

    private static TimetableEntryDto lesson(String day, int n, String subject) {
        TimetableEntryDto e = new TimetableEntryDto();
        e.setLessonSlotId((long) (day.hashCode() + n));
        e.setWeekday(day);
        e.setStartTime(LocalTime.of(8, 0).plusMinutes(55L * n));
        e.setEndTime(LocalTime.of(8, 45).plusMinutes(55L * n));
        e.setSubjectName(subject);
        e.setTeacherLastName("Каримова");
        e.setRoomNumber("201");
        return e;
    }

    @Test
    void eightLessonsSixDays_fitOneLandscapePage() throws Exception {
        School school = new School();
        school.setId(1L);
        school.setName("1-maktab");
        when(schools.findById(1L)).thenReturn(Optional.of(school));
        SchoolClass c = schoolClass(5L, 7, "A", school);
        when(classes.findById(5L)).thenReturn(Optional.of(c));
        List<TimetableEntryDto> week = new ArrayList<>();
        for (String day : TimetablePdfService.WEEKDAYS) {
            for (int n = 0; n < 8; n++) week.add(lesson(day, n, n % 2 == 0 ? "Matematika" : "Она тили"));
        }
        when(lessons.getTimetable(eq(1L), eq(5L), isNull(), isNull())).thenReturn(week);

        byte[] pdf = service.render(1L, 5L);

        PdfReader reader = new PdfReader(pdf);
        assertEquals(1, reader.getNumberOfPages(), "the whole week on one page");
        var size = reader.getPageSizeWithRotation(1);
        assertTrue(size.getWidth() > size.getHeight(), "landscape");
        String text = new PdfTextExtractor(reader).getTextFromPage(1);
        assertTrue(text.contains("7-A sinf"), text);
        assertTrue(text.contains("8-dars"), text);
        assertTrue(text.contains("Она тили"), "Cyrillic is embedded and extractable: " + text);
    }

    @Test
    void allClasses_onePageEach() throws Exception {
        School school = new School();
        school.setId(1L);
        school.setName("1-maktab");
        when(schools.findById(1L)).thenReturn(Optional.of(school));
        when(classes.findByAcademicYearSchoolId(1L)).thenReturn(List.of(
                schoolClass(1L, 1, "A", school), schoolClass(2L, 1, "B", school), schoolClass(3L, 2, "A", school)));
        when(lessons.getTimetable(eq(1L), anyLong(), isNull(), isNull()))
                .thenReturn(List.of(lesson("Dushanba", 0, "Matematika")));

        assertEquals(3, new PdfReader(service.render(1L, null)).getNumberOfPages());
    }

    @Test
    void classOfAnotherSchool_isRefused() {
        School other = new School();
        other.setId(2L);
        when(classes.findById(9L)).thenReturn(Optional.of(schoolClass(9L, 5, "A", other)));
        assertThrows(IllegalStateException.class, () -> service.render(1L, 9L));
    }
}
