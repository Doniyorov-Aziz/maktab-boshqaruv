package uz.azizbek.maktabboshqaruv.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.azizbek.maktabboshqaruv.entity.ParentTelegramLink;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.repository.ParentTelegramLinkRepository;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import uz.azizbek.maktabboshqaruv.telegram.LinkCodeGenerator;
import uz.azizbek.maktabboshqaruv.telegram.PhoneNormalizer;
import uz.azizbek.maktabboshqaruv.telegram.TelegramClient;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels;
import uz.azizbek.maktabboshqaruv.telegram.TokenMasker;
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

import static uz.azizbek.maktabboshqaruv.telegram.MessageFormatter.escape;

/**
 * The parent-facing side of the bot: linking (deep-link code or shared
 * phone number) and the /farzandlarim, /stop, /yordam commands. Only private
 * chats are served, and a wrong code never reveals whether any code exists.
 */
@Service
public class TelegramBotService {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotService.class);

    static final int MAX_FAILED_CODES = 5;
    static final long FAILED_CODE_WINDOW_MS = 10 * 60 * 1000;

    static final String HELP = """
            ℹ️ <b>Buyruqlar</b>
            /start — farzandingizni ulash
            /farzandlarim — ulangan farzandlar ro'yxati
            /stop — xabarnomalarni to'xtatish
            /yordam — shu yordam

            Farzandingizni ulash uchun sinf rahbari bergan QR kod yoki havolani oching, \
            yoki /start bosib telefon raqamingizni ulashing.""";

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ParentTelegramLinkRepository linkRepository;

    @Autowired
    private TelegramClient telegramClient;

    @Autowired
    private Clock clock;

    private final Map<Long, Deque<Long>> failedCodeAttempts = new ConcurrentHashMap<>();

    @Transactional
    public void handleUpdate(TelegramModels.Update update) {
        TelegramModels.Message message = update.message();
        if (message == null || message.chat() == null || message.from() == null) return;
        if (!"private".equals(message.chat().type())) return;
        if (Boolean.TRUE.equals(message.from().isBot())) return;

        long chatId = message.chat().id();

        if (message.contact() != null) {
            handleContact(chatId, message);
            return;
        }

        String text = message.text() == null ? "" : message.text().trim();
        String command = text.split("\\s+", 2)[0];
        int at = command.indexOf('@');
        if (at > 0) command = command.substring(0, at); // "/start@MyBot"
        String argument = text.contains(" ") ? text.substring(text.indexOf(' ') + 1).trim() : "";

        switch (command.toLowerCase()) {
            case "/start" -> {
                if (argument.isEmpty()) {
                    reply(chatId, welcome(), TelegramModels.shareContactKeyboard());
                } else {
                    handleCode(chatId, message.from(), argument);
                }
            }
            case "/farzandlarim" -> reply(chatId, childrenList(chatId), null);
            case "/stop" -> reply(chatId, stop(chatId), TelegramModels.removeKeyboard());
            default -> reply(chatId, HELP, null); // /yordam, /help and anything unknown
        }
    }

    void handleCode(long chatId, TelegramModels.User from, String code) {
        if (isRateLimited(chatId)) {
            reply(chatId, "⏳ Juda ko'p noto'g'ri urinish. 10 daqiqadan so'ng qayta urinib ko'ring.", null);
            return;
        }
        Student student = LinkCodeGenerator.looksValid(code)
                ? studentRepository.findByTelegramLinkCode(code).orElse(null)
                : null;
        if (student == null) {
            recordFailedAttempt(chatId);
            // Same answer for malformed, unknown and revoked codes — nothing to enumerate.
            reply(chatId, "❌ Kod topilmadi. Havolani sinf rahbaringizdan qayta so'rang yoki /start bosib raqamingizni ulashing.", null);
            return;
        }

        link(student, chatId, from);
        reply(chatId, "✅ Farzandingiz <b>" + escape(fullName(student)) + "</b>, " + escape(className(student))
                + " sinfiga ulandingiz.\n\nEndi davomat, baholar va e'lonlar haqidagi xabarlar shu yerga keladi."
                + "\n/farzandlarim — ro'yxat · /stop — to'xtatish", TelegramModels.removeKeyboard());
    }

    void handleContact(long chatId, TelegramModels.Message message) {
        TelegramModels.Contact contact = message.contact();
        // Only the sender's own number (sent via the request_contact button) is trusted —
        // a forwarded contact card would let anyone claim someone else's children.
        if (contact.userId() == null || !contact.userId().equals(message.from().id())) {
            reply(chatId, "⚠️ Iltimos, faqat <b>o'zingizning</b> raqamingizni «📱 Raqamni ulashish» tugmasi orqali yuboring.",
                    TelegramModels.shareContactKeyboard());
            return;
        }

        String normalized = PhoneNormalizer.normalize(contact.phoneNumber());
        List<Student> matches = normalized == null || normalized.length() < 9 ? List.of()
                : studentRepository.findByGuardianPhoneLast9(normalized.substring(normalized.length() - 9)).stream()
                .filter(s -> normalized.equals(PhoneNormalizer.normalize(s.getGuardianPhone())))
                .toList();

        if (matches.isEmpty()) {
            reply(chatId, "😕 Bu raqam maktab bazasida topilmadi.\nSinf rahbaridan QR kod yoki havola so'rang.",
                    TelegramModels.removeKeyboard());
            return;
        }

        StringBuilder sb = new StringBuilder("✅ Raqamingiz bo'yicha ").append(matches.size())
                .append(matches.size() == 1 ? " ta farzand ulandi:\n" : " ta farzand ulandi:\n");
        for (Student s : matches) {
            link(s, chatId, message.from());
            sb.append("• <b>").append(escape(fullName(s))).append("</b> — ").append(escape(className(s))).append("\n");
        }
        sb.append("\nEndi davomat, baholar va e'lonlar haqidagi xabarlar shu yerga keladi.");
        reply(chatId, sb.toString(), TelegramModels.removeKeyboard());
    }

    private void link(Student student, long chatId, TelegramModels.User from) {
        ParentTelegramLink link = linkRepository.findByStudentIdAndChatId(student.getId(), chatId)
                .orElseGet(ParentTelegramLink::new);
        if (link.getId() == null || !Boolean.TRUE.equals(link.getActive())) {
            link.setLinkedAt(LocalDateTime.now(clock));
        }
        link.setStudent(student);
        link.setChatId(chatId);
        link.setTelegramUsername(from.username());
        link.setFirstName(from.firstName());
        link.setActive(true);
        linkRepository.save(link);
        failedCodeAttempts.remove(chatId);
        log.info("Telegram: ota-ona (chat={}) o'quvchi #{} ga ulandi", chatId, student.getId());
    }

    private String childrenList(long chatId) {
        List<ParentTelegramLink> links = linkRepository.findByChatIdAndActiveTrue(chatId);
        if (links.isEmpty()) {
            return "Hali hech bir farzand ulanmagan. Ulanish uchun /start bosing.";
        }
        StringBuilder sb = new StringBuilder("👨‍👩‍👧 <b>Ulangan farzandlar</b>\n");
        int i = 1;
        for (ParentTelegramLink l : links) {
            Student s = l.getStudent();
            sb.append(i++).append(". <b>").append(escape(fullName(s))).append("</b> — ")
                    .append(escape(className(s))).append(" · ")
                    .append(escape(s.getSchoolClass().getAcademicYear().getSchool().getName())).append("\n");
        }
        return sb.toString().stripTrailing();
    }

    private String stop(long chatId) {
        int count = linkRepository.deactivateByChatId(chatId);
        if (count == 0) {
            return "Sizda faol obuna yo'q. Ulanish uchun /start bosing.";
        }
        return "🔕 Obuna bekor qilindi (" + count + " ta farzand). Endi xabar yuborilmaydi.\nQayta ulanish uchun /start bosing.";
    }

    private String welcome() {
        return """
                👋 Assalomu alaykum! Bu — <b>Maktab Boshqaruv</b> boti.
                Farzandingizning davomati, baholari va maktab e'lonlari shu yerga keladi.

                Ulanish uchun pastdagi «📱 Raqamni ulashish» tugmasini bosing — maktabda qayd etilgan \
                raqamingiz bo'yicha farzandingiz topiladi. Yoki sinf rahbari bergan QR kodni skanerlang.""";
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

    private void reply(long chatId, String html, Object keyboard) {
        try {
            telegramClient.sendMessage(chatId, html, keyboard);
        } catch (Exception e) {
            log.warn("Telegram javobini yuborib bo'lmadi (chat={}): {}", chatId, TokenMasker.mask(e.getMessage()));
        }
    }

    private static String fullName(Student s) {
        return s.getFirstName() + " " + s.getLastName();
    }

    private static String className(Student s) {
        return s.getSchoolClass().getGradeNumber() + "-" + s.getSchoolClass().getSectionLetter();
    }
}
