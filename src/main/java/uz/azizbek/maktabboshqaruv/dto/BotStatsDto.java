package uz.azizbek.maktabboshqaruv.dto;

import java.time.LocalDate;
import java.util.List;

/** "Bot statistikasi": coverage, activity, popular sections and delivery over time. */
public class BotStatsDto {

    private long totalStudents;
    private long linkedStudents;
    private Double linkedPercent;
    private long parentCount;
    private long active7;
    private long active30;
    private long newMessages;
    private long pendingAbsences;
    private List<ClassCoverage> classes;
    private List<SectionCount> sections;
    private List<DayCount> sentPerDay;

    public static class ClassCoverage {
        private Long classId;
        private String className;
        private long students;
        private long linked;
        private Double percent;

        public Long getClassId() {
            return classId;
        }

        public void setClassId(Long classId) {
            this.classId = classId;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public long getStudents() {
            return students;
        }

        public void setStudents(long students) {
            this.students = students;
        }

        public long getLinked() {
            return linked;
        }

        public void setLinked(long linked) {
            this.linked = linked;
        }

        public Double getPercent() {
            return percent;
        }

        public void setPercent(Double percent) {
            this.percent = percent;
        }
    }

    public static class SectionCount {
        private String section;
        private long count;

        public SectionCount() {
        }

        public SectionCount(String section, long count) {
            this.section = section;
            this.count = count;
        }

        public String getSection() {
            return section;
        }

        public void setSection(String section) {
            this.section = section;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public static class DayCount {
        private LocalDate date;
        private long count;

        public DayCount() {
        }

        public DayCount(LocalDate date, long count) {
            this.date = date;
            this.count = count;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public long getLinkedStudents() {
        return linkedStudents;
    }

    public void setLinkedStudents(long linkedStudents) {
        this.linkedStudents = linkedStudents;
    }

    public Double getLinkedPercent() {
        return linkedPercent;
    }

    public void setLinkedPercent(Double linkedPercent) {
        this.linkedPercent = linkedPercent;
    }

    public long getParentCount() {
        return parentCount;
    }

    public void setParentCount(long parentCount) {
        this.parentCount = parentCount;
    }

    public long getActive7() {
        return active7;
    }

    public void setActive7(long active7) {
        this.active7 = active7;
    }

    public long getActive30() {
        return active30;
    }

    public void setActive30(long active30) {
        this.active30 = active30;
    }

    public long getNewMessages() {
        return newMessages;
    }

    public void setNewMessages(long newMessages) {
        this.newMessages = newMessages;
    }

    public long getPendingAbsences() {
        return pendingAbsences;
    }

    public void setPendingAbsences(long pendingAbsences) {
        this.pendingAbsences = pendingAbsences;
    }

    public List<ClassCoverage> getClasses() {
        return classes;
    }

    public void setClasses(List<ClassCoverage> classes) {
        this.classes = classes;
    }

    public List<SectionCount> getSections() {
        return sections;
    }

    public void setSections(List<SectionCount> sections) {
        this.sections = sections;
    }

    public List<DayCount> getSentPerDay() {
        return sentPerDay;
    }

    public void setSentPerDay(List<DayCount> sentPerDay) {
        this.sentPerDay = sentPerDay;
    }
}
