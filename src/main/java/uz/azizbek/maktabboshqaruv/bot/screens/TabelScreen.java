package uz.azizbek.maktabboshqaruv.bot.screens;

import org.springframework.stereotype.Component;
import uz.azizbek.maktabboshqaruv.bot.*;
import uz.azizbek.maktabboshqaruv.service.ReportCardService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentModulesService;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineButton;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineKeyboardMarkup;

import static uz.azizbek.maktabboshqaruv.bot.BotContext.e;

/** 📄 Tabel — the child's quarter grades of every subject and the year grade; "📄 PDF" sends the printed tabel (a=pdf). */
@Component
public class TabelScreen implements Screen {

    private final ParentModulesService data;

    public TabelScreen(ParentModulesService data) {
        this.data = data;
    }

    @Override
    public String code() {
        return "tab";
    }

    @Override
    public BotView render(BotContext ctx, CallbackData cb) {
        String toast = null;
        if ("pdf".equals(cb.get("a"))) {
            toast = switch (data.sendTabel(ctx.student(), ctx.chatId())) {
                case QUEUED -> ctx.t("tab.toast.sending");
                case ALREADY -> ctx.t("tab.toast.already");
                case NONE -> ctx.t("common.denied");
            };
        }
        ReportCardService.StudentYear y = data.tabel(ctx.student());
        StringBuilder sb = new StringBuilder(ctx.crumb(ctx.t("sec.more"), ctx.t("sec.tabel"))).append("\n")
                .append(ctx.childHeader()).append("\n");
        boolean any = y.subjects().stream().anyMatch(s -> s.quarters().stream().anyMatch(q -> q != null));
        if (y.terms().isEmpty() || !any) {
            sb.append(ctx.t("tab.empty"));
        } else {
            StringBuilder head = new StringBuilder();
            for (ReportCardService.Term t : y.terms()) head.append(roman(t.number())).append(" ");
            sb.append("<code>").append(pad(ctx.t("tab.subject"), 14)).append(head).append(ctx.t("tab.year")).append("</code>\n");
            for (ReportCardService.YearSubject s : y.subjects()) {
                sb.append("<code>").append(e(pad(s.subject(), 14)));
                for (int i = 0; i < s.quarters().size(); i++) {
                    Integer q = s.quarters().get(i);
                    sb.append(pad(q == null ? "–" : q.toString(), roman(i + 1).length() + 1));
                }
                sb.append(s.year() == null ? "–" : s.year().toString()).append("</code>\n");
            }
        }
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .row(ctx.btn(ctx.t("tab.btn.pdf"), CallbackData.of("tab").with("a", "pdf")).styled("primary"))
                .row(ctx.switchRow("tab").toArray(new InlineButton[0]))
                .row(ctx.navRow(CallbackData.of("more")).toArray(new InlineButton[0]))
                .build();
        BotView v = BotView.of(sb.toString().stripTrailing(), kb).section("tabel");
        return toast == null ? v : v.toast(toast);
    }

    static String roman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> String.valueOf(n);
        };
    }

    /** Fixed width in the monospace table (long subject names are cut). */
    static String pad(String s, int width) {
        if (s.length() >= width) return s.substring(0, Math.max(1, width - 1)) + " ";
        return s + " ".repeat(width - s.length());
    }
}
