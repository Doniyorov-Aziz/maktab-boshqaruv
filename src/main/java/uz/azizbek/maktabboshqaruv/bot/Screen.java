package uz.azizbek.maktabboshqaruv.bot;

/**
 * One bot page. The router maps callback_data's first segment to {@link #code()}
 * and calls {@link #render}; the context already carries the verified child.
 */
public interface Screen {

    /** First segment of callback_data, e.g. "att". */
    String code();

    BotView render(BotContext ctx, CallbackData data);

    /** Most pages show one child's data; the few that do not (settings, children) override this. */
    default boolean requiresStudent() {
        return true;
    }
}
