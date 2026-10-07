package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.BotStatsDto;
import uz.azizbek.maktabboshqaruv.entity.SchoolClass;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.repository.*;
import uz.azizbek.maktabboshqaruv.service.parent.ParentDataService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class BotStatsService {

    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private SchoolClassRepository schoolClassRepository;
    @Autowired
    private ParentTelegramLinkRepository linkRepository;
    @Autowired
    private BotUsageEventRepository usageRepository;
    @Autowired
    private NotificationLogRepository notificationLogRepository;
    @Autowired
    private uz.azizbek.maktabboshqaruv.repository.AppealRepository appealRepository;
    @Autowired
    private AbsenceRequestService absenceRequestService;
    @Autowired
    private Clock clock;

    public BotStatsDto stats(Long schoolId) {
        LocalDateTime now = LocalDateTime.now(clock);
        BotStatsDto dto = new BotStatsDto();

        long total = studentRepository.countBySchoolClassAcademicYearSchoolId(schoolId);
        long linked = linkRepository.countLinkedStudentsBySchoolId(schoolId);
        dto.setTotalStudents(total);
        dto.setLinkedStudents(linked);
        dto.setLinkedPercent(ParentStats.rate(linked, total));
        dto.setParentCount(linkRepository.countParentsBySchoolId(schoolId));
        dto.setActive7(usageRepository.countActiveChats(schoolId, now.minusDays(7)));
        dto.setActive30(usageRepository.countActiveChats(schoolId, now.minusDays(30)));
        // parent appeals still waiting to be opened (status NEW)
        dto.setNewMessages(appealRepository.countBySchoolIdAndStatus(schoolId, uz.azizbek.maktabboshqaruv.entity.AppealEnums.Status.NEW));
        dto.setPendingAbsences(absenceRequestService.countPending(schoolId));

        Map<Long, Long> studentsByClass = new HashMap<>();
        for (Student s : studentRepository.findBySchoolClassAcademicYearSchoolId(schoolId)) {
            studentsByClass.merge(s.getSchoolClass().getId(), 1L, Long::sum);
        }
        Map<Long, Long> linkedByClass = new HashMap<>();
        for (Object[] row : linkRepository.countLinkedStudentsByClass(schoolId)) {
            linkedByClass.put((Long) row[0], ((Number) row[1]).longValue());
        }
        List<BotStatsDto.ClassCoverage> classes = new ArrayList<>();
        List<SchoolClass> schoolClasses = new ArrayList<>(schoolClassRepository.findByAcademicYearSchoolId(schoolId));
        schoolClasses.sort(Comparator.comparing(SchoolClass::getGradeNumber).thenComparing(SchoolClass::getSectionLetter));
        for (SchoolClass c : schoolClasses) {
            long students = studentsByClass.getOrDefault(c.getId(), 0L);
            if (students == 0) continue;
            BotStatsDto.ClassCoverage cc = new BotStatsDto.ClassCoverage();
            cc.setClassId(c.getId());
            cc.setClassName(ParentDataService.className(c));
            cc.setStudents(students);
            cc.setLinked(linkedByClass.getOrDefault(c.getId(), 0L));
            cc.setPercent(ParentStats.rate(cc.getLinked(), students));
            classes.add(cc);
        }
        dto.setClasses(classes);

        List<BotStatsDto.SectionCount> sections = new ArrayList<>();
        for (Object[] row : usageRepository.countBySection(schoolId, now.minusDays(30))) {
            sections.add(new BotStatsDto.SectionCount((String) row[0], ((Number) row[1]).longValue()));
        }
        dto.setSections(sections);

        // 30 days, zero-filled so the chart has no gaps.
        Map<LocalDate, Long> perDay = new HashMap<>();
        for (Object[] row : notificationLogRepository.sentPerDay(schoolId, now.toLocalDate().minusDays(29).atStartOfDay())) {
            perDay.put((LocalDate) row[0], ((Number) row[1]).longValue());
        }
        List<BotStatsDto.DayCount> days = new ArrayList<>();
        for (LocalDate d = now.toLocalDate().minusDays(29); !d.isAfter(now.toLocalDate()); d = d.plusDays(1)) {
            days.add(new BotStatsDto.DayCount(d, perDay.getOrDefault(d, 0L)));
        }
        dto.setSentPerDay(days);
        return dto;
    }
}
