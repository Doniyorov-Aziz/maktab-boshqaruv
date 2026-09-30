package uz.azizbek.maktabboshqaruv.telegram;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenMaskerTest {

    // Built at runtime so no token-shaped literal exists anywhere in the source tree.
    private static final String FAKE_TOKEN = "1".repeat(9) + ":" + "x".repeat(35);

    @Test
    void urlInExceptionMessage_isMasked() {
        String msg = "I/O error on POST request for \"https://api.telegram.org/bot" + FAKE_TOKEN + "/sendMessage\"";
        String masked = TokenMasker.mask(msg);
        assertFalse(masked.contains(FAKE_TOKEN));
        assertTrue(masked.contains("/bot***:***/sendMessage"));
    }

    @Test
    void explicitTokenIsMaskedEvenIfOddlyShaped() {
        assertEquals("a ***:*** b", TokenMasker.mask("a short:tok b", "short:tok"));
    }

    @Test
    void propertiesToString_neverPrintsToken() {
        TelegramProperties p = new TelegramProperties();
        p.setEnabled(true);
        p.setBotToken(FAKE_TOKEN);
        assertFalse(p.toString().contains(FAKE_TOKEN));
        assertEquals(TelegramProperties.Mode.LIVE, p.mode());
    }

    @Test
    void mode_resolution() {
        TelegramProperties p = new TelegramProperties();
        assertEquals(TelegramProperties.Mode.DISABLED, p.mode());
        assertFalse(p.isActive());
        p.setEnabled(true);
        assertEquals(TelegramProperties.Mode.NO_TOKEN, p.mode());
        assertFalse(p.isActive());
        p.setMock(true);
        assertEquals(TelegramProperties.Mode.MOCK, p.mode());
        assertTrue(p.isActive());
    }

    @Test
    void linkCodes_areLongRandomAndLinkSafe() {
        String a = LinkCodeGenerator.generate();
        String b = LinkCodeGenerator.generate();
        assertTrue(a.length() >= 8);
        assertNotEquals(a, b);
        assertTrue(a.matches("[A-Za-z0-9]+"));
        assertTrue(LinkCodeGenerator.looksValid(a));
        assertFalse(LinkCodeGenerator.looksValid("abc"));
        assertFalse(LinkCodeGenerator.looksValid("abcdefgh' or 1=1"));
    }
}
