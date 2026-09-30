package uz.azizbek.maktabboshqaruv.telegram;

import tools.jackson.databind.json.JsonMapper;

/** One shared JSON mapper for keyboards stored in the outbox and multipart reply_markup fields. */
public final class TelegramJson {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private TelegramJson() {
    }

    public static String write(Object value) {
        if (value == null) return null;
        if (value instanceof String s) return s;
        return MAPPER.writeValueAsString(value);
    }
}
