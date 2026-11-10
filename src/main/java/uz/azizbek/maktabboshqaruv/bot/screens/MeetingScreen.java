package uz.azizbek.maktabboshqaruv.bot.screens;

import org.springframework.stereotype.Component;
import uz.azizbek.maktabboshqaruv.bot.*;
import uz.azizbek.maktabboshqaruv.service.MeetingService.BookResult;
import uz.azizbek.maktabboshqaruv.service.parent.ParentModulesService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentModulesService.SlotItem;
import uz.azizbek.maktabboshqaruv.service.parent.ParentModulesService.TeacherFree;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineButton;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineKeyboardMarkup;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static uz.azizbek.maktabboshqaruv.bot.BotContext.e;

/**
 * 📅 O'qituvchi bilan uchrashuv.
 * <pre>
 *   mt            — my bookings (with ❌) and the child's teachers who have free times
 *   mt:t:&lt;id&gt;     — a teacher's free times (the next month), one button each
 *   mt:b:&lt;slot&gt;   — "Tasdiqlaysizmi?"
 *   mt:y:&lt;slot&gt;   — book it
 *   mt:c:&lt;slot&gt;   — cancel my booking
 * </pre>
 */
@Component
public class MeetingScreen implements Screen {

    static final int MAX_SLOTS = 24;

    private final ParentModulesService data;

    public MeetingScreen(ParentModulesService data) {
        this.data = data;
    }

    @Override
    public String code() {
        return "mt";
    }

    @Override
    public BotView render(BotContext ctx, CallbackData cb) {
        if (cb.get("y") != null) return book(ctx, cb.getLong("y"));
        if (cb.get("b") != null) return confirm(ctx, cb.getLong("b"));
        if (cb.get("t") != null) return teacher(ctx, cb.getLong("t"));
        String toast = null;
        if (cb.get("c") != null) toast = data.cancel(ctx.chatId(), cb.getLong("c")) ? ctx.t("mt.toast.cancelled") : ctx.t("mt.toast.cannot_cancel");
        BotView v = main(ctx);
        return toast == null ? v : v.toast(toast);
    }

    private BotView main(BotContext ctx) {
        StringBuilder sb = new StringBuilder(ctx.crumb(ctx.t("sec.more"), ctx.t("sec.meetings"))).append("\n")
                .append(ctx.childHeader()).append("\n");
        List<SlotItem> mine = data.myBookings(ctx.chatId());
        InlineKeyboardMarkup.Builder kb = InlineKeyboardMarkup.builder();
        if (!mine.isEmpty()) {
            sb.append(ctx.t("mt.mine")).append("\n");
            for (SlotItem m : mine) {
                sb.append("• <b>").append(ctx.dateShort(m.date())).append(", ").append(m.start()).append("</b> — ")
                        .append(e(m.teacher()));
                if (m.childName() != null && ctx.students().size() > 1) sb.append(" (").append(e(m.childName())).append(")");
                if (m.place() != null) sb.append(" · 📍 ").append(e(m.place()));
                sb.append("\n");
                kb.row(ctx.btn(ctx.t("mt.btn.cancel", "date", ctx.dateShort(m.date()), "time", m.start()),
                        CallbackData.of("mt").with("c", m.id())));
            }
            sb.append("\n");
        }
        List<TeacherFree> teachers = data.meetingTeachers(ctx.student());
        if (teachers.isEmpty()) {
            sb.append(ctx.t("mt.no_free"));
        } else {
            sb.append(ctx.t("mt.pick_teacher"));
            for (TeacherFree t : teachers) {
                String label = (t.classTeacher() ? "⭐ " : "👩‍🏫 ") + t.name() + " (" + t.free() + ")";
                kb.row(ctx.btn(label, CallbackData.of("mt").with("t", t.employeeId())));
            }
        }
        kb.row(ctx.switchRow("mt").toArray(new InlineButton[0]));
        kb.row(ctx.navRow(CallbackData.of("more")).toArray(new InlineButton[0]));
        return BotView.of(sb.toString().stripTrailing(), kb.build()).section("meetings");
    }

    private BotView teacher(BotContext ctx, Long teacherId) {
        List<SlotItem> free = data.freeSlots(ctx.student(), teacherId);
        if (free.isEmpty()) return main(ctx).toast(ctx.t("mt.toast.no_free"));
        String teacher = free.get(0).teacher();
        StringBuilder sb = new StringBuilder(ctx.crumb(ctx.t("sec.meetings"), e(teacher))).append("\n")
                .append(ctx.childHeader()).append("\n").append(ctx.t("mt.pick_time")).append("\n");
        InlineKeyboardMarkup.Builder kb = InlineKeyboardMarkup.builder();
        LocalDate day = null;
        List<InlineButton> row = new ArrayList<>();
        int shown = 0;
        for (SlotItem s : free) {
            if (shown++ >= MAX_SLOTS) break;
            if (!s.date().equals(day)) {
                if (!row.isEmpty()) kb.row(row.toArray(new InlineButton[0]));
                row = new ArrayList<>();
                day = s.date();
                sb.append("\n📅 <b>").append(ctx.dateFull(day)).append("</b>");
                if (s.place() != null) sb.append(" · 📍 ").append(e(s.place()));
            }
            row.add(ctx.btn(ctx.dateShort(s.date()) + " " + s.start(), CallbackData.of("mt").with("b", s.id())));
            if (row.size() == 3) {
                kb.row(row.toArray(new InlineButton[0]));
                row = new ArrayList<>();
            }
        }
        if (!row.isEmpty()) kb.row(row.toArray(new InlineButton[0]));
        kb.row(ctx.navRow(CallbackData.of("mt")).toArray(new InlineButton[0]));
        return BotView.of(sb.toString().stripTrailing(), kb.build()).section("meetings");
    }

    private BotView confirm(BotContext ctx, Long slotId) {
        Optional<SlotItem> s = data.slot(slotId);
        if (s.isEmpty()) return main(ctx).toast(ctx.t("mt.toast.taken"));
        SlotItem m = s.get();
        String text = ctx.crumb(ctx.t("sec.meetings"), ctx.t("mt.confirm_title")) + "\n" + ctx.childHeader() + "\n"
                + ctx.t("mt.confirm", "teacher", e(m.teacher()), "date", ctx.dateFull(m.date()),
                "time", m.start() + "–" + m.end(), "place", m.place() == null ? "" : "\n📍 " + e(m.place()));
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .row(ctx.btn(ctx.t("mt.btn.book"), CallbackData.of("mt").with("y", slotId)).styled("success"))
                .row(ctx.navRow(CallbackData.of("mt").with("t", m.employeeId())).toArray(new InlineButton[0]))
                .build();
        return BotView.of(text, kb).section("meetings");
    }

    private BotView book(BotContext ctx, Long slotId) {
        String name = ctx.from() == null ? null : ctx.from().firstName();
        BookResult r = data.book(ctx.chatId(), ctx.student(), slotId, name);
        String toast = switch (r) {
            case OK -> ctx.t("mt.toast.booked");
            case TAKEN -> ctx.t("mt.toast.taken");
            case TOO_LATE -> ctx.t("mt.toast.too_late");
            case ALREADY_WITH_TEACHER -> ctx.t("mt.toast.already");
            case NOT_ALLOWED -> ctx.t("common.denied");
        };
        return main(ctx).toast(toast);
    }
}
