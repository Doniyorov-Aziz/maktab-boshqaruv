package uz.azizbek.maktabboshqaruv.service.parent;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Read-only views of a child's school life, shared by the Telegram bot screens
 * and the Mini App API (/api/parent/**). Plain records: serialized as JSON for
 * the Mini App, formatted into text/images by the bot.
 */
public final class ParentViews {

    private ParentViews() {
    }

    public record ChildInfo(Long studentId, String fullName, String firstName, String className, Long classId,
                            Long schoolId, String schoolName, String classTeacher) {
    }

    public record LessonView(int number, LocalTime start, LocalTime end, Long subjectId, String subject,
                             String teacher, String room, boolean now, Integer breakBeforeMinutes) {
    }

    public record DaySchedule(LocalDate date, DayOfWeek dayOfWeek, List<LessonView> lessons, String holidayTitle,
                              boolean weekend, List<EventView> events) {
        public boolean hasLessons() {
            return holidayTitle == null && !lessons.isEmpty();
        }
    }

    /** One calendar day: PRESENT, LATE, ABSENT, EXCUSED or NONE (no lessons marked) — the "worst" status of the day. */
    public record AttendanceDay(LocalDate date, String status) {
    }

    public record AttendanceSummary(String period, LocalDate from, LocalDate to, int total, int present, int late,
                                    int absent, int excused, Double rate, Double classRate,
                                    List<AttendanceDay> days) {
    }

    public record AttendanceIncident(LocalDate date, int lessonNumber, String subject, String status) {
    }

    public record SubjectAbsence(String subject, int absent, int late) {
    }

    public record GradeView(Long id, LocalDate date, Long subjectId, String subject, int score, String type,
                            String comment) {
    }

    /** trend: UP, DOWN, FLAT or NONE (this month vs previous month). */
    public record SubjectAverage(Long subjectId, String subject, double average, int count, String trend) {
    }

    /** One day of the child's grade dynamics: the average of that day's grades. */
    public record TrendPoint(LocalDate date, double average, int count) {
    }

    public record SubjectDetail(Long subjectId, String subject, String teacher, double average,
                                List<GradeView> grades) {
    }

    public record ReportView(String period, LocalDate from, LocalDate to, AttendanceSummary attendance,
                             int gradeCount, Double gradeAverage, int rewards, int warnings,
                             List<String> strongSubjects, List<String> attentionSubjects) {
    }

    public record BehaviorView(LocalDate date, String type, String description) {
    }

    public record AnnouncementView(Long id, String title, String content, LocalDate date, boolean important,
                                   boolean unread, LocalDate deadline, String classLabel) {
    }

    public record EventView(Long id, String title, String type, LocalDate startDate, LocalDate endDate,
                            String description) {
    }

    public record TeacherView(String subject, String name, String phone, boolean classTeacher) {
    }

    public record BellView(int number, LocalTime start, LocalTime end) {
    }

    public record SchoolInfo(String name, String address, String phone, String director, String receptionHours,
                             String bellNote, List<BellView> bells) {
    }

    public record TodaySummary(ChildInfo child, LocalDate date, DaySchedule schedule, String attendanceState,
                               int lateCount, int absentCount, List<GradeView> gradesToday,
                               int newAnnouncements, EventView nextEvent) {
    }

    public record MessageView(Long id, String recipient, String text, String status, LocalDateTime createdAt,
                              String replyText) {
    }

    public record AbsenceView(Long id, LocalDate from, LocalDate to, String reason, String status,
                              String decisionNote) {
    }
}
