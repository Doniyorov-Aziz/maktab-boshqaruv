package uz.azizbek.maktabboshqaruv.dto;

import java.util.List;

public class GradebookResponseDto {

    private List<StudentRow> students;
    private List<GradeResponseDto> grades;

    public List<StudentRow> getStudents() {
        return students;
    }

    public void setStudents(List<StudentRow> students) {
        this.students = students;
    }

    public List<GradeResponseDto> getGrades() {
        return grades;
    }

    public void setGrades(List<GradeResponseDto> grades) {
        this.grades = grades;
    }

    public static class StudentRow {
        private Long studentId;
        private String studentName;
        private Double average;

        public Long getStudentId() {
            return studentId;
        }

        public void setStudentId(Long studentId) {
            this.studentId = studentId;
        }

        public String getStudentName() {
            return studentName;
        }

        public void setStudentName(String studentName) {
            this.studentName = studentName;
        }

        public Double getAverage() {
            return average;
        }

        public void setAverage(Double average) {
            this.average = average;
        }
    }
}
