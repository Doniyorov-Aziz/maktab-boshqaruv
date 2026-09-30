package uz.azizbek.maktabboshqaruv.bot.screens;

import uz.azizbek.maktabboshqaruv.bot.*;
import uz.azizbek.maktabboshqaruv.entity.BotSetting;
import uz.azizbek.maktabboshqaruv.entity.NotificationSettings;
import uz.azizbek.maktabboshqaruv.entity.ParentSession;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.service.BotSettingService;
import uz.azizbek.maktabboshqaruv.service.NotificationSettingsService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentAccessService;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineButton;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineKeyboardMarkup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * ⚙️ Sozlamalar — per-parent switches for every automatic message, the time of
 * the "tomorrow's schedule" message, quiet hours, and the bot language
 * (O'zbekcha · Ўзбекча · Русский).
 */
@Component
public class SettingsScreen implements Screen {

    record Toggle(String code, String key, Function<ParentSession, Boolean> get, BiConsumer<ParentSession, Boolean> set) {
    }

    static final List<Toggle> TOGGLES = List.of(
            new Toggle("abs", "set.item.absence", ParentSession::getNotifyAbsence, ParentSession::setNotifyAbsence),
            new Toggle("gr", "set.item.grades", ParentSession::getNotifyGrades, ParentSession::setNotifyGrades),
            new Toggle("low", "set.item.low_grade", ParentSession::getNotifyLowGrade, ParentSession::setNotifyLowGrade),
            new Toggle("ann", "set.item.announcements", ParentSession::getNotifyAnnouncements, ParentSession::setNotifyAnnouncements),
            new Toggle("tom", "set.item.tomorrow", ParentSession::getNotifyTomorrowSchedule, ParentSession::setNotifyTomorrowSchedule),
            new Toggle("wk", "set.item.weekly", ParentSession::getNotifyWeeklyReport, ParentSession::setNotifyWeeklyReport),
            new Toggle("ev", "set.item.events", ParentSession::getNotifyEventReminder, ParentSession::setNotifyEventReminder));

    @Autowired
    private ParentAccessService access;

    @Autowired
    private BotSettingService botSettingService;

    @Autowired
    private NotificationSettingsService notificationSettingsService;

    @Override
    public String code() {
        return "set";
    }

    @Override
    public boolean requiresStudent() {
        return false;
    }

    @Override
    public BotView render(BotContext ctx, CallbackData cb) {
        ParentSession s = ctx.session();
        String action = cb.get("a");
        if ("tg".equals(action)) {
            for (Toggle t : TOGGLES) {
                if (t.code().equals(cb.get("k"))) {
                    t.set().accept(s, !isOn(t.get().apply(s)));
                    access.save(s);
                }
            }
            return main(ctx);
        }
        if ("st".equals(action)) {
            s.setScheduleTime(parseTime(cb.get("t")));
            access.save(s);
            return times(ctx);
        }
        if ("qt".equals(action)) {
            String q = cb.get("q", "school");
            switch (q) {
                case "off" -> s.setQuietHoursEnabled(false);
                case "school" -> {
                    s.setQuietHoursEnabled(null);
                    s.setQuietStart(null);
                    s.setQuietEnd(null);
                }
                default -> {
                    s.setQuietHoursEnabled(true);
                    s.setQuietStart(LocalTime.of(Integer.parseInt(q.substring(0, 2)), 0));
                    s.setQuietEnd(LocalTime.of(Integer.parseInt(q.substring(2, 4)), 0));
                }
            }
            access.save(s);
            return times(ctx);
        }
        if ("lg".equals(action)) {
            s.setLanguage(BotI18n.normalize(cb.get("l")));
            access.save(s);
            BotContext fresh = new BotContext(ctx.chatId(), ctx.messageId(), s, ctx.student(), ctx.students(), ctx.from());
            // The bottom keyboard labels change with the language — reinstall them.
            return main(fresh).preface(fresh.t("set.language.saved"), Keyboards.main(fresh.lang()));
        }
        return switch (cb.get("v", "main")) {
            case "times" -> times(ctx);
            case "lang" -> language(ctx);
            default -> main(ctx);
        };
    }

    private static boolean isOn(Boolean value) {
        return !Boolean.FALSE.equals(value);
    }

    private BotView main(BotContext ctx) {
        ParentSession s = ctx.session();
        StringBuilder sb = new StringBuilder(ctx.crumb(ctx.t("sec.settings"))).append("\n\n").append(ctx.t("set.title")).append("\n\n");
        List<InlineButton> buttons = new ArrayList<>();
        for (Toggle t : TOGGLES) {
            boolean on = isOn(t.get().apply(s));
            sb.append(on ? ctx.t("set.on") : ctx.t("set.off")).append(" ").append(ctx.t(t.key())).append("\n");
            buttons.add(ctx.btn((on ? ctx.t("set.on") : ctx.t("set.off")) + " " + ctx.t(t.key()),
                    CallbackData.of("set").with("a", "tg").with("k", t.code())));
        }
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .grid(buttons, 2)
                .row(ctx.btn(ctx.t("set.btn.times"), CallbackData.of("set").with("v", "times")),
                        ctx.btn(ctx.t("set.btn.language"), CallbackData.of("set").with("v", "lang")))
                .row(ctx.navRow(null).toArray(new InlineButton[0]))
                .build();
        return BotView.of(sb.toString().stripTrailing(), kb).section("settings");
    }

    private BotView times(BotContext ctx) {
        ParentSession s = ctx.session();
        School school = ctx.student() == null ? null : ctx.student().getSchoolClass().getAcademicYear().getSchool();
        BotSetting bot = school == null ? null : botSettingService.getOrDefault(school);
        NotificationSettings ns = school == null ? null : notificationSettingsService.getOrDefault(school);
        LocalTime scheduleTime = s.getScheduleTime() != null ? s.getScheduleTime()
                : bot != null ? bot.getTomorrowScheduleTime() : LocalTime.of(19, 0);
        String quiet;
        if (s.getQuietHoursEnabled() == null) {
            quiet = ns == null ? ctx.t("set.quiet.off")
                    : ctx.t("set.quiet.school", "from", ns.getQuietHoursStart(), "to", ns.getQuietHoursEnd());
        } else if (!s.getQuietHoursEnabled()) {
            quiet = ctx.t("set.quiet.off");
        } else {
            quiet = ctx.t("set.quiet.range", "from", s.getQuietStart(), "to", s.getQuietEnd());
        }
        String text = ctx.crumb(ctx.t("sec.settings"), ctx.t("set.btn.times")) + "\n\n"
                + ctx.t("set.times.title", "time", scheduleTime, "quiet", quiet);

        List<InlineButton> hours = new ArrayList<>();
        for (int h : new int[]{18, 19, 20, 21}) {
            String label = String.format("%02d:00", h);
            hours.add(ctx.btn(label.equals(scheduleTime.toString()) ? "• " + label + " •" : label,
                    CallbackData.of("set").with("a", "st").with("t", String.format("%02d00", h))));
        }
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .row(ctx.btn(ctx.t("set.times.schedule_label"), CallbackData.of("noop")))
                .grid(hours, 4)
                .row(ctx.btn(ctx.t("set.times.quiet_label"), CallbackData.of("noop")))
                .row(quietBtn(ctx, "2107", "21:00–07:00"), quietBtn(ctx, "2207", "22:00–07:00"), quietBtn(ctx, "2307", "23:00–07:00"))
                .row(ctx.btn(ctx.t("set.quiet.btn_school"), CallbackData.of("set").with("a", "qt").with("q", "school")),
                        ctx.btn(ctx.t("set.quiet.btn_off"), CallbackData.of("set").with("a", "qt").with("q", "off")))
                .row(ctx.navRow(CallbackData.of("set")).toArray(new InlineButton[0]))
                .build();
        return BotView.of(text, kb).section("settings");
    }

    private InlineButton quietBtn(BotContext ctx, String code, String label) {
        return ctx.btn(label, CallbackData.of("set").with("a", "qt").with("q", code));
    }

    private BotView language(BotContext ctx) {
        String text = ctx.crumb(ctx.t("sec.settings"), ctx.t("set.btn.language")) + "\n\n" + ctx.t("set.language.title");
        List<InlineButton> langs = new ArrayList<>();
        for (String l : BotI18n.LANGUAGES) {
            String label = ctx.t("lang." + l);
            langs.add(ctx.btn(l.equals(ctx.lang()) ? "✅ " + label : label, CallbackData.of("set").with("a", "lg").with("l", l)));
        }
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .grid(langs, 1)
                .row(ctx.navRow(CallbackData.of("set")).toArray(new InlineButton[0]))
                .build();
        return BotView.of(text, kb).section("settings");
    }

    private static LocalTime parseTime(String hhmm) {
        try {
            return LocalTime.of(Integer.parseInt(hhmm.substring(0, 2)), Integer.parseInt(hhmm.substring(2, 4)));
        } catch (Exception e) {
            return null;
        }
    }
}
