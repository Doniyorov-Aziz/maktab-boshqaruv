package uz.azizbek.maktabboshqaruv.service.dashboard;

/**
 * SQL shared by the home-page widgets. Every query is limited to one school (:s).
 *
 * <p>A student's status for a day is the mark of their <b>latest</b> lesson that day ("where the
 * student is now"): absent in the 1st lesson but present in the 3rd counts as present. Every
 * percentage on the home page — KPI, map, ring, trend, ranking — uses this one definition, so the
 * numbers agree with each other. Present = PRESENT or LATE; the base is present + absent —
 * excused students are left out of both sides (DashboardMetrics.attendance).
 */
final class DashboardQueries {

    private DashboardQueries() {
    }

    /** One row per (date, student) with the latest lesson's mark, between :from and :to. */
    static final String DAY_CTE = """
            day as (
              select distinct on (a.record_date, a.student_id)
                     a.record_date as d, a.student_id, ls.class_id, a.status, a.source
                from attendance a
                join lesson_slot ls on ls.id = a.lesson_slot_id
                join school_class c on c.id = ls.class_id
                join academic_year y on y.id = c.academic_year_id
               where y.school_id = :s and a.record_date between :from and :to
               order by a.record_date, a.student_id, ls.start_time desc)""";

    /** Per date: marked students, present, absent, excused, classes, automatic classes, plus late from all lessons. */
    static final String DAILY = "with " + DAY_CTE + """
            , late as (
              select a.record_date as d,
                     count(distinct a.student_id) as late,
                     avg(a.late_minutes) as avg_late
                from attendance a
                join lesson_slot ls on ls.id = a.lesson_slot_id
                join school_class c on c.id = ls.class_id
                join academic_year y on y.id = c.academic_year_id
               where y.school_id = :s and a.record_date between :from and :to and a.status = 'LATE'
               group by a.record_date)
            select day.d,
                   count(*) as marked,
                   count(*) filter (where day.status <> 'EXCUSED') as counted,
                   count(*) filter (where day.status in ('PRESENT', 'LATE')) as present,
                   count(*) filter (where day.status = 'ABSENT') as absent,
                   count(*) filter (where day.status = 'EXCUSED') as excused,
                   count(distinct day.class_id) as classes,
                   count(distinct day.class_id) filter (where day.source in ('AUTO_CARRY', 'DEMO_SIM')) as auto_classes,
                   max(late.late) as late,
                   max(late.avg_late) as avg_late
              from day left join late on late.d = day.d
             group by day.d
             order by day.d""";

    /** Today per class and status/source: student counts. */
    static final String TODAY_BY_CLASS = "with " + DAY_CTE + """
            select class_id, status, source, count(*) as n
              from day group by class_id, status, source""";

    /**
     * Classes of the school that have students, with their class teacher and number of students.
     * A class without students (an old year's leftover, a class not filled yet) is on no widget.
     */
    static final String CLASSES = """
            select c.id, c.grade_number, c.section_letter,
                   e.id as teacher_id, trim(coalesce(e.first_name, '') || ' ' || coalesce(e.last_name, '')) as teacher,
                   (select count(*) from students st where st.class_id = c.id) as students
              from school_class c
              join academic_year y on y.id = c.academic_year_id
              left join employee e on e.id = c.class_teacher_id
             where y.school_id = :s and exists (select 1 from students st where st.class_id = c.id)
             order by c.grade_number, c.section_letter""";

    /** Lessons of a weekday with class, subject, teacher, room. */
    static final String LESSONS_OF_WEEKDAY = """
            select ls.id, ls.class_id, c.grade_number || '-' || c.section_letter as class_name,
                   s.name as subject, ls.employee_id,
                   trim(coalesce(e.first_name, '') || ' ' || coalesce(e.last_name, '')) as teacher,
                   r.room_number, ls.start_time, ls.end_time,
                   (select count(*) from students st where st.class_id = ls.class_id) as students
              from lesson_slot ls
              join school_class c on c.id = ls.class_id
              join academic_year y on y.id = c.academic_year_id
              join subject s on s.id = ls.subject_id
              join employee e on e.id = ls.employee_id
              left join room r on r.id = ls.room_id
             where y.school_id = :s and ls.weekday = :weekday
             order by ls.start_time, c.grade_number, c.section_letter""";

    /** Marks of one day per lesson: how many, and how many real (MANUAL/DEVICE), automatic, demo. */
    static final String MARKS_BY_LESSON = """
            select a.lesson_slot_id,
                   count(*) as marked,
                   count(*) filter (where a.source in ('MANUAL', 'DEVICE')) as real_marks,
                   count(*) filter (where a.source = 'AUTO_CARRY') as carried,
                   count(*) filter (where a.source = 'DEMO_SIM') as demo,
                   max(a.created_by) filter (where a.source = 'MANUAL') as taken_by,
                   max(a.created_date) filter (where a.source = 'MANUAL') as taken_at
              from attendance a
              join lesson_slot ls on ls.id = a.lesson_slot_id
              join school_class c on c.id = ls.class_id
              join academic_year y on y.id = c.academic_year_id
             where y.school_id = :s and a.record_date = :date
             group by a.lesson_slot_id""";

    /** Grade counts per date between :from and :to: total, passed (3–5), good (4–5). */
    static final String GRADES_DAILY = """
            select g.grade_date as d, count(*) as total,
                   count(*) filter (where g.score >= 3) as passed,
                   count(*) filter (where g.score >= 4) as good
              from grade g
              join students st on st.id = g.student_id
              join school_class c on c.id = st.class_id
              join academic_year y on y.id = c.academic_year_id
             where y.school_id = :s and g.grade_date between :from and :to
             group by g.grade_date""";

    /** The weekday name the lesson_slot table uses, for a date column/expression. */
    static String weekdayOf(String dateExpr) {
        return "(case extract(isodow from " + dateExpr + ") when 1 then 'Dushanba' when 2 then 'Seshanba' "
                + "when 3 then 'Chorshanba' when 4 then 'Payshanba' when 5 then 'Juma' when 6 then 'Shanba' end)";
    }
}
