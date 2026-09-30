package uz.azizbek.maktabboshqaruv.dto;

public class NotificationStatsDto {

    private long sentToday;
    private long failedToday;
    private long failedTotal;
    private long pending;
    private long skippedToday;
    private long linkedStudents;
    private long totalStudents;
    private Double linkedPercent;
    private long parentCount;

    public long getSentToday() {
        return sentToday;
    }

    public void setSentToday(long sentToday) {
        this.sentToday = sentToday;
    }

    public long getFailedToday() {
        return failedToday;
    }

    public void setFailedToday(long failedToday) {
        this.failedToday = failedToday;
    }

    public long getFailedTotal() {
        return failedTotal;
    }

    public void setFailedTotal(long failedTotal) {
        this.failedTotal = failedTotal;
    }

    public long getPending() {
        return pending;
    }

    public void setPending(long pending) {
        this.pending = pending;
    }

    public long getSkippedToday() {
        return skippedToday;
    }

    public void setSkippedToday(long skippedToday) {
        this.skippedToday = skippedToday;
    }

    public long getLinkedStudents() {
        return linkedStudents;
    }

    public void setLinkedStudents(long linkedStudents) {
        this.linkedStudents = linkedStudents;
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
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
}
