package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.entity.BotUsageEvent;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.repository.BotUsageEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

/** Records which bot section a parent opened — the data behind "Bot statistikasi". Never throws. */
@Service
public class BotUsageService {

    @Autowired
    private BotUsageEventRepository repository;

    @Autowired
    private Clock clock;

    public void record(long chatId, Student student, String section) {
        if (section == null) return;
        try {
            BotUsageEvent e = new BotUsageEvent();
            e.setChatId(chatId);
            if (student != null) {
                e.setStudentId(student.getId());
                e.setSchoolId(student.getSchoolClass().getAcademicYear().getSchool().getId());
            }
            e.setSection(section);
            e.setCreatedAt(LocalDateTime.now(clock));
            repository.save(e);
        } catch (Exception ignored) {
            // statistics are best-effort; never break the parent's page
        }
    }
}
