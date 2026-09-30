package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.entity.*;

import java.time.LocalTime;

/** Small in-memory object graph shared by the Telegram-related service tests. */
final class TelegramTestFixtures {

    private TelegramTestFixtures() {
    }

    static School school() {
        School school = new School();
        school.setId(1L);
        school.setName("1-maktab");
        return school;
    }

    static SchoolClass schoolClass(School school, long id, int grade, String letter) {
        AcademicYear year = new AcademicYear();
        year.setSchool(school);
        SchoolClass c = new SchoolClass();
        c.setId(id);
        c.setGradeNumber(grade);
        c.setSectionLetter(letter);
        c.setAcademicYear(year);
        return c;
    }

    static Student student(SchoolClass c, long id, String first, String last) {
        Student s = new Student();
        s.setId(id);
        s.setSchoolClass(c);
        s.setFirstName(first);
        s.setLastName(last);
        return s;
    }

    static LessonSlot slot(SchoolClass c, long id, String subject, LocalTime start) {
        Subject subj = new Subject();
        subj.setName(subject);
        LessonSlot slot = new LessonSlot();
        slot.setId(id);
        slot.setSchoolClass(c);
        slot.setSubject(subj);
        slot.setWeekday("Chorshanba");
        slot.setStartTime(start);
        slot.setEndTime(start.plusMinutes(45));
        return slot;
    }

    static ParentTelegramLink link(Student s, long chatId) {
        ParentTelegramLink l = new ParentTelegramLink();
        l.setStudent(s);
        l.setChatId(chatId);
        l.setActive(true);
        return l;
    }
}
