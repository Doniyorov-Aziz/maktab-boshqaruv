package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.bot.BotI18n;
import uz.azizbek.maktabboshqaruv.bot.SubjectIcons;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.service.parent.ParentDataService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentStats;
import uz.azizbek.maktabboshqaruv.service.parent.ParentViews.*;
import uz.azizbek.maktabboshqaruv.telegram.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The Saturday weekly report as a formatted text message (HTML, emoji): attendance,
 * the week's average, one line per subject, the best subject and the one that
 * needs attention. Nothing is drawn — "📱 Batafsil" opens the charts in the Mini App.
 */
@Service
public class WeeklyReportService {

    /** Subjects averaging below this are flagged as needing attention. */
    static final double ATTENTION_BELOW = 4.0;
    private static final int MAX_SUBJECT_LINES = 10;

    private final BotI18n i18n = BotI18n.get();

    @Autowired
    private ParentDataService data;

    /** What the report says about one week. */
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

    /** The whole message: header, numbers, per-subject lines, best/attention, footer. */
    public String text(String lang, Student s, LocalDate from, LocalDate to, WeeklyFacts f, School school) {
        String none = i18n.t(lang, "common.none");
        StringBuilder sb = new StringBuilder(i18n.t(lang, "notif.weekly_caption",
                "period", i18n.dateShort(lang, from) + " – " + i18n.dateShort(lang, to),
                "name", MessageFormatter.escape(ParentDataService.fullName(s)),
                "class", MessageFormatter.escape(ParentDataService.className(s.getSchoolClass())),
                "rate", f.attendanceRate() == null ? none : String.format(Locale.ROOT, "%.0f%%", f.attendanceRate()),
                "avg", f.gradeAverage() == null ? none : String.format(Locale.ROOT, "%.2f", f.gradeAverage()),
                "best", f.best() == null ? none : MessageFormatter.escape(f.best()),
                "attention", f.attention() == null ? none : MessageFormatter.escape(f.attention())));
        if (!f.subjects().isEmpty()) {
            sb.append("\n\n").append(i18n.t(lang, "notif.weekly_subjects"));
            for (SubjectAverage a : f.subjects().stream().limit(MAX_SUBJECT_LINES).toList()) {
                sb.append("\n").append(i18n.t(lang, "notif.weekly_subject_line",
                        "emoji", SubjectIcons.subject(a.subject()),
                        "subject", MessageFormatter.escape(a.subject()),
                        "avg", String.format(Locale.ROOT, "%.1f", a.average()),
                        "count", a.count(),
                        "mark", a.average() >= 4.5 ? "🟢" : a.average() >= ATTENTION_BELOW ? "🔵" : "🟠"));
            }
        }
        return sb + MessageFormatter.footer(lang, school.getName());
    }
}
