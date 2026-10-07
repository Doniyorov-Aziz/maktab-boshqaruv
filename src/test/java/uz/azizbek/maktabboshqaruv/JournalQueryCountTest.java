package uz.azizbek.maktabboshqaruv;

import jakarta.persistence.EntityManagerFactory;
import uz.azizbek.maktabboshqaruv.entity.LessonSlot;
import uz.azizbek.maktabboshqaruv.repository.LessonSlotRepository;
import uz.azizbek.maktabboshqaruv.service.JournalService;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** "Har endpoint 1–3 ta SQL so'rov, N+1 yo'q": statements per call, whatever the class size. */
@SpringBootTest(properties = {
        "telegram.mock=true",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@ActiveProfiles("local")
class JournalQueryCountTest {

    @Autowired
    private JournalService journal;
    @Autowired
    private LessonSlotRepository slots;
    @Autowired
    private EntityManagerFactory emf;

    @Test
    void journalOverviewAndCurrentLesson_needAtMostThreeStatements() {
        LessonSlot slot = slots.findAll().get(0);
        Long classId = slot.getSchoolClass().getId();
        Long subjectId = slot.getSubject().getId();
        Long teacherId = slot.getEmployee().getId();
        Statistics st = emf.unwrap(SessionFactory.class).getStatistics();

        st.clear();
        JournalService.Journal j = journal.journal(classId, subjectId, YearMonth.of(2026, 10));
        long journalStatements = st.getPrepareStatementCount();
        assertTrue(j.students().size() > 5, "a real class");
        assertTrue(journalStatements <= 3, "journal: " + journalStatements + " statements");

        st.clear();
        JournalService.Overview o = journal.overview(classId);
        long overviewStatements = st.getPrepareStatementCount();
        assertTrue(!o.subjects().isEmpty());
        assertTrue(overviewStatements <= 3, "overview: " + overviewStatements + " statements");

        st.clear();
        journal.currentLesson(teacherId);
        long currentStatements = st.getPrepareStatementCount();
        assertTrue(currentStatements <= 2, "current lesson: " + currentStatements + " statements");
        System.out.println("JOURNAL statements: journal=" + journalStatements + " overview=" + overviewStatements
                + " currentLesson=" + currentStatements);
    }
}
