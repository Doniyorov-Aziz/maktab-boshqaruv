package uz.azizbek.maktabboshqaruv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class HomeworkDtos {

    private HomeworkDtos() {
    }

    /** Create / update. {@code notifyParents} = true (create only) tells the class's parents in the bot at once. */
    public record Request(
            @NotNull(message = "Sinf tanlanishi shart") Long schoolClassId,
            @NotNull(message = "Fan tanlanishi shart") Long subjectId,
            Long employeeId,
            LocalDate assignedDate,
            @NotNull(message = "Topshirish sanasi kiritilishi shart") LocalDate dueDate,
            @NotBlank(message = "Vazifa nomi kiritilishi shart") @Size(max = 200, message = "Nomi 200 belgidan oshmasin") String title,
            @Size(max = 4000, message = "Matn 4000 belgidan oshmasin") String description,
            Boolean notifyParents) {
    }

    public record Response(Long id, Long schoolClassId, String className, Long subjectId, String subjectName,
                           String subjectColor, Long employeeId, String employeeName, LocalDate assignedDate,
                           LocalDate dueDate, String title, String description, String fileName, Long fileSize,
                           LocalDateTime notifiedAt, String createdBy, LocalDateTime createdAt) {
    }
}
