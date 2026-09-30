package uz.azizbek.maktabboshqaruv.bot.screens;

import uz.azizbek.maktabboshqaruv.bot.*;
import uz.azizbek.maktabboshqaruv.service.parent.ParentAccessService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentDataService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentViews.AnnouncementView;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineButton;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineKeyboardMarkup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static uz.azizbek.maktabboshqaruv.bot.BotContext.e;

/** 📢 E'lonlar — paged list (🆕 unread, 🔴 important); tapping one opens the full text and marks it read. */
@Component
public class AnnouncementsScreen implements Screen {

    private static final int PAGE = 6;

    @Autowired
    private ParentDataService data;

    @Autowired
    private ParentAccessService access;

    @Override
    public String code() {
        return "ann";
    }

    @Override
    public BotView render(BotContext ctx, CallbackData cb) {
        Long id = cb.getLong("id");
        int page = cb.getInt("p", 0);
        return id != null ? detail(ctx, id, page) : list(ctx, page);
    }

    private BotView list(BotContext ctx, int page) {
        List<AnnouncementView> all = data.announcements(ctx.student(), ctx.session());
        int pages = Math.max(1, (all.size() + PAGE - 1) / PAGE);
        page = Math.max(0, Math.min(page, pages - 1));
        StringBuilder sb = new StringBuilder(ctx.crumb(ctx.t("sec.announcements"))).append("\n")
                .append(ctx.childHeader()).append("\n")
                .append(ctx.t("ann.title", "count", all.size())).append("\n\n");
        InlineKeyboardMarkup.Builder kb = InlineKeyboardMarkup.builder();
        if (all.isEmpty()) {
            sb.append(ctx.t("ann.empty"));
        } else {
            for (AnnouncementView a : all.subList(page * PAGE, Math.min(all.size(), (page + 1) * PAGE))) {
                sb.append(ctx.t("ann.item", "flags", flags(ctx, a), "title", e(a.title()),
                        "date", a.date() == null ? "" : ctx.dateShort(a.date()))).append("\n");
                kb.row(ctx.btn((a.unread() ? "🆕 " : "") + (a.important() ? "🔴 " : "") + shorten(a.title()),
                        CallbackData.of("ann").with("id", a.id()).with("p", page)));
            }
            sb.append("\n").append(ctx.t("ann.legend"));
        }
        kb.row(ctx.pagerRow(CallbackData.of("ann"), page, pages).toArray(new InlineButton[0]));
        kb.row(ctx.switchRow("ann").toArray(new InlineButton[0]));
        kb.row(ctx.navRow(null).toArray(new InlineButton[0]));
        return BotView.of(sb.toString().stripTrailing(), kb.build()).section("announcements");
    }

    private BotView detail(BotContext ctx, Long id, int page) {
        CallbackData back = CallbackData.of("ann").with("p", page);
        Optional<AnnouncementView> found = data.announcement(ctx.student(), ctx.session(), id);
        if (found.isEmpty()) {
            // Not one of this child's announcements (or deleted) — never reveal anything about it.
            return BotView.of(ctx.t("common.denied"), InlineKeyboardMarkup.builder().row(ctx.navRow(back).toArray(new InlineButton[0])).build());
        }
        AnnouncementView a = found.get();
        ctx.session().markRead(a.id());
        access.save(ctx.session());
        String text = ctx.t("ann.detail", "flags", a.important() ? ctx.t("ann.important") : "", "title", e(a.title()),
                "date", a.date() == null ? "" : ctx.dateFull(a.date()),
                "scope", a.classLabel() == null ? "" : ctx.t("ann.scope.class", "class", e(a.classLabel())),
                "content", e(a.content()),
                "deadline", a.deadline() == null ? "" : ctx.t("ann.deadline", "date", ctx.dateFull(a.deadline())));
        String full = ctx.crumb(ctx.t("sec.announcements"), e(shorten(a.title()))) + "\n\n" + text;
        return BotView.of(full, InlineKeyboardMarkup.builder().row(ctx.navRow(back).toArray(new InlineButton[0])).build())
                .section("announcements");
    }

    private String flags(BotContext ctx, AnnouncementView a) {
        return (a.unread() ? ctx.t("ann.new") : "") + (a.important() ? ctx.t("ann.important") : "");
    }

    private static String shorten(String s) {
        return s.length() <= 40 ? s : s.substring(0, 39) + "…";
    }
}
