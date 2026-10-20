package uz.azizbek.maktabboshqaruv.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhoneFormatTest {

    @Test
    void everyWayOfTypingANumber_readsTheSame() {
        for (String typed : new String[]{"998901112233", "+998901112233", "+998 (90) 111-22-33",
                "+998 90 111 22 33", "901112233", "90 111 22 33"}) {
            assertEquals("+998 90 111 22 33", PhoneFormat.format(typed), typed);
        }
    }

    @Test
    void anythingElse_staysAsTyped() {
        assertNull(PhoneFormat.format(null));
        assertEquals("", PhoneFormat.format("  "));
        assertEquals("12345", PhoneFormat.format(" 12345 "));
        assertEquals("+7 495 123 45 67", PhoneFormat.format("+7 495 123 45 67"));
    }
}
