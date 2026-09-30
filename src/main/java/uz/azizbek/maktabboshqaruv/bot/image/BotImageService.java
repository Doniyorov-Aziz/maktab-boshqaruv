package uz.azizbek.maktabboshqaruv.bot.image;

import uz.azizbek.maktabboshqaruv.bot.BotI18n;
import uz.azizbek.maktabboshqaruv.service.parent.ParentViews.*;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

/**
 * Turns parent views into PNG pages, with labels in the parent's language.
 * Rendered images are cached by a key that includes the data itself, so an
 * unchanged calendar is never drawn twice; the Telegram file_id of an image
 * already uploaded is remembered too, so re-sending it costs no upload.
 */
@Service
public class BotImageService {

    private static final int CACHE_SIZE = 200;
    private final BotI18n i18n = BotI18n.get();

    public record Rendered(String cacheKey, byte[] png, String fileName) {
    }

    private final Map<String, byte[]> images = Collections.synchronizedMap(new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
            return size() > CACHE_SIZE;
        }
    });
    private final Map<String, String> fileIds = Collections.synchronizedMap(new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > CACHE_SIZE;
        }
    });

    public String cachedFileId(String key) {
        return fileIds.get(key);
    }

    public void rememberFileId(String key, String fileId) {
        if (fileId != null) fileIds.put(key, fileId);
    }

    public Rendered attendanceCalendar(String lang, ChildInfo child, AttendanceSummary summary, int year, int month,
                                       LocalDate today) {
        List<String> statuses = summary.days().stream().map(AttendanceDay::status).toList();
        String key = "cal:" + lang + ":" + child.studentId() + ":" + year + "-" + month + ":" + today + ":" + statuses.hashCode();
        byte[] png = images.computeIfAbsent(key, k -> {
            List<String> dows = new ArrayList<>();
            for (DayOfWeek d : DayOfWeek.values()) dows.add(i18n.dowShort(lang, d));
            Map<String, String> legend = Map.of(
                    "PRESENT", i18n.t(lang, "img.legend.present"), "LATE", i18n.t(lang, "img.legend.late"),
                    "ABSENT", i18n.t(lang, "img.legend.absent"), "EXCUSED", i18n.t(lang, "img.legend.excused"),
                    "NONE", i18n.t(lang, "img.legend.none"));
            return BotImageRenderer.attendanceCalendar(new BotImageRenderer.CalendarInput(
                    i18n.t(lang, "img.attendance_title") + " · " + i18n.monthYear(lang, month, year),
                    child.fullName() + " · " + child.className(),
                    child.schoolName() + " · " + i18n.t(lang, "app.name"),
                    dows,
                    new BotImageRenderer.YearMonthDays(LocalDate.of(year, month, 1), statuses, today),
                    legend, summary.rate(), i18n.t(lang, "img.rate"),
                    i18n.t(lang, "img.counts", "present", summary.present(), "late", summary.late(),
                            "absent", summary.absent(), "excused", summary.excused())));
        });
        return new Rendered(key, png, "davomat-" + year + "-" + month + ".png");
    }

    public Rendered subjectChart(String lang, ChildInfo child, List<SubjectAverage> averages) {
        String key = "chart:" + lang + ":" + child.studentId() + ":" + averages.hashCode();
        byte[] png = images.computeIfAbsent(key, k -> BotImageRenderer.subjectChart(new BotImageRenderer.ChartInput(
                i18n.t(lang, "img.subjects_title"),
                child.fullName() + " · " + child.className(),
                child.schoolName() + " · " + i18n.t(lang, "app.name"),
                averages.stream().map(a -> new BotImageRenderer.SubjectBar(a.subject(), a.average(), a.trend())).toList(),
                i18n.t(lang, "img.subjects_empty"))));
        return new Rendered(key, png, "baholar.png");
    }

    public Rendered reportCard(String lang, ChildInfo child, ReportView report, boolean month) {
        String key = "report:" + lang + ":" + child.studentId() + ":" + report.hashCode();
        byte[] png = images.computeIfAbsent(key, k -> BotImageRenderer.reportCard(new BotImageRenderer.ReportInput(
                i18n.t(lang, month ? "img.report_month" : "img.report_week"),
                report.period(),
                child.fullName(),
                child.className() + " · " + child.schoolName(),
                i18n.t(lang, "img.metric.attendance"), report.attendance().rate(),
                i18n.t(lang, "img.metric.grades"), report.gradeAverage(),
                i18n.t(lang, "img.grades_count", "count", report.gradeCount()),
                i18n.t(lang, "img.metric.rewards"), report.rewards(),
                i18n.t(lang, "img.metric.warnings"), report.warnings(),
                i18n.t(lang, "img.strong"), report.strongSubjects(),
                i18n.t(lang, "img.attention"), report.attentionSubjects(),
                i18n.t(lang, "img.none"),
                i18n.t(lang, "app.name"))));
        return new Rendered(key, png, "hisobot.png");
    }
}
