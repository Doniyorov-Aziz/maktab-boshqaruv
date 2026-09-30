package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.BotSettingDto;
import uz.azizbek.maktabboshqaruv.entity.BotSetting;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.repository.BotSettingRepository;
import uz.azizbek.maktabboshqaruv.repository.LessonSlotRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** "Bot sozlamalari": school contacts and automatic-message times for the parent bot. */
@Service
public class BotSettingService {

    @Autowired
    private BotSettingRepository botSettingRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private LessonSlotRepository lessonSlotRepository;

    public BotSetting getOrDefault(School school) {
        return botSettingRepository.findBySchoolId(school.getId()).orElseGet(() -> BotSetting.defaults(school));
    }

    public BotSettingDto getSettings(Long schoolId) {
        return toDto(getOrDefault(findSchool(schoolId)));
    }

    @Transactional
    public BotSettingDto updateSettings(Long schoolId, BotSettingDto request) {
        BotSetting s = getOrDefault(findSchool(schoolId));
        s.setPhone(blankToNull(request.getPhone()));
        s.setDirectorName(blankToNull(request.getDirectorName()));
        s.setReceptionHours(blankToNull(request.getReceptionHours()));
        s.setBellScheduleNote(blankToNull(request.getBellScheduleNote()));
        s.setShowTeacherPhones(request.getShowTeacherPhones());
        s.setTomorrowScheduleTime(LocalTime.parse(request.getTomorrowScheduleTime()));
        s.setWeeklyReportDay(DayOfWeek.valueOf(request.getWeeklyReportDay()));
        s.setWeeklyReportTime(LocalTime.parse(request.getWeeklyReportTime()));
        s.setEventReminderTime(LocalTime.parse(request.getEventReminderTime()));
        s.setLowGradeThreshold(request.getLowGradeThreshold());
        return toDto(botSettingRepository.save(s));
    }

    private School findSchool(Long schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> new IllegalStateException("Bunday maktab mavjud emas"));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private BotSettingDto toDto(BotSetting s) {
        BotSettingDto dto = new BotSettingDto();
        dto.setPhone(s.getPhone());
        dto.setDirectorName(s.getDirectorName());
        dto.setReceptionHours(s.getReceptionHours());
        dto.setBellScheduleNote(s.getBellScheduleNote());
        dto.setShowTeacherPhones(s.getShowTeacherPhones());
        dto.setTomorrowScheduleTime(s.getTomorrowScheduleTime().toString());
        dto.setWeeklyReportDay(s.getWeeklyReportDay().name());
        dto.setWeeklyReportTime(s.getWeeklyReportTime().toString());
        dto.setEventReminderTime(s.getEventReminderTime().toString());
        dto.setLowGradeThreshold(s.getLowGradeThreshold());
        List<String> bells = new ArrayList<>();
        int n = 1;
        for (Object[] row : lessonSlotRepository.findDistinctPeriodsForWeekday(s.getSchool().getId(), "Dushanba")) {
            bells.add(n++ + " · " + row[0].toString().substring(0, 5) + "–" + row[1].toString().substring(0, 5));
        }
        dto.setDerivedBells(bells);
        return dto;
    }
}
