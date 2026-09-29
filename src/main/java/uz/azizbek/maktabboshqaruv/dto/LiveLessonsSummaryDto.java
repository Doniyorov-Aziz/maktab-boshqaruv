package uz.azizbek.maktabboshqaruv.dto;

public class LiveLessonsSummaryDto {

    private String periodLabel;
    private long classesInSession;
    private String transitionLabel;
    private Long minutesToTransition;

    public String getPeriodLabel() {
        return periodLabel;
    }

    public void setPeriodLabel(String periodLabel) {
        this.periodLabel = periodLabel;
    }

    public long getClassesInSession() {
        return classesInSession;
    }

    public void setClassesInSession(long classesInSession) {
        this.classesInSession = classesInSession;
    }

    public String getTransitionLabel() {
        return transitionLabel;
    }

    public void setTransitionLabel(String transitionLabel) {
        this.transitionLabel = transitionLabel;
    }

    public Long getMinutesToTransition() {
        return minutesToTransition;
    }

    public void setMinutesToTransition(Long minutesToTransition) {
        this.minutesToTransition = minutesToTransition;
    }
}
