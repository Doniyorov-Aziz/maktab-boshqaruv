package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.bot.BotI18n;
import uz.azizbek.maktabboshqaruv.bot.image.BotImageRenderer;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import uz.azizbek.maktabboshqaruv.service.parent.ParentDataService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentStats;
import uz.azizbek.maktabboshqaruv.service.parent.ParentViews.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The Saturday weekly report: the facts of one child's week, its short caption
 * and the 1080×1350 picture card. The outbox stores only a reference
 * ("weekly:<studentId>:<from>:<to>:<lang>") and the picture is drawn when the
 * message is actually sent, so it always shows the latest data.
 */
@Service
public class WeeklyReportCardService {

    public static final String PREFIX = "weekly:";
    /** Subjects averaging below this are flagged as needing attention. */
    static final double ATTENTION_BELOW = 4.0;

    private final BotI18n i18n = BotI18n.get();

    @Autowired
    private ParentDataService data;

    @Autowired
    private StudentRepository studentRepository;

    /** What the card and the caption say about one week. */
    public record WeeklyFacts(Double attendanceRate, int lessons, Double gradeAverage, int gradeCount,
                              List<SubjectAverage> subjects, String best, String attention) {
    }

    public WeeklyFacts facts(Student s, LocalDate from, LocalDate to) {
        ReportView report = data.report(s, from, to, null);
        List<GradeView> grades = data.gradesBetween(s, from, to);
        Map<String, List<Integer>> bySubject = grades.stream().collect(Collectors.groupingBy(GradeView::subject,
                LinkedHashMap::new, Collectors.mapping(GradeView::score, Collectors.toList())));
        List<SubjectAverage> subjects = new ArrayList<>();
        bySubject.forEach((subject, scores) ->
                subjects.add(new SubjectAverage(null, subject, ParentStats.average(scores), scores.size(), "NONE")));
        subjects.sort(Comparator.comparingDouble(SubjectAverage::average).reversed().thenComparing(SubjectAverage::subject));
        // 🏆 only for a subject averaging 4+, ⚠️ only for one below 4 — never the same subject twice
        SubjectAverage top = subjects.isEmpty() ? null : subjects.get(0);
        SubjectAverage weakest = subjects.isEmpty() ? null : subjects.get(subjects.size() - 1);
        String best = top != null && top.average() >= ATTENTION_BELOW ? top.subject() : null;
        String attention = weakest != null && weakest.average() < ATTENTION_BELOW ? weakest.subject() : null;
        Double rate = report.attendance().total() == 0 ? null : report.attendance().rate();
        return new WeeklyFacts(rate, report.attendance().total(), report.gradeAverage(), report.gradeCount(),
                subjects, best, attention);
    }

    public static String reference(Long studentId, LocalDate from, LocalDate to, String lang) {
        return PREFIX + studentId + ":" + from + ":" + to + ":" + lang;
    }

    /** Draws the card for an outbox reference; null if the reference is not a weekly card. */
    @Transactional(readOnly = true)
    public byte[] render(String reference) {
        if (reference == null || !reference.startsWith(PREFIX)) return null;
        String[] p = reference.substring(PREFIX.length()).split(":");
        Student s = studentRepository.findById(Long.parseLong(p[0])).orElse(null);
        if (s == null) return null;
        return render(s, LocalDate.parse(p[1]), LocalDate.parse(p[2]), p.length > 3 ? p[3] : BotI18n.DEFAULT_LANG);
    }

    public byte[] render(Student s, LocalDate from, LocalDate to, String lang) {
        WeeklyFacts f = facts(s, from, to);
        ChildInfo child = data.child(s);
        return BotImageRenderer.weeklyCard(new BotImageRenderer.WeeklyInput(
                i18n.t(lang, "img.weekly.title"),
                i18n.dateShort(lang, from) + " – " + i18n.dateShort(lang, to),
                child.fullName(),
                child.className() + " · " + child.schoolName(),
                i18n.t(lang, "img.metric.attendance"), f.attendanceRate(),
                i18n.t(lang, "img.metric.grades"), f.gradeAverage(),
                i18n.t(lang, "img.grades_count", "count", f.gradeCount()),
                i18n.t(lang, "img.weekly.chart"),
                f.subjects().stream().map(a -> new BotImageRenderer.SubjectBar(a.subject(), a.average(), a.trend())).toList(),
                i18n.t(lang, "img.weekly.no_grades"),
                i18n.t(lang, "img.weekly.best"), f.best(),
                i18n.t(lang, "img.weekly.attention"), f.attention(),
                i18n.t(lang, "img.none"),
                child.schoolName() + " · " + i18n.t(lang, "app.name")));
    }
}
