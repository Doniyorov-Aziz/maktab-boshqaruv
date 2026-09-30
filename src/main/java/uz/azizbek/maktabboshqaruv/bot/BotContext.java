package uz.azizbek.maktabboshqaruv.bot;

import uz.azizbek.maktabboshqaruv.entity.ParentSession;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.telegram.MessageFormatter;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineButton;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Everything one screen render needs: the chat, the parent's session and
 * language, and the child being viewed — already verified to be linked to
 * this chat. Also the small helpers every page shares (breadcrumb, child
 * header, Back / Home row).
 */
public final class BotContext {

    private final long chatId;
    private final Long messageId;
    private final ParentSession session;
    private final Student student;
    private final List<Student> students;
    private final TelegramModels.User from;
    private final BotI18n i18n = BotI18n.get();

    public BotContext(long chatId, Long messageId, ParentSession session, Student student, List<Student> students,
                      TelegramModels.User from) {
        this.chatId = chatId;
        this.messageId = messageId;
        this.session = session;
        this.student = student;
        this.students = students;
        this.from = from;
    }

    public long chatId() {
        return chatId;
    }

    public Long messageId() {
        return messageId;
    }

    public ParentSession session() {
        return session;
    }

    public Student student() {
        return student;
    }

    public List<Student> students() {
        return students;
    }

    public TelegramModels.User from() {
        return from;
    }

    public String lang() {
        return session.lang();
    }

    public BotI18n i18n() {
        return i18n;
    }

    public String t(String key, Object... args) {
        return i18n.t(lang(), key, args);
    }

    public static String e(String s) {
        return MessageFormatter.escape(s);
    }

    public String dateFull(LocalDate d) {
        return i18n.dateFull(lang(), d);
    }

    public String dateShort(LocalDate d) {
        return i18n.dateShort(lang(), d);
    }

    /** "🏠 › ✅ Davomat › Sentabr" — shows where the parent is. */
    public String crumb(String... parts) {
        StringBuilder sb = new StringBuilder(t("sec.home"));
        for (String p : parts) {
            if (p != null && !p.isBlank()) sb.append(" › ").append(p);
        }
        return sb.toString();
    }

    /** "👤 Aziza Doniyorova · 11-B sinf" line, shown only when several children are linked. */
    public String childHeader() {
        if (student == null || students.size() < 2) return "";
        return t("common.child_header", "name", e(student.getFirstName() + " " + student.getLastName()),
                "class", e(student.getSchoolClass().getGradeNumber() + "-" + student.getSchoolClass().getSectionLetter())) + "\n";
    }

    public InlineButton btn(String text, CallbackData data) {
        return InlineButton.callback(text, data.toString());
    }

    public InlineButton btn(String text, String screen) {
        return InlineButton.callback(text, CallbackData.of(screen).toString());
    }

    /** Bottom row of every page: ⬅️ Orqaga · 🏠 Bosh menyu. */
    public List<InlineButton> navRow(CallbackData back) {
        List<InlineButton> row = new ArrayList<>();
        if (back != null) row.add(btn(t("common.back"), back));
        row.add(btn(t("common.home"), "home"));
        return row;
    }

    /** "🔄 Farzandni almashtirish" — only with more than one child; returns to {@code screen} after choosing. */
    public List<InlineButton> switchRow(String screen) {
        if (students.size() < 2) return List.of();
        return List.of(btn(t("common.switch_child"), CallbackData.of("ch").with("r", screen)));
    }

    /** "◀️ 1/3 ▶️" pager row; empty when everything fits on one page. */
    public List<InlineButton> pagerRow(CallbackData base, int page, int pages) {
        if (pages <= 1) return List.of();
        List<InlineButton> row = new ArrayList<>();
        row.add(page > 0 ? btn(t("common.prev"), base.with("p", page - 1)) : btn(" ", CallbackData.of("noop")));
        row.add(btn((page + 1) + "/" + pages, CallbackData.of("noop")));
        row.add(page < pages - 1 ? btn(t("common.next"), base.with("p", page + 1)) : btn(" ", CallbackData.of("noop")));
        return row;
    }
}
