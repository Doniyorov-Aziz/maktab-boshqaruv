package uz.azizbek.maktabboshqaruv.bot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CallbackDataTest {

    @Test
    void roundTrip_keepsScreenAndParams() {
        String raw = CallbackData.of("att").with("k", "m").with("m", "2026-09").with("s", 123).toString();
        assertEquals("att:k:m:m:2026-09:s:123", raw);

        CallbackData parsed = CallbackData.parse(raw);
        assertEquals("att", parsed.screen());
        assertEquals("2026-09", parsed.get("m"));
        assertEquals(123L, parsed.getLong("s"));
        assertEquals("m", parsed.get("k"));
    }

    @Test
    void nullValuesAreSkipped_andDefaultsWork() {
        CallbackData cb = CallbackData.of("gr").with("v", "det").with("id", null);
        assertEquals("gr:v:det", cb.toString());
        assertEquals("recent", CallbackData.of("gr").get("v", "recent"));
        assertEquals(0, CallbackData.of("gr").getInt("p", 0));
    }

    @Test
    void garbageNeverThrows() {
        assertEquals("home", CallbackData.parse(null).screen());
        assertEquals("home", CallbackData.parse("  ").screen());
        CallbackData odd = CallbackData.parse("att:s:abc:dangling");
        assertNull(odd.getLong("s"), "non-numeric id must not parse into a student id");
        assertNull(odd.get("dangling"));
    }

    @Test
    void valuesMayNotContainTheSeparator() {
        assertThrows(IllegalArgumentException.class, () -> CallbackData.of("x").with("t", "a:b"));
    }

    @Test
    void over64BytesIsRejected() {
        CallbackData longOne = CallbackData.of("abs").with("d", "2026-10-01").with("n", 7).with("r", "I")
                .with("extra", "x".repeat(40));
        assertThrows(IllegalStateException.class, longOne::toString);
    }

    @Test
    void everyCallbackTheBotUsesFitsIn64Bytes() {
        // The longest shapes the screens build.
        String[] samples = {
                CallbackData.of("att").with("k", "m").with("m", "2026-09").with("v", "det").with("p", 12).toString(),
                CallbackData.of("ch").with("a", "sel").with("id", 9_999_999_999L).with("r", "home").toString(),
                CallbackData.of("abs").with("d", "2026-12-31").with("n", 7).with("r", "I").toString(),
                CallbackData.of("gr").with("v", "det").with("id", 9_999_999_999L).with("p", 99).toString(),
                "msg:a:to:to:CT:s:9999999999:n:1",
                "rep:t:week:s:9999999999:n:1"
        };
        for (String s : samples) {
            assertTrue(s.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 64, s);
        }
    }
}
