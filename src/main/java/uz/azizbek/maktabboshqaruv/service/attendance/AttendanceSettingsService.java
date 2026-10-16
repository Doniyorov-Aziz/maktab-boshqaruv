package uz.azizbek.maktabboshqaruv.service.attendance;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.azizbek.maktabboshqaruv.entity.SchoolSettings;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolSettingsRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/** "Maktab sozlamalari": a school's own row, or the application.properties defaults. */
@Service
public class AttendanceSettingsService {

    /** The settings a school works with (all of them, whether saved or default). */
    public record Settings(
            boolean autoCarryEnabled,
            @Min(1) @Max(45) int autoCarryDelayMinutes,
            @Min(1) @Max(45) int reminderMinutes,
            @Min(1) @Max(45) int escalationMinutes,
            boolean notifyOnAuto,
            @Min(50) @Max(100) int attendanceTarget,
            @Min(2) @Max(10) int absentStreakDays,
            @Min(5) @Max(100) int disciplineThreshold,
            @Min(1) @Max(168) int appealOverdueHours,
            @Min(0) @Max(100) int telegramLowPercent,
            @Min(50) @Max(100) int heatGood,
            @Min(50) @Max(100) int heatOk,
            @Min(50) @Max(100) int heatWarn,
            boolean saved,
            LocalDateTime updatedAt,
            String updatedBy) {
    }

    private final SchoolSettingsRepository repository;
    private final SchoolRepository schools;
    private final Clock clock;
    private final Settings defaults;
    private final Cache<Long, Settings> cache = Caffeine.newBuilder()
            .maximumSize(1_000).expireAfterWrite(Duration.ofMinutes(5)).build();

    public AttendanceSettingsService(SchoolSettingsRepository repository, SchoolRepository schools, Clock clock,
                                     @Value("${attendance.auto-carry.enabled:true}") boolean autoCarry,
                                     @Value("${attendance.auto-carry.delay-minutes:10}") int autoCarryDelay,
                                     @Value("${attendance.reminder-minutes:5}") int reminder,
                                     @Value("${attendance.escalation-minutes:10}") int escalation,
                                     @Value("${attendance.notify-on-auto:false}") boolean notifyOnAuto,
                                     @Value("${dashboard.attendance-target:95}") int target,
                                     @Value("${dashboard.absent-streak-days:3}") int streak,
                                     @Value("${dashboard.discipline-threshold:30}") int discipline,
                                     @Value("${dashboard.appeal-overdue-hours:24}") int appealHours,
                                     @Value("${dashboard.telegram-low-percent:50}") int telegramLow,
                                     @Value("${dashboard.heat.good:95}") int heatGood,
                                     @Value("${dashboard.heat.ok:90}") int heatOk,
                                     @Value("${dashboard.heat.warn:85}") int heatWarn) {
        this.repository = repository;
        this.schools = schools;
        this.clock = clock;
        this.defaults = new Settings(autoCarry, autoCarryDelay, reminder, escalation, notifyOnAuto, target, streak,
                discipline, appealHours, telegramLow, heatGood, heatOk, heatWarn, false, null, null);
    }

    @Transactional(readOnly = true)
    public Settings forSchool(long schoolId) {
        return cache.get(schoolId, id -> repository.findBySchoolId(id).map(AttendanceSettingsService::toSettings).orElse(defaults));
    }

    @Transactional
    public Settings save(long schoolId, Settings s, String username) {
        if (!(s.heatGood() >= s.heatOk() && s.heatOk() >= s.heatWarn())) {
            throw new IllegalArgumentException("Xarita chegaralari kamayib borishi kerak: to'q yashil ≥ yashil ≥ sariq");
        }
        SchoolSettings row = repository.findBySchoolId(schoolId).orElseGet(() -> {
            SchoolSettings n = new SchoolSettings();
            n.setSchool(schools.findById(schoolId).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Bunday maktab topilmadi: " + schoolId)));
            return n;
        });
        row.setAutoCarryEnabled(s.autoCarryEnabled());
        row.setAutoCarryDelayMinutes(s.autoCarryDelayMinutes());
        row.setReminderMinutes(s.reminderMinutes());
        row.setEscalationMinutes(s.escalationMinutes());
        row.setNotifyOnAuto(s.notifyOnAuto());
        row.setAttendanceTarget(s.attendanceTarget());
        row.setAbsentStreakDays(s.absentStreakDays());
        row.setDisciplineThreshold(s.disciplineThreshold());
        row.setAppealOverdueHours(s.appealOverdueHours());
        row.setTelegramLowPercent(s.telegramLowPercent());
        row.setHeatGood(s.heatGood());
        row.setHeatOk(s.heatOk());
        row.setHeatWarn(s.heatWarn());
        row.setUpdatedAt(LocalDateTime.now(clock));
        row.setUpdatedBy(username);
        Settings saved = toSettings(repository.save(row));
        cache.put(schoolId, saved);
        return saved;
    }

    /** For tests: forget cached settings. */
    public void forget(long schoolId) {
        cache.invalidate(schoolId);
    }

    public Optional<Settings> defaults() {
        return Optional.of(defaults);
    }

    private static Settings toSettings(SchoolSettings r) {
        return new Settings(r.isAutoCarryEnabled(), r.getAutoCarryDelayMinutes(), r.getReminderMinutes(),
                r.getEscalationMinutes(), r.isNotifyOnAuto(), r.getAttendanceTarget(), r.getAbsentStreakDays(),
                r.getDisciplineThreshold(), r.getAppealOverdueHours(), r.getTelegramLowPercent(), r.getHeatGood(),
                r.getHeatOk(), r.getHeatWarn(), true, r.getUpdatedAt(), r.getUpdatedBy());
    }
}
