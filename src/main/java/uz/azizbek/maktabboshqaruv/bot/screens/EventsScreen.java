package uz.azizbek.maktabboshqaruv.bot.screens;

import uz.azizbek.maktabboshqaruv.bot.*;
import uz.azizbek.maktabboshqaruv.service.parent.ParentDataService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentViews.EventView;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineButton;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineKeyboardMarkup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static uz.azizbek.maktabboshqaruv.bot.BotContext.e;

/** 🗓 Tadbirlar — holidays, exams, parent meetings and vacations of the next 60 days (paged). */
@Component
public class EventsScreen implements Screen {

    private static final int PAGE = 6;

    @Autowired
    private ParentDataService data;

    @Override
    public String code() {
        return "ev";
    }

    @Override
    public BotView render(BotContext ctx, CallbackData cb) {
        List<EventView> all = data.upcomingEvents(ctx.student(), 60);
        int pages = Math.max(1, (all.size() + PAGE - 1) / PAGE);
        int page = Math.max(0, Math.min(cb.getInt("p", 0), pages - 1));
        LocalDate today = data.today();
        StringBuilder sb = new StringBuilder(ctx.crumb(ctx.t("sec.events"))).append("\n")
                .append(ctx.childHeader()).append("\n")
                .append(ctx.t("ev.title")).append("\n\n");
        if (all.isEmpty()) {
            sb.append(ctx.t("ev.empty"));
        } else {
            for (EventView ev : all.subList(page * PAGE, Math.min(all.size(), (page + 1) * PAGE))) {
                String date = ev.endDate() != null && !ev.endDate().equals(ev.startDate())
                        ? ctx.t("ev.range", "from", ctx.dateShort(ev.startDate()), "to", ctx.dateShort(ev.endDate()))
                        : ctx.dateFull(ev.startDate());
                long days = ChronoUnit.DAYS.between(today, ev.startDate());
                String when = days <= 0 ? ctx.t("ev.today") : days == 1 ? ctx.t("ev.tomorrow") : ctx.t("ev.days_left", "n", days);
                sb.append(ctx.t("ev.item", "emoji", SubjectIcons.event(ev.type()), "title", e(ev.title()),
                        "date", date, "when", when)).append("\n");
            }
            sb.append("\n").append(ctx.t("ev.remind_note"));
        }
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .row(ctx.pagerRow(CallbackData.of("ev"), page, pages).toArray(new InlineButton[0]))
                .row(ctx.navRow(null).toArray(new InlineButton[0]))
                .build();
        return BotView.of(sb.toString().stripTrailing(), kb).section("events");
    }
}
