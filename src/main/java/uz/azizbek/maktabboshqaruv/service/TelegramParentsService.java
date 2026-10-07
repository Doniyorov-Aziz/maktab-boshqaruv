package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.entity.ParentTelegramLink;
import uz.azizbek.maktabboshqaruv.entity.Role;
import uz.azizbek.maktabboshqaruv.entity.SchoolClass;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.exception.ForbiddenException;
import uz.azizbek.maktabboshqaruv.repository.ParentTelegramLinkRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolClassRepository;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import uz.azizbek.maktabboshqaruv.service.parent.ParentDataService;
import uz.azizbek.maktabboshqaruv.telegram.TelegramProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * "Sinf bo'yicha ota-onalar": every student of a class with the parents linked in the
 * bot. The Telegram chat id is shown to ADMIN only, and only partly (12****89); the
 * invite link holds the child's link code, so only EDITOR+ receive it.
 */
@Service
public class TelegramParentsService {

    public record Parent(Long linkId, String name, String username, LocalDateTime linkedAt, String chatId) {
    }

    public record Row(Long studentId, String studentName, String guardianName, String guardianPhone,
                      boolean linked, List<Parent> parents, String inviteLink) {
    }

    public record ClassParents(Long classId, String className, int total, int linked, List<Row> rows) {
    }

    /** One linked parent for the "choose parents" list of a broadcast. */
    public record Choice(Long linkId, String parentName, String username, String studentName, String className) {
    }

    private final StudentRepository students;
    private final SchoolClassRepository classes;
    private final ParentTelegramLinkRepository links;
    private final TelegramProperties telegram;

    public TelegramParentsService(StudentRepository students, SchoolClassRepository classes,
                                  ParentTelegramLinkRepository links, TelegramProperties telegram) {
        this.students = students;
        this.classes = classes;
        this.links = links;
        this.telegram = telegram;
    }

    /** {@code status}: ALL, LINKED or NOT_LINKED. Two queries whatever the class size. */
    @Transactional(readOnly = true)
    public ClassParents byClass(SchoolAccessService.Caller caller, Long classId, String status) {
        SchoolClass c = classes.findById(classId).orElseThrow(() -> new IllegalStateException("Sinf topilmadi"));
        if (caller.schoolId() != null && !caller.schoolId().equals(c.getAcademicYear().getSchool().getId())) {
            throw new ForbiddenException("Boshqa maktab ma'lumotiga ruxsat yo'q");
        }
        Map<Long, List<ParentTelegramLink>> byStudent = new HashMap<>();
        for (ParentTelegramLink l : links.findActiveByClassId(classId)) {
            byStudent.computeIfAbsent(l.getStudent().getId(), k -> new ArrayList<>()).add(l);
        }
        boolean admin = caller.role() == Role.ADMIN;
        boolean editor = admin || caller.role() == Role.EDITOR;
        List<Row> rows = new ArrayList<>();
        int linked = 0;
        List<Student> all = students.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(classId);
        for (Student s : all) {
            List<ParentTelegramLink> own = byStudent.getOrDefault(s.getId(), List.of());
            boolean isLinked = !own.isEmpty();
            if (isLinked) linked++;
            if ("LINKED".equals(status) && !isLinked) continue;
            if ("NOT_LINKED".equals(status) && isLinked) continue;
            List<Parent> parents = own.stream()
                    .sorted(Comparator.comparing(ParentTelegramLink::getLinkedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(l -> new Parent(l.getId(), l.getFirstName(), l.getTelegramUsername(), l.getLinkedAt(),
                            admin ? mask(l.getChatId()) : null))
                    .toList();
            rows.add(new Row(s.getId(), s.getLastName() + " " + s.getFirstName(), s.getGuardianName(), s.getGuardianPhone(),
                    isLinked, parents, editor && !isLinked ? telegram.deepLink(s.getTelegramLinkCode()) : null));
        }
        return new ClassParents(c.getId(), ParentDataService.className(c), all.size(), linked, rows);
    }

    /** Linked parents to choose from (search by parent or child name); at most 200 per request. */
    @Transactional(readOnly = true)
    public List<Choice> choices(Long schoolId, Long classId, String q) {
        String like = q == null || q.isBlank() ? "" : "%" + q.strip().toLowerCase(Locale.ROOT) + "%";
        List<Choice> result = new ArrayList<>();
        for (ParentTelegramLink l : links.findActiveWithStudent(schoolId, classId, like, PageRequest.of(0, 200))) {
            Student s = l.getStudent();
            result.add(new Choice(l.getId(), l.getFirstName(), l.getTelegramUsername(),
                    s.getLastName() + " " + s.getFirstName(), ParentDataService.className(s.getSchoolClass())));
        }
        return result;
    }

    /** 1234567890 → "12******90": enough to tell chats apart, not enough to contact anyone. */
    static String mask(Long chatId) {
        if (chatId == null) return null;
        String s = String.valueOf(Math.abs(chatId));
        if (s.length() <= 4) return "*".repeat(s.length());
        return s.substring(0, 2) + "*".repeat(s.length() - 4) + s.substring(s.length() - 2);
    }
}
