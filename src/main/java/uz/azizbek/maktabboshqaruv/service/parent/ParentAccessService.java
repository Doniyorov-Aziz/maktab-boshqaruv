package uz.azizbek.maktabboshqaruv.service.parent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.azizbek.maktabboshqaruv.entity.ParentSession;
import uz.azizbek.maktabboshqaruv.entity.ParentTelegramLink;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.repository.ParentSessionRepository;
import uz.azizbek.maktabboshqaruv.repository.ParentTelegramLinkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The single gate between a Telegram chat and a student's data. Every bot
 * callback, command and Mini App request resolves its student through here:
 * a student id arriving from outside (callback_data, URL) is never trusted —
 * it must belong to an active link of this very chat.
 */
@Service
public class ParentAccessService {

    private static final Logger log = LoggerFactory.getLogger(ParentAccessService.class);

    /** Thrown when a chat asks for a student it is not linked to. */
    public static class AccessDenied extends RuntimeException {
        public AccessDenied(String message) {
            super(message);
        }
    }

    @Autowired
    private ParentTelegramLinkRepository linkRepository;

    @Autowired
    private ParentSessionRepository sessionRepository;

    @Autowired
    private Clock clock;

    public List<Student> linkedStudents(long chatId) {
        return linkRepository.findByChatIdAndActiveTrue(chatId).stream()
                .map(ParentTelegramLink::getStudent)
                .sorted(java.util.Comparator.comparing(Student::getFirstName).thenComparing(Student::getId))
                .toList();
    }

    /** The student with this id, only if this chat is actively linked to it. */
    public Student requireLinked(long chatId, Long studentId) {
        if (studentId != null) {
            for (Student s : linkedStudents(chatId)) {
                if (s.getId().equals(studentId)) return s;
            }
        }
        log.warn("Ruxsatsiz urinish: chat={} o'quvchi #{} ga bog'lanmagan", chatId, studentId);
        throw new AccessDenied("Bu o'quvchi ushbu chat'ga bog'lanmagan");
    }

    /**
     * The child the parent is currently looking at: the remembered choice if it
     * is still linked, otherwise the first linked child (and the choice is
     * updated). Null when nothing is linked.
     */
    @Transactional
    public Student selected(ParentSession session) {
        List<Student> students = linkedStudents(session.getChatId());
        if (students.isEmpty()) return null;
        for (Student s : students) {
            if (s.getId().equals(session.getSelectedStudentId())) return s;
        }
        Student first = students.get(0);
        session.setSelectedStudentId(first.getId());
        sessionRepository.save(session);
        return first;
    }

    @Transactional
    public ParentSession session(long chatId) {
        return sessionRepository.findByChatId(chatId).orElseGet(() -> {
            ParentSession s = new ParentSession();
            s.setChatId(chatId);
            s.setCreatedAt(LocalDateTime.now(clock));
            return sessionRepository.save(s);
        });
    }

    @Transactional
    public ParentSession save(ParentSession session) {
        return sessionRepository.save(session);
    }
}
