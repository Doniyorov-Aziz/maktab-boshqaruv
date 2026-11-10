package uz.azizbek.maktabboshqaruv.bot.screens;

import org.springframework.stereotype.Component;
import uz.azizbek.maktabboshqaruv.bot.*;
import uz.azizbek.maktabboshqaruv.service.PaymentService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentModulesService;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineButton;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineKeyboardMarkup;

import static uz.azizbek.maktabboshqaruv.bot.BotContext.e;

/** 💳 To'lovlar — the child's months (paid / part paid / unpaid / not due yet), the debt and how to pay. */
@Component
public class PaymentScreen implements Screen {

    private final ParentModulesService data;

    public PaymentScreen(ParentModulesService data) {
        this.data = data;
    }

    @Override
    public String code() {
        return "pay";
    }

    @Override
    public BotView render(BotContext ctx, CallbackData cb) {
        StringBuilder sb = new StringBuilder(ctx.crumb(ctx.t("sec.more"), ctx.t("sec.payments"))).append("\n")
                .append(ctx.childHeader()).append("\n");
        if (!data.paymentsEnabled(ctx.student())) {
            sb.append(ctx.t("pay.off"));
        } else {
            PaymentService.StudentAccount a = data.account(ctx.student());
            if (a.monthlyFee() == 0) {
                sb.append(ctx.t("pay.free"));
            } else {
                sb.append(ctx.t("pay.fee", "fee", PaymentService.money(a.monthlyFee()))).append("\n\n");
                for (PaymentService.MonthRow m : a.months()) {
                    String icon = switch (m.status()) {
                        case PAID -> "✅";
                        case PARTIAL -> "🟡";
                        case UNPAID -> "❌";
                        case UPCOMING -> "⏳";
                        case FREE -> "🎁";
                    };
                    sb.append(icon).append(" ").append(ctx.i18n().monthYear(ctx.lang(), m.month().getMonthValue(), m.month().getYear()));
                    if (m.status() == PaymentService.MonthStatus.PARTIAL) {
                        sb.append(" — ").append(ctx.t("pay.partial", "paid", PaymentService.money(m.paid())));
                    } else if (m.status() == PaymentService.MonthStatus.UPCOMING) {
                        sb.append(" — ").append(ctx.t("pay.until", "date", ctx.dateShort(m.dueDate())));
                    }
                    sb.append("\n");
                }
                sb.append("\n").append(a.debt() > 0 ? ctx.t("pay.debt", "debt", PaymentService.money(a.debt())) : ctx.t("pay.no_debt"));
                if (a.payInfo() != null) sb.append("\n\n").append(ctx.t("pay.how")).append("\n").append(e(a.payInfo()));
            }
        }
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .row(ctx.switchRow("pay").toArray(new InlineButton[0]))
                .row(ctx.navRow(CallbackData.of("more")).toArray(new InlineButton[0]))
                .build();
        return BotView.of(sb.toString().stripTrailing(), kb).section("payments");
    }
}
