package uz.azizbek.maktabboshqaruv.bot.screens;

import uz.azizbek.maktabboshqaruv.bot.*;
import uz.azizbek.maktabboshqaruv.entity.AbsenceReason;
import uz.azizbek.maktabboshqaruv.service.AbsenceRequestService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentAccessService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentDataService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentViews.AbsenceView;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineButton;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineKeyboardMarkup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static uz.azizbek.maktabboshqaruv.bot.BotContext.e;

/**
 * 🤒 Sababli ariza — a short guided flow:
 * start date (d) → number of days (n) → reason (r) → comment (typed or skipped)
 * → optional photo of a doctor's note → confirm → the class teacher decides in the admin panel.
 */
@Component
public class AbsenceScreen implements Screen, InputHandler {

    static final String COMMENT = "ABS_COMMENT";
    static final String PHOTO = "ABS_PHOTO";
    static final String CONFIRM = "ABS_CONFIRM";
    private static final Map<String, AbsenceReason> REASONS = Map.of(
            "I", AbsenceReason.ILLNESS, "F", AbsenceReason.FAMILY, "O", AbsenceReason.OTHER);

    @Autowired
    private ParentDataService data;

    @Autowired
    private ParentAccessService access;

    @Autowired
    private AbsenceRequestService requests;

    @Override
    public String code() {
        return "abs";
    }

    @Override
    public List<String> pendingPrefixes() {
        return List.of("ABS_");
    }

    @Override
    public BotView render(BotContext ctx, CallbackData cb) {
        String action = cb.get("a");
        if ("x".equals(action)) {
            clear(ctx);
            return start(ctx).toast(ctx.t("msg.cancelled"));
        }
        if ("skip".equals(action)) return skip(ctx);
        if ("send".equals(action)) return send(ctx);

        String date = cb.get("d");
        if (date == null) return start(ctx);
        LocalDate from;
        try {
            from = LocalDate.parse(date);
        } catch (Exception ex) {
            return start(ctx);
        }
        String days = cb.get("n");
        if (days == null) return daysQuestion(ctx, from);
        String reason = cb.get("r");
        if (reason == null || !REASONS.containsKey(reason)) return reasonQuestion(ctx, from, days);

        // All three picked: remember them and ask for a comment.
        ctx.session().setPendingAction(COMMENT);
        ctx.session().setPendingData(new PendingData().put("sid", ctx.student().getId()).put("from", from)
                .put("days", days).put("reason", REASONS.get(reason).name()).toString());
        access.save(ctx.session());
        return prompt(ctx, ctx.t("abs.comment_prompt"));
    }

    private BotView start(BotContext ctx) {
        StringBuilder sb = new StringBuilder(ctx.crumb(ctx.t("sec.absence"))).append("\n").append(ctx.childHeader()).append("\n")
                .append(ctx.t("abs.title"));
        List<AbsenceView> history = data.absenceRequests(ctx.student(), ctx.chatId());
        if (!history.isEmpty()) {
            sb.append("\n\n").append(ctx.t("abs.history_title")).append("\n");
            for (AbsenceView a : history) {
                sb.append(ctx.t("abs.history_item", "status", ctx.t("abs.status." + a.status()),
                        "from", ctx.dateShort(a.from()), "to", ctx.dateShort(a.to()),
                        "reason", ctx.t("abs.reason." + a.reason()))).append("\n");
            }
        }
        LocalDate today = data.today();
        List<InlineButton> dates = new ArrayList<>();
        dates.add(ctx.btn(ctx.t("abs.today"), CallbackData.of("abs").with("d", today)));
        dates.add(ctx.btn(ctx.t("abs.tomorrow"), CallbackData.of("abs").with("d", today.plusDays(1))));
        for (LocalDate d = today.plusDays(2); dates.size() < 6; d = d.plusDays(1)) {
            if (d.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
            dates.add(ctx.btn(ctx.dateShort(d) + ", " + ctx.i18n().dowShort(ctx.lang(), d.getDayOfWeek()), CallbackData.of("abs").with("d", d)));
        }
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .grid(dates, 2)
                .row(ctx.switchRow("abs").toArray(new InlineButton[0]))
                .row(ctx.navRow(null).toArray(new InlineButton[0]))
                .build();
        return BotView.of(sb.toString().stripTrailing(), kb).section("absence");
    }

    private BotView daysQuestion(BotContext ctx, LocalDate from) {
        List<InlineButton> buttons = new ArrayList<>();
        for (int n : new int[]{1, 2, 3, 5, 7}) {
            buttons.add(ctx.btn(ctx.t("abs.days_btn", "n", n), CallbackData.of("abs").with("d", from).with("n", n)));
        }
        String text = ctx.crumb(ctx.t("sec.absence")) + "\n\n" + ctx.t("abs.days_question", "date", ctx.dateFull(from));
        return BotView.of(text, InlineKeyboardMarkup.builder().grid(buttons, 3)
                .row(ctx.navRow(CallbackData.of("abs")).toArray(new InlineButton[0])).build()).section("absence");
    }

    private BotView reasonQuestion(BotContext ctx, LocalDate from, String days) {
        CallbackData base = CallbackData.of("abs").with("d", from).with("n", days);
        String text = ctx.crumb(ctx.t("sec.absence")) + "\n\n" + ctx.t("abs.reason_question");
        return BotView.of(text, InlineKeyboardMarkup.builder()
                .row(ctx.btn(ctx.t("abs.reason.ILLNESS"), base.with("r", "I")))
                .row(ctx.btn(ctx.t("abs.reason.FAMILY"), base.with("r", "F")))
                .row(ctx.btn(ctx.t("abs.reason.OTHER"), base.with("r", "O")))
                .row(ctx.navRow(CallbackData.of("abs").with("d", from)).toArray(new InlineButton[0])).build()).section("absence");
    }

    private BotView prompt(BotContext ctx, String text) {
        return BotView.of(ctx.crumb(ctx.t("sec.absence")) + "\n\n" + text, InlineKeyboardMarkup.builder()
                .row(ctx.btn(ctx.t("common.skip"), CallbackData.of("abs").with("a", "skip")))
                .row(ctx.btn(ctx.t("common.cancel"), CallbackData.of("abs").with("a", "x"))).build()).section("absence");
    }

    private BotView skip(BotContext ctx) {
        String step = ctx.session().getPendingAction();
        if (COMMENT.equals(step)) {
            ctx.session().setPendingAction(PHOTO);
            access.save(ctx.session());
            return prompt(ctx, ctx.t("abs.photo_prompt"));
        }
        if (PHOTO.equals(step)) return confirm(ctx);
        return start(ctx);
    }

    private BotView confirm(BotContext ctx) {
        ctx.session().setPendingAction(CONFIRM);
        access.save(ctx.session());
        PendingData p = PendingData.parse(ctx.session().getPendingData());
        LocalDate from = LocalDate.parse(p.get("from"));
        int days = Integer.parseInt(p.get("days"));
        uz.azizbek.maktabboshqaruv.entity.Student child = access.requireLinked(ctx.chatId(), p.getLong("sid"));
        String text = ctx.crumb(ctx.t("sec.absence")) + "\n\n" + ctx.t("abs.confirm",
                "name", e(child.getFirstName() + " " + child.getLastName()),
                "from", ctx.dateFull(from), "to", ctx.dateFull(from.plusDays(days - 1)), "days", days,
                "reason", ctx.t("abs.reason." + p.get("reason")),
                "comment", p.get("comment") == null ? "" : ctx.t("abs.confirm_comment", "text", e(p.get("comment"))),
                "photo", p.get("photo") == null ? "" : ctx.t("abs.confirm_photo"));
        return BotView.of(text, InlineKeyboardMarkup.builder()
                .row(ctx.btn(ctx.t("abs.btn.send"), CallbackData.of("abs").with("a", "send")))
                .row(ctx.btn(ctx.t("common.cancel"), CallbackData.of("abs").with("a", "x"))).build()).section("absence");
    }

    private BotView send(BotContext ctx) {
        if (!CONFIRM.equals(ctx.session().getPendingAction())) return start(ctx);
        PendingData p = PendingData.parse(ctx.session().getPendingData());
        requests.create(access.requireLinked(ctx.chatId(), p.getLong("sid")), ctx.chatId(), ctx.from(),
                LocalDate.parse(p.get("from")), Integer.parseInt(p.get("days")), AbsenceReason.valueOf(p.get("reason")),
                p.get("comment"), p.get("photo"));
        clear(ctx);
        BotView page = start(ctx);
        return BotView.of(ctx.t("abs.sent") + "\n\n" + page.text(), page.keyboard()).section("absence");
    }

    @Override
    public BotView handleInput(BotContext ctx, String text, String photoFileId) {
        String step = ctx.session().getPendingAction();
        PendingData p = PendingData.parse(ctx.session().getPendingData());
        if (COMMENT.equals(step)) {
            if (text == null || text.isBlank()) return prompt(ctx, ctx.t("abs.comment_prompt")).asNewMessage();
            p.put("comment", text.length() > 500 ? text.substring(0, 500) : text.strip());
            if (photoFileId != null) p.put("photo", photoFileId);
            ctx.session().setPendingData(p.toString());
            ctx.session().setPendingAction(photoFileId != null ? CONFIRM : PHOTO);
            access.save(ctx.session());
            return (photoFileId != null ? confirm(ctx) : prompt(ctx, ctx.t("abs.photo_prompt"))).asNewMessage();
        }
        if (PHOTO.equals(step)) {
            if (photoFileId == null) return prompt(ctx, ctx.t("abs.photo_prompt")).asNewMessage();
            p.put("photo", photoFileId);
            ctx.session().setPendingData(p.toString());
            access.save(ctx.session());
            return confirm(ctx).asNewMessage();
        }
        return confirm(ctx).asNewMessage();
    }

    private void clear(BotContext ctx) {
        ctx.session().setPendingAction(null);
        ctx.session().setPendingData(null);
        access.save(ctx.session());
    }
}
