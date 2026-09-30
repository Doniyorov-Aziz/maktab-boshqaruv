package uz.azizbek.maktabboshqaruv.event;

/** Published by GradeService when a grade is created ({@code created=true}) or its score/subject/type changed. */
public record GradeSavedEvent(Long gradeId, boolean created) {
}
