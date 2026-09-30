package uz.azizbek.maktabboshqaruv.controller;

import uz.azizbek.maktabboshqaruv.dto.BotSettingDto;
import uz.azizbek.maktabboshqaruv.dto.BotStatsDto;
import uz.azizbek.maktabboshqaruv.service.BotScheduledJobs;
import uz.azizbek.maktabboshqaruv.service.BotSettingService;
import uz.azizbek.maktabboshqaruv.service.BotStatsService;
import uz.azizbek.maktabboshqaruv.telegram.TelegramProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

/** "Bot statistikasi" and "Bot sozlamalari". */
@RestController
@RequestMapping("/api/bot")
public class BotAdminController {

    @Autowired
    private BotStatsService botStatsService;

    @Autowired
    private BotSettingService botSettingService;

    @Autowired
    private BotScheduledJobs jobs;

    @Autowired
    private TelegramProperties telegramProperties;

    @Autowired
    private Clock clock;

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @GetMapping("/stats")
    public BotStatsDto stats(@RequestParam Long schoolId) {
        return botStatsService.stats(schoolId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @GetMapping("/settings")
    public BotSettingDto getSettings(@RequestParam Long schoolId) {
        return botSettingService.getSettings(schoolId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/settings")
    public BotSettingDto updateSettings(@RequestParam Long schoolId, @Valid @RequestBody BotSettingDto request) {
        return botSettingService.updateSettings(schoolId, request);
    }

    /** telegram.mock=true only: run the scheduled digests now, ignoring their time of day (for demos and testing). */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/mock/run-jobs")
    public Map<String, Integer> runJobs() {
        if (!telegramProperties.isMock()) {
            throw new IllegalStateException("Bu endpoint faqat telegram.mock=true rejimida ishlaydi");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        return Map.of(
                "tomorrowSchedule", jobs.runTomorrowSchedules(now, true),
                "weeklyReport", jobs.runWeeklyReports(now, true),
                "eventReminder", jobs.runEventReminders(now, true));
    }
}
