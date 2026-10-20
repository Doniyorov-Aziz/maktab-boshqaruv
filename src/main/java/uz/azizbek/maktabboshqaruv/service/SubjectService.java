package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.SubjectRequestDto;
import uz.azizbek.maktabboshqaruv.dto.SubjectResponseDto;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.entity.Subject;
import uz.azizbek.maktabboshqaruv.repository.GradeRepository;
import uz.azizbek.maktabboshqaruv.repository.LessonSlotRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import uz.azizbek.maktabboshqaruv.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.service.pages.ListStatsService;
import uz.azizbek.maktabboshqaruv.util.PageSort;

import java.util.List;
import java.util.Map;

/**
 * Subjects are never deleted. "O'chirish" makes a subject inactive; renaming a subject
 * that already has grades creates a new active subject and retires the old one, so
 * old grades keep their old name while the timetable moves to the new subject.
 * Inactive subjects are not offered for new grades or lessons.
 */
@Service
public class SubjectService {

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ListStatsService listStats;

    @Autowired
    private BotCache botCache;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private LessonSlotRepository lessonSlotRepository;

    /** Active subjects (what pickers offer); with includeInactive — every subject of the school. */
    public Page<SubjectResponseDto> getAllSubjects(Long schoolId, boolean includeInactive, Pageable pageable) {
        return getAllSubjects(schoolId, includeInactive, null, pageable);
    }

    /** One page of subjects with teachers, classes and lessons a week (one grouped query for the school). */
    public Page<SubjectResponseDto> getAllSubjects(Long schoolId, boolean includeInactive, String q, Pageable pageable) {
        Page<SubjectResponseDto> page = subjectRepository.search(schoolId, includeInactive, PageSort.like(q),
                PageSort.map(pageable, SORTABLE)).map(this::toResponseDto);
        Map<Long, ListStatsService.SubjectStats> stats = listStats.subjects(schoolId);
        for (SubjectResponseDto dto : page.getContent()) {
            ListStatsService.SubjectStats st = stats.get(dto.getId());
            dto.setTeacherCount(st == null ? 0 : st.teachers());
            dto.setClassCount(st == null ? 0 : st.classes());
            dto.setWeeklyLessons(st == null ? 0 : st.weeklyLessons());
        }
        return page;
    }

    private static final Map<String, List<String>> SORTABLE = Map.of(
            "id", List.of("id"), "name", List.of("name"), "active", List.of("active", "name"));

    public Page<SubjectResponseDto> getAllSubjects(Long schoolId, Pageable pageable) {
        return getAllSubjects(schoolId, false, pageable);
    }

    public SubjectResponseDto getSubjectById(Long id) {
        return toResponseDto(find(id));
    }

    /** For grade and lesson forms: the subject must exist and be active. */
    public Subject requireActive(Long id) {
        Subject subject = find(id);
        if (!subject.isActive()) {
            throw new IllegalStateException("«" + subject.getName() + "» fani nofaol — uni tanlab bo'lmaydi");
        }
        return subject;
    }

    @Transactional
    public SubjectResponseDto createSubject(SubjectRequestDto request) {
        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday maktab mavjud emas"));

        if (subjectRepository.existsActiveBySchoolIdAndName(school.getId(), request.getName())) {
            throw new IllegalStateException("Bu fan ushbu maktabda allaqachon mavjud");
        }

        Subject subject = new Subject();
        subject.setSchool(school);
        subject.setName(request.getName());
        subject.setColor(blankToNull(request.getColor()));
        subject.setActive(true);
        return toResponseDto(subjectRepository.save(subject));
    }

    @Transactional
    public SubjectResponseDto updateSubject(Long id, SubjectRequestDto request) {
        Subject subject = find(id);
        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday maktab mavjud emas"));

        boolean renamed = !request.getName().equals(subject.getName());
        String color = blankToNull(request.getColor());
        if (!renamed) {
            if (java.util.Objects.equals(color, subject.getColor())) return toResponseDto(subject);
            subject.setColor(color);
            return toResponseDto(subjectRepository.save(subject));
        }
        botCache.evictAllClasses(); // the bot's cached timetables show subject names
        if (subjectRepository.existsActiveBySchoolIdAndName(school.getId(), request.getName())) {
            throw new IllegalStateException("Bu fan ushbu maktabda allaqachon mavjud");
        }

        // nothing graded yet: a plain rename is safe
        if (!gradeRepository.existsBySubjectId(id)) {
            subject.setSchool(school);
            subject.setName(request.getName());
            subject.setColor(color);
            return toResponseDto(subjectRepository.save(subject));
        }

        // graded: keep the old name for history, continue under the new one
        Subject renamedSubject = new Subject();
        renamedSubject.setSchool(school);
        renamedSubject.setName(request.getName());
        renamedSubject.setColor(color);
        renamedSubject.setActive(true);
        renamedSubject = subjectRepository.save(renamedSubject);
        lessonSlotRepository.moveSubject(id, renamedSubject.getId());
        subject.setActive(false);
        subjectRepository.save(subject);
        return toResponseDto(renamedSubject);
    }

    /** "O'chirish" = nofaol qilish. */
    @Transactional
    public void deleteSubject(Long id) {
        Subject subject = find(id);
        subject.setActive(false);
        subjectRepository.save(subject);
    }

    @Transactional
    public SubjectResponseDto activate(Long id) {
        Subject subject = find(id);
        if (!subject.isActive()
                && subjectRepository.existsActiveBySchoolIdAndName(subject.getSchool().getId(), subject.getName())) {
            throw new IllegalStateException("Shu nomli faol fan allaqachon bor");
        }
        subject.setActive(true);
        return toResponseDto(subjectRepository.save(subject));
    }

    private Subject find(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday fan topilmadi: " + id));
    }

    private SubjectResponseDto toResponseDto(Subject subject) {
        SubjectResponseDto dto = new SubjectResponseDto();
        dto.setId(subject.getId());
        dto.setName(subject.getName());
        dto.setActive(subject.isActive());
        dto.setColor(subject.getColor());
        if (subject.getSchool() != null) {
            dto.setSchoolId(subject.getSchool().getId());
            dto.setSchoolName(subject.getSchool().getName());
        }
        return dto;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim().toLowerCase();
    }
}
