package uz.azizbek.maktabboshqaruv.config;

import org.jspecify.annotations.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Every JSON answer to the demo visitor (GUEST) passes through here: Telegram chat and user ids,
 * tokens, API keys, link codes, password hashes and the like are masked ("12****89") by field
 * name, and bot tokens / calendar feed links / device keys by their shape anywhere in a text.
 * Works on the serialized tree, so DTOs, entities and maps added later are covered as well.
 */
@ControllerAdvice
public class GuestResponseMasker implements ResponseBodyAdvice<Object> {

    /** Field names (lower case, without '_' and '-') that are always masked. */
    private static final Set<String> EXACT = Set.of(
            "chatid", "telegramid", "telegramuserid", "telegramchatid", "tguserid", "tgchatid", "fromid",
            "token", "password", "secret", "apikey", "linkcode", "invitelink", "inviteurl", "feedurl",
            "lasterror", "keyhash", "iphash");
    /** …and names ending like this (botToken, feedToken, passwordHash, clientSecret, …). */
    private static final String[] SUFFIXES = {"chatid", "token", "password", "passwordhash", "secret", "apikey"};

    // a bot token (123456:ABC…), a device key (mk_…), a calendar subscription link (/api/public/calendar/<token>)
    private static final Pattern IN_TEXT = Pattern.compile(
            "\\d{6,12}:[A-Za-z0-9_-]{30,}|mk_[A-Za-z0-9_-]{16,}|(?<=/api/public/calendar/)[A-Za-z0-9_-]{8,}");

    private final JsonMapper mapper;

    public GuestResponseMasker(JsonMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean supports(@NonNull MethodParameter returnType, @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, @NonNull MethodParameter returnType, @NonNull MediaType contentType,
                                  @NonNull Class<? extends HttpMessageConverter<?>> converterType,
                                  @NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response) {
        if (body == null || body instanceof byte[] || body instanceof Resource || body instanceof CharSequence) return body;
        if (!MediaType.APPLICATION_JSON.isCompatibleWith(contentType) && !contentType.getSubtype().endsWith("+json")) return body;
        if (!GuestPolicy.currentIsGuest()) return body;
        JsonNode tree = mapper.valueToTree(body);
        return mask(tree);
    }

    static JsonNode mask(JsonNode node) {
        if (node instanceof ObjectNode obj) {
            for (Map.Entry<String, JsonNode> e : new ArrayList<>(obj.properties())) {
                JsonNode value = e.getValue();
                if (sensitive(e.getKey()) && !value.isNull() && !value.isBoolean()) {
                    obj.put(e.getKey(), maskValue(value.isValueNode() ? value.asString() : "****"));
                } else {
                    obj.set(e.getKey(), mask(value));
                }
            }
            return obj;
        }
        if (node instanceof ArrayNode arr) {
            for (int i = 0; i < arr.size(); i++) arr.set(i, mask(arr.get(i)));
            return arr;
        }
        if (node.isString()) {
            String text = node.asString();
            Matcher m = IN_TEXT.matcher(text);
            if (m.find()) return JsonNodeFactory.instance.stringNode(m.replaceAll(r -> Matcher.quoteReplacement(maskValue(r.group()))));
        }
        return node;
    }

    static boolean sensitive(String field) {
        String f = field.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
        if (EXACT.contains(f)) return true;
        for (String s : SUFFIXES) if (f.endsWith(s)) return true;
        return false;
    }

    /** "1234567890" → "12****90"; short values become "****". */
    static String maskValue(String raw) {
        if (raw == null || raw.length() <= 4) return "****";
        return raw.substring(0, 2) + "****" + raw.substring(raw.length() - 2);
    }
}
