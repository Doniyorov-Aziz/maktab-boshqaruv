package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.entity.ActivityLog;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.repository.ActivityLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Thin recorder other services call, one line, right after a write they
 * want to surface on the dashboard's activity feed. Never throws — a
 * logging failure must not break the actual operation it's describing.
 */
@Service
public class ActivityLogService {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    public void record(School school, String icon, String description) {
        try {
            ActivityLog log = new ActivityLog();
            log.setSchool(school);
            log.setIcon(icon);
            log.setDescription(description);
            log.setActorUsername(currentUsername());
            log.setOccurredAt(LocalDateTime.now());
            activityLogRepository.save(log);
        } catch (Exception ignored) {
            // best-effort — the actual write this describes already succeeded
        }
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }
}
