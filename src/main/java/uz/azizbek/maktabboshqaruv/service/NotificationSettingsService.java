package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.NotificationSettingsDto;
import uz.azizbek.maktabboshqaruv.entity.NotificationSettings;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.repository.NotificationSettingsRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import uz.azizbek.maktabboshqaruv.telegram.TelegramProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

@Service
public class NotificationSettingsService {

    @Autowired
    private NotificationSettingsRepository settingsRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private TelegramProperties telegramProperties;

    /** The school's saved settings, or unsaved defaults (all on, global quiet hours). */
    public NotificationSettings getOrDefault(School school) {
        return settingsRepository.findBySchoolId(school.getId())
                .orElseGet(() -> NotificationSettings.defaults(school,
                        telegramProperties.getQuietHoursStart(), telegramProperties.getQuietHoursEnd()));
    }

    public NotificationSettingsDto getSettings(Long schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new IllegalStateException("Bunday maktab mavjud emas"));
        return toDto(getOrDefault(school));
    }

    @Transactional
    public NotificationSettingsDto updateSettings(Long schoolId, NotificationSettingsDto request) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new IllegalStateException("Bunday maktab mavjud emas"));
        NotificationSettings settings = getOrDefault(school);

        settings.setAttendanceEnabled(request.getAttendanceEnabled());
        settings.setGradeEnabled(request.getGradeEnabled());
        settings.setAnnouncementEnabled(request.getAnnouncementEnabled());
        settings.setQuietHoursEnabled(request.getQuietHoursEnabled());
        settings.setQuietHoursStart(parseTime(request.getQuietHoursStart()));
        settings.setQuietHoursEnd(parseTime(request.getQuietHoursEnd()));

        return toDto(settingsRepository.save(settings));
    }

    private LocalTime parseTime(String value) {
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalStateException("Vaqt HH:mm formatida bo'lishi kerak: " + value);
        }
    }

    private NotificationSettingsDto toDto(NotificationSettings s) {
        NotificationSettingsDto dto = new NotificationSettingsDto();
        dto.setAttendanceEnabled(s.getAttendanceEnabled());
        dto.setGradeEnabled(s.getGradeEnabled());
        dto.setAnnouncementEnabled(s.getAnnouncementEnabled());
        dto.setQuietHoursEnabled(s.getQuietHoursEnabled());
        dto.setQuietHoursStart(s.getQuietHoursStart().toString());
        dto.setQuietHoursEnd(s.getQuietHoursEnd().toString());
        return dto;
    }
}
