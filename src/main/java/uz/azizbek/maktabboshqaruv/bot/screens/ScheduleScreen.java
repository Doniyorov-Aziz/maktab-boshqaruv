package uz.azizbek.maktabboshqaruv.bot.screens;

import uz.azizbek.maktabboshqaruv.bot.*;
import uz.azizbek.maktabboshqaruv.service.parent.ParentDataService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentViews.*;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineButton;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineKeyboardMarkup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static uz.azizbek.maktabboshqaruv.bot.BotContext.e;

/** 📅 Dars jadvali — tabs Bugun · Ertaga · Hafta; ▶️ marks the lesson in progress, breaks are shown. */
@Component
public class ScheduleScreen implements Screen {

    @Autowired
    private ParentDataService data;

    @Override
    public String code() {
        return "sch";
    }

    @Override
    public BotView render(BotContext ctx, CallbackData cb) {
        String tab = cb.get("t", "today");
        LocalDate today = data.today();
        StringBuilder sb = new StringBuilder();
        String tabLabel = ctx.t("sched.tab." + tab);
        sb.append(ctx.crumb(ctx.t("sec.schedule"), tabLabel)).append("\n").append(ctx.childHeader()).append("\n");

        if ("week".equals(tab)) {
            for (DaySchedule day : data.week(ctx.student(), today)) {
                sb.append(weekDay(ctx, day)).append("\n");
            }
        } else {
            LocalDate date = "tomorrow".equals(tab) ? today.plusDays(1) : today;
            sb.append(dayBlock(ctx, data.day(ctx.student(), date), "tomorrow".equals(tab)));
        }

        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .row(tab(ctx, "today", tab), tab(ctx, "tomorrow", tab), tab(ctx, "week", tab))
                .row(ctx.switchRow("sch").toArray(new InlineButton[0]))
                .row(ctx.navRow(null).toArray(new InlineButton[0]))
                .build();
        return BotView.of(sb.toString().stripTrailing(), kb).section("schedule");
    }

    private InlineButton tab(BotContext ctx, String name, String current) {
        String label = ctx.t("sched.tab." + name);
        return ctx.btn(name.equals(current) ? "• " + label + " •" : label, CallbackData.of("sch").with("t", name));
    }

    String dayBlock(BotContext ctx, DaySchedule day, boolean tomorrow) {
        StringBuilder sb = new StringBuilder(ctx.t("sched.day_header", "date", ctx.dateFull(day.date()))).append("\n\n");
        if (day.holidayTitle() != null) {
            return sb.append(ctx.t(tomorrow ? "sched.tomorrow_holiday" : "sched.holiday", "title", e(day.holidayTitle()))).toString();
        }
        if (day.lessons().isEmpty()) {
            return sb.append(ctx.t(day.weekend() ? "sched.weekend" : "sched.no_lessons")).toString();
        }
        boolean anyNow = false;
        for (LessonView l : day.lessons()) {
            if (l.breakBeforeMinutes() != null && l.breakBeforeMinutes() >= 5) {
                sb.append(ctx.t("sched.break", "minutes", l.breakBeforeMinutes())).append("\n");
            }
            anyNow |= l.now();
            sb.append(ctx.t("sched.lesson", "now", l.now() ? ctx.t("sched.now_mark") : "", "n", l.number(),
                    "start", l.start(), "end", l.end(), "emoji", SubjectIcons.subject(l.subject()),
                    "subject", e(l.subject()), "teacher", e(orDash(ctx, l.teacher())), "room", e(orDash(ctx, l.room()))))
                    .append("\n");
        }
        List<EventView> notable = day.events().stream()
                .filter(ev -> !"HOLIDAY".equals(ev.type()) && !"VACATION".equals(ev.type())).toList();
        if (!notable.isEmpty()) {
            sb.append("\n").append(ctx.t("sched.events_note", "list", notable.stream()
                    .map(ev -> SubjectIcons.event(ev.type()) + " " + e(ev.title())).collect(Collectors.joining(", "))));
        }
        if (anyNow) sb.append("\n").append(ctx.t("sched.now_note"));
        return sb.toString();
    }

    private String weekDay(BotContext ctx, DaySchedule day) {
        String dow = ctx.i18n().dowTitle(ctx.lang(), day.dayOfWeek()) + ", " + ctx.dateShort(day.date());
        if (day.holidayTitle() != null) {
            return "<b>" + dow + "</b>\n" + ctx.t("sched.holiday", "title", e(day.holidayTitle())) + "\n";
        }
        if (day.lessons().isEmpty()) {
            return "<b>" + dow + "</b>\n" + ctx.t("sched.weekend") + "\n";
        }
        StringBuilder sb = new StringBuilder(ctx.t("sched.week_day", "dow", dow, "count", day.lessons().size(),
                "start", day.lessons().get(0).start(), "end", day.lessons().get(day.lessons().size() - 1).end())).append("\n");
        for (LessonView l : day.lessons()) {
            sb.append(ctx.t("sched.week_line", "n", l.number(), "emoji", SubjectIcons.subject(l.subject()),
                    "subject", e(l.subject()))).append(l.now() ? " ▶️" : "").append("\n");
        }
        return sb.toString();
    }

    private static String orDash(BotContext ctx, String s) {
        return s == null || s.isBlank() ? ctx.t("common.none") : s;
    }
}
