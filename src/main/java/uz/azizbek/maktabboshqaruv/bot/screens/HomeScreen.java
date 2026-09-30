package uz.azizbek.maktabboshqaruv.bot.screens;

import uz.azizbek.maktabboshqaruv.bot.*;
import uz.azizbek.maktabboshqaruv.service.parent.ParentDataService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentViews.*;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineButton;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineKeyboardMarkup;
import uz.azizbek.maktabboshqaruv.telegram.TelegramProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static uz.azizbek.maktabboshqaruv.bot.BotContext.e;

/** 🏠 Bosh menyu — "Bugun": who, today's short summary, and every section one tap away. */
@Component
public class HomeScreen implements Screen {

    @Autowired
    private ParentDataService data;

    @Autowired
    private TelegramProperties telegramProperties;

    @Override
    public String code() {
        return "home";
    }

    @Override
    public BotView render(BotContext ctx, CallbackData cb) {
        TodaySummary t = data.todaySummary(ctx.student(), ctx.session());
        ChildInfo child = t.child();
        StringBuilder sb = new StringBuilder();
        sb.append(ctx.t("home.title", "date", ctx.dateFull(t.date()))).append("\n\n");
        sb.append(ctx.t("common.child_header", "name", e(child.fullName()), "class", e(child.className()))).append("\n");
        if (child.classTeacher() != null) {
            sb.append(ctx.t("home.class_teacher", "teacher", e(child.classTeacher()))).append("\n");
        }
        sb.append("\n").append(ctx.t("home.today")).append("\n");

        DaySchedule day = t.schedule();
        if (day.holidayTitle() != null) {
            sb.append(ctx.t("home.holiday", "title", e(day.holidayTitle()))).append("\n");
        } else if (day.lessons().isEmpty()) {
            sb.append(ctx.t("home.no_lessons")).append("\n");
        } else {
            LessonView first = day.lessons().get(0);
            LessonView last = day.lessons().get(day.lessons().size() - 1);
            sb.append(ctx.t("home.lessons", "count", day.lessons().size(), "start", first.start(), "end", last.end())).append("\n");
            sb.append(switch (t.attendanceState()) {
                case "PRESENT" -> ctx.t("home.att_present");
                case "LATE" -> ctx.t("home.att_late", "count", t.lateCount());
                case "ABSENT" -> ctx.t("home.att_absent", "count", t.absentCount());
                case "EXCUSED" -> ctx.t("home.att_excused");
                default -> ctx.t("home.att_none");
            }).append("\n");
        }
        if (t.gradesToday().isEmpty()) {
            sb.append(ctx.t("home.grades_none")).append("\n");
        } else {
            String list = t.gradesToday().stream()
                    .map(g -> SubjectIcons.grade(g.score()) + " <b>" + g.score() + "</b> " + e(g.subject()))
                    .collect(Collectors.joining(", "));
            sb.append(ctx.t("home.grades_today", "list", list)).append("\n");
        }
        if (t.newAnnouncements() > 0) {
            sb.append(ctx.t("home.new_announcements", "count", t.newAnnouncements())).append("\n");
        }
        if (t.nextEvent() != null) {
            sb.append(ctx.t("home.next_event", "title", e(t.nextEvent().title()),
                    "date", ctx.dateShort(t.nextEvent().startDate()))).append("\n");
        }
        sb.append("\n").append(ctx.t("home.pick_section"));

        InlineKeyboardMarkup.Builder kb = InlineKeyboardMarkup.builder();
        String webapp = telegramProperties.webappUrlIfValid();
        if (webapp != null) {
            kb.row(InlineButton.webApp(ctx.t("menu.webapp"), webapp));
        }
        kb.grid(List.of(
                ctx.btn(ctx.t("kb.schedule"), "sch"), ctx.btn(ctx.t("kb.attendance"), "att"),
                ctx.btn(ctx.t("kb.grades"), "gr"), ctx.btn(ctx.t("kb.report"), "rep"),
                ctx.btn(ctx.t("menu.behavior"), "beh"), ctx.btn(ctx.t("kb.announcements"), "ann"),
                ctx.btn(ctx.t("kb.events"), "ev"), ctx.btn(ctx.t("kb.teachers"), "tch"),
                ctx.btn(ctx.t("kb.write"), "msg"), ctx.btn(ctx.t("menu.absence"), "abs"),
                ctx.btn(ctx.t("menu.school"), "info"), ctx.btn(ctx.t("kb.settings"), "set")), 2);
        kb.row(ctx.btn(ctx.t("kb.children"), "ch"));
        kb.row(ctx.switchRow("home").toArray(new InlineButton[0]));
        return BotView.of(sb.toString(), kb.build()).section("home");
    }
}
