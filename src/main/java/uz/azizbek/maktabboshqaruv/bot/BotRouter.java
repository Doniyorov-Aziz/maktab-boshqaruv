package uz.azizbek.maktabboshqaruv.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.azizbek.maktabboshqaruv.bot.screens.MessageScreen;
import uz.azizbek.maktabboshqaruv.entity.ParentSession;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.service.BotUsageService;
import uz.azizbek.maktabboshqaruv.service.LinkingService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentAccessService;
import uz.azizbek.maktabboshqaruv.telegram.LinkCodeGenerator;
import uz.azizbek.maktabboshqaruv.telegram.TelegramClient;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels;
import uz.azizbek.maktabboshqaruv.telegram.TokenMasker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central dispatcher for every incoming update.
 *
 *  - callback_query: answered at once (the button never "hangs"), routed by the
 *    first segment of callback_data to its {@link Screen}, and the tapped
 *    message is edited in place;
 *  - messages: /commands, bottom-keyboard labels (any language), shared
 *    contacts, and typed input for an open conversation.
 *
 * Security: the child shown is always resolved through ParentAccessService —
 * an "s" id inside callback_data is re-checked against this chat's links, and
 * a foreign id is refused. A per-chat rate limit protects the database, and
 * one failing update never stops polling: the parent gets "Nimadir xato ketdi".
 */
@Service
public class BotRouter {

    private static final Logger log = LoggerFactory.getLogger(BotRouter.class);
    private static final Map<String, String> COMMANDS = Map.of(
            "/menu", "home", "/jadval", "sch", "/davomat", "att", "/baholar", "gr");

    private final Map<String, Screen> screens = new HashMap<>();
    private final List<InputHandler> inputHandlers;

    @Autowired
    private ParentAccessService access;
    @Autowired
    private LinkingService linking;
    @Autowired
    private Onboarding onboarding;
    @Autowired
    private BotResponder responder;
    @Autowired
    private TelegramClient client;
    @Autowired
    private ChatRateLimiter rateLimiter;
    @Autowired
    private BotUsageService usage;
    @Autowired
    private Clock clock;
    @Autowired
    private BotBanners banners;

    public BotRouter(List<Screen> screenBeans, List<InputHandler> inputHandlers) {
        for (Screen s : screenBeans) screens.put(s.code(), s);
        this.inputHandlers = inputHandlers;
    }

    public void handle(TelegramModels.Update update) {
        if (update.callbackQuery() != null) {
            handleCallback(update.callbackQuery());
        } else if (update.message() != null) {
            handleMessage(update.message());
        }
    }

    // ------------------------------------------------------------ callbacks

    void handleCallback(TelegramModels.CallbackQuery cq) {
        if (cq.message() == null || cq.message().chat() == null || !"private".equals(cq.message().chat().type())) {
            answer(cq.id(), null, false);
            return;
        }
        long chatId = cq.message().chat().id();
        ParentSession session = null;
        try {
            if (!rateLimiter.allow(chatId)) {
                answer(cq.id(), BotI18n.get().t(access.session(chatId).lang(), "common.slow_down"), false);
                return;
            }
            CallbackData data = CallbackData.parse(cq.data());
            if ("noop".equals(data.screen())) {
                answer(cq.id(), null, false);
                return;
            }
            session = touch(chatId, cq.from());
            // A new tap elsewhere abandons a half-finished conversation.
            Screen screen = screens.getOrDefault(data.screen(), screens.get("home"));
            if (session.getPendingAction() != null && !ownsPending(screen, session.getPendingAction())) {
                session.setPendingAction(null);
                session.setPendingData(null);
                access.save(session);
            }

            List<Student> students = access.linkedStudents(chatId);
            if (students.isEmpty()) {
                answer(cq.id(), null, false);
                BotContext ctx = new BotContext(chatId, null, session, null, students, cq.from());
                responder.deliver(chatId, null, decorate(ctx, onboarding.welcome(ctx), null));
                return;
            }
            Student student = data.get("s") != null
                    ? access.requireLinked(chatId, data.getLong("s"))
                    : access.selected(session);
            if (data.get("s") != null && !student.getId().equals(session.getSelectedStudentId())) {
                session.setSelectedStudentId(student.getId());
                access.save(session);
            }

            if (isSlowText(data)) {
                client.sendChatAction(chatId, "typing");
            }
            // Buttons under an automatic notification open a new page instead of overwriting the notification.
            Long messageId = "1".equals(data.get("n")) ? null : cq.message().messageId();
            BotContext ctx = new BotContext(chatId, messageId, session, student, students, cq.from());
            BotView view = decorate(ctx, render(ctx, screen, data), data);
            answer(cq.id(), view.toast(), false);
            responder.deliver(chatId, messageId, view, messageId != null && cq.message().photo() != null);
            usage.record(chatId, ctx.student(), view.section());
        } catch (ParentAccessService.AccessDenied denied) {
            answer(cq.id(), BotI18n.get().t(session == null ? "uz" : session.lang(), "common.denied"), true);
        } catch (Exception e) {
            log.error("Callback'ni qayta ishlashda xato (chat={}, data={}): {}", chatId, cq.data(), TokenMasker.mask(e.toString()), e);
            answer(cq.id(), BotI18n.get().t(session == null ? "uz" : session.lang(), "common.error"), true);
        }
    }

    private BotView render(BotContext ctx, Screen screen, CallbackData data) {
        BotView view = screen.render(ctx, data);
        if (view.redirectScreen() != null) {
            Screen target = screens.getOrDefault(view.redirectScreen(), screens.get("home"));
            Student selected = access.selected(ctx.session());
            BotContext fresh = new BotContext(ctx.chatId(), ctx.messageId(), ctx.session(), selected, ctx.students(), ctx.from());
            BotView redirected = target.render(fresh, CallbackData.of(target.code()));
            return redirected.toast(view.toast());
        }
        return view;
    }

    /** Turns a page into a card under its section's static banner (sent by file_id, never drawn). */
    private BotView decorate(BotContext ctx, BotView view, CallbackData data) {
        if (view == null || view.text() == null || view.banner() != null) return view;
        String section = "onboarding".equals(view.section()) ? "welcome" : view.section();
        if (!"welcome".equals(section) && ctx.student() == null) return view;
        BotBanners.Banner banner = banners.of(section);
        return banner == null ? view : view.banner(banner);
    }

    /** Pages that scan a whole quarter/year or month of records. */
    private static boolean isSlowText(CallbackData data) {
        return ("att".equals(data.screen()) && ("y".equals(data.get("k")) || "q".equals(data.get("k"))))
                || ("rep".equals(data.screen()) && "month".equals(data.get("t")));
    }

    private boolean writingAppeal(long chatId, TelegramModels.Message m) {
        if (m.text() != null && m.text().startsWith("/")) return false;
        String pending = access.session(chatId).getPendingAction();
        return pending != null && pending.startsWith(MessageScreen.PENDING);
    }

    private static boolean ownsPending(Screen screen, String pending) {
        return screen instanceof InputHandler h && h.pendingPrefixes().stream().anyMatch(pending::startsWith);
    }

    // ------------------------------------------------------------- messages

    void handleMessage(TelegramModels.Message m) {
        if (m.chat() == null || m.from() == null || !"private".equals(m.chat().type())) return;
        if (Boolean.TRUE.equals(m.from().isBot())) return;
        long chatId = m.chat().id();
        ParentSession session = null;
        try {
            // An album (or several forwarded messages) arrives as a burst of updates; while the
            // parent is writing an appeal none of them may be dropped (the appeal has its own cap).
            if (!rateLimiter.allow(chatId) && !writingAppeal(chatId, m)) return;
            session = touch(chatId, m.from());
            List<Student> students = access.linkedStudents(chatId);
            Student selected = students.isEmpty() ? null : access.selected(session);
            BotContext ctx = new BotContext(chatId, null, session, selected, students, m.from());

            BotView view = route(ctx, m);
            if (view != null) {
                responder.deliver(chatId, null, decorate(ctx, view, null).asNewMessage());
                usage.record(chatId, ctx.student(), view.section());
            }
        } catch (ParentAccessService.AccessDenied denied) {
            safeSend(chatId, BotI18n.get().t(session == null ? "uz" : session.lang(), "common.denied"));
        } catch (Exception e) {
            log.error("Xabarni qayta ishlashda xato (chat={}): {}", chatId, TokenMasker.mask(e.toString()), e);
            safeSend(chatId, BotI18n.get().t(session == null ? "uz" : session.lang(), "common.error"));
        }
    }

    private BotView route(BotContext ctx, TelegramModels.Message m) {
        if (m.contact() != null) {
            LinkingService.Result result = linking.byContact(ctx.chatId(), m);
            return result.ok() ? onboarding.linked(ctx, result.linked()) : error(ctx, result.errorKey());
        }

        String text = m.text() != null ? m.text().trim() : m.caption() != null ? m.caption().trim() : "";
        String photo = m.largestPhotoId();
        String command = text.startsWith("/") ? text.split("\\s+", 2)[0].toLowerCase() : "";
        int at = command.indexOf('@');
        if (at > 0) command = command.substring(0, at); // "/start@MyBot"

        if ("/start".equals(command)) {
            String code = text.contains(" ") ? text.substring(text.indexOf(' ') + 1).trim() : "";
            if (!code.isEmpty()) {
                LinkingService.Result result = linking.byCode(ctx.chatId(), ctx.from(), code);
                return result.ok() ? onboarding.linked(ctx, result.linked()) : error(ctx, result.errorKey());
            }
            return ctx.students().isEmpty() ? onboarding.welcome(ctx) : homeWithKeyboard(ctx);
        }
        if ("/stop".equals(command)) {
            int count = linking.stop(ctx.chatId());
            return BotView.of(count == 0 ? ctx.t("stop.none") : ctx.t("stop.done", "count", count), null)
                    .withReplyKeyboard(TelegramModels.removeKeyboard()).section("stop");
        }
        if ("/yordam".equals(command) || "/help".equals(command)) {
            return BotView.of(ctx.t("help.text"), null).withReplyKeyboard(Keyboards.main(ctx.lang())).section("help");
        }

        // A conversation is open (writing to school, absence request, adding a child).
        String pending = ctx.session().getPendingAction();
        if (pending != null && command.isEmpty()) {
            for (InputHandler h : inputHandlers) {
                if (h.pendingPrefixes().stream().anyMatch(pending::startsWith)) {
                    if (ctx.student() == null && !(h instanceof uz.azizbek.maktabboshqaruv.bot.screens.ChildrenScreen)) break;
                    return h.handleMessage(ctx, m);
                }
            }
        }

        if (ctx.students().isEmpty()) {
            // Not linked yet: a typed code is accepted as if it came through the deep link.
            if (Onboarding.isCodeButton(ctx, text)) return onboarding.codePrompt(ctx);
            if (LinkCodeGenerator.looksValid(text)) {
                LinkingService.Result result = linking.byCode(ctx.chatId(), ctx.from(), text);
                return result.ok() ? onboarding.linked(ctx, result.linked()) : error(ctx, result.errorKey());
            }
            return onboarding.welcome(ctx);
        }

        String screenCode = COMMANDS.getOrDefault(command, Keyboards.screenFor(text));
        if (screenCode != null) {
            Screen screen = screens.get(screenCode);
            if ("home".equals(screenCode)) return homeWithKeyboard(ctx);
            return screen.render(ctx, CallbackData.of(screenCode));
        }
        return BotView.of(ctx.t("unknown"), null).withReplyKeyboard(Keyboards.main(ctx.lang())).section("unknown");
    }

    private BotView homeWithKeyboard(BotContext ctx) {
        return screens.get("home").render(ctx, CallbackData.of("home"))
                .preface(ctx.t("home.pick_section"), Keyboards.main(ctx.lang()));
    }

    private BotView error(BotContext ctx, String key) {
        Object keyboard = ctx.students().isEmpty()
                ? Onboarding.welcomeKeyboard(ctx)
                : Keyboards.main(ctx.lang());
        return BotView.of(ctx.t(key), null).withReplyKeyboard(keyboard).section("link_error");
    }

    // --------------------------------------------------------------- helpers

    /**
     * Loads the chat's session and refreshes what changed. Written back only when
     * something did (or "last active" is five minutes old) — not an UPDATE per tap.
     */
    private ParentSession touch(long chatId, TelegramModels.User from) {
        ParentSession session = access.session(chatId);
        LocalDateTime now = LocalDateTime.now(clock);
        boolean changed = false;
        if (session.getLastActiveAt() == null || session.getLastActiveAt().isBefore(now.minusMinutes(5))) {
            session.setLastActiveAt(now);
            changed = true;
        }
        if (from != null) {
            if (!java.util.Objects.equals(session.getFirstName(), from.firstName())
                    || !java.util.Objects.equals(session.getUsername(), from.username())) {
                session.setFirstName(from.firstName());
                session.setUsername(from.username());
                changed = true;
            }
            // First contact: pick the language from the Telegram app when it is Russian.
            if (session.getLanguage() == null) {
                session.setLanguage("ru".equals(from.languageCode()) ? "ru" : "uz");
                changed = true;
            }
        }
        return changed ? access.save(session) : session;
    }

    private void answer(String callbackId, String text, boolean alert) {
        try {
            client.answerCallbackQuery(callbackId, text, alert);
        } catch (Exception e) {
            log.debug("answerCallbackQuery xato: {}", TokenMasker.mask(e.getMessage()));
        }
    }

    private void safeSend(long chatId, String text) {
        try {
            client.sendMessage(chatId, text, null);
        } catch (Exception e) {
            log.warn("Xato haqidagi xabarni yuborib bo'lmadi (chat={}): {}", chatId, TokenMasker.mask(e.getMessage()));
        }
    }
}
