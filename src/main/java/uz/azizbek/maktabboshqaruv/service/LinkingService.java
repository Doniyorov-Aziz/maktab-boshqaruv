package uz.azizbek.maktabboshqaruv.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.azizbek.maktabboshqaruv.entity.ParentTelegramLink;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.repository.ParentTelegramLinkRepository;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import uz.azizbek.maktabboshqaruv.telegram.LinkCodeGenerator;
import uz.azizbek.maktabboshqaruv.telegram.PhoneNormalizer;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Linking a Telegram chat (a parent) to students: by the deep-link code, or by
 * the parent's own shared phone number. A wrong code never reveals whether any
 * code exists, and repeated wrong codes from one chat are throttled.
 */
@Service
public class LinkingService {

    private static final Logger log = LoggerFactory.getLogger(LinkingService.class);

    public static final int MAX_FAILED_CODES = 5;
    static final long FAILED_CODE_WINDOW_MS = 10 * 60 * 1000;

    /** errorKey is an i18n key ("link.code_not_found", ...) or null on success. */
    public record Result(String errorKey, List<Student> linked) {
        public boolean ok() {
            return errorKey == null;
        }

        static Result error(String key) {
            return new Result(key, List.of());
        }
    }

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ParentTelegramLinkRepository linkRepository;

    @Autowired
    private Clock clock;

    private final Map<Long, Deque<Long>> failedCodeAttempts = new ConcurrentHashMap<>();

    @Transactional
    public Result byCode(long chatId, TelegramModels.User from, String code) {
        if (isRateLimited(chatId)) return Result.error("link.rate_limited");
        Student student = LinkCodeGenerator.looksValid(code)
                ? studentRepository.findByTelegramLinkCode(code.trim()).orElse(null)
                : null;
        if (student == null) {
            recordFailedAttempt(chatId);
            // Same answer for malformed, unknown and revoked codes — nothing to enumerate.
            return Result.error("link.code_not_found");
        }
        link(student, chatId, from);
        return new Result(null, List.of(student));
    }

    @Transactional
    public Result byContact(long chatId, TelegramModels.Message message) {
        TelegramModels.Contact contact = message.contact();
        // Only the sender's own number (sent via the request_contact button) is trusted —
        // a forwarded contact card would let anyone claim someone else's children.
        if (contact.userId() == null || !contact.userId().equals(message.from().id())) {
            return Result.error("link.not_own_contact");
        }
        String normalized = PhoneNormalizer.normalize(contact.phoneNumber());
        List<Student> matches = normalized == null || normalized.length() < 9 ? List.of()
                : studentRepository.findByGuardianPhoneLast9(normalized.substring(normalized.length() - 9)).stream()
                .filter(s -> normalized.equals(PhoneNormalizer.normalize(s.getGuardianPhone())))
                .toList();
        if (matches.isEmpty()) return Result.error("link.phone_not_found");
        for (Student s : matches) link(s, chatId, message.from());
        return new Result(null, matches);
    }

    /** /stop: every link of this chat goes inactive. */
    @Transactional
    public int stop(long chatId) {
        return linkRepository.deactivateByChatId(chatId);
    }

    /** "Farzandni uzish" from the bot — only this chat's own link. */
    @Transactional
    public void unlink(long chatId, Long studentId) {
        linkRepository.findByStudentIdAndChatId(studentId, chatId).ifPresent(l -> {
            l.setActive(false);
            linkRepository.save(l);
        });
    }

    private void link(Student student, long chatId, TelegramModels.User from) {
        ParentTelegramLink link = linkRepository.findByStudentIdAndChatId(student.getId(), chatId)
                .orElseGet(ParentTelegramLink::new);
        if (link.getId() == null || !Boolean.TRUE.equals(link.getActive())) {
            link.setLinkedAt(LocalDateTime.now(clock));
        }
        link.setStudent(student);
        link.setChatId(chatId);
        link.setTelegramUsername(from == null ? null : from.username());
        link.setFirstName(from == null ? null : from.firstName());
        link.setActive(true);
        linkRepository.save(link);
        failedCodeAttempts.remove(chatId);
        log.info("Telegram: ota-ona (chat={}) o'quvchi #{} ga ulandi", chatId, student.getId());
    }

    private boolean isRateLimited(long chatId) {
        Deque<Long> attempts = failedCodeAttempts.get(chatId);
        if (attempts == null) return false;
        long cutoff = clock.millis() - FAILED_CODE_WINDOW_MS;
        synchronized (attempts) {
            while (!attempts.isEmpty() && attempts.peekFirst() < cutoff) attempts.pollFirst();
            return attempts.size() >= MAX_FAILED_CODES;
        }
    }

    private void recordFailedAttempt(long chatId) {
        Deque<Long> attempts = failedCodeAttempts.computeIfAbsent(chatId, k -> new ArrayDeque<>());
        synchronized (attempts) {
            attempts.addLast(clock.millis());
        }
    }
}
