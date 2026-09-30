package uz.azizbek.maktabboshqaruv.telegram;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhoneNormalizerTest {

    @Test
    void allCommonUzbekFormats_normalizeToSameValue() {
        String expected = "+998901234567";
        assertEquals(expected, PhoneNormalizer.normalize("+998901234567"));
        assertEquals(expected, PhoneNormalizer.normalize("998901234567"));     // how Telegram shares it
        assertEquals(expected, PhoneNormalizer.normalize("+998 90 123-45-67"));
        assertEquals(expected, PhoneNormalizer.normalize("(90) 123 45 67"));
        assertEquals(expected, PhoneNormalizer.normalize("901234567"));
        assertEquals(expected, PhoneNormalizer.normalize("8 90 1234567"));
        assertEquals(expected, PhoneNormalizer.normalize("00998901234567"));
    }

    @Test
    void foreignNumber_keptWithPlus() {
        assertEquals("+79161234567", PhoneNormalizer.normalize("+7 916 123-45-67"));
    }

    @Test
    void garbage_returnsNull() {
        assertNull(PhoneNormalizer.normalize(null));
        assertNull(PhoneNormalizer.normalize(""));
        assertNull(PhoneNormalizer.normalize("abc"));
        assertNull(PhoneNormalizer.normalize("12345"));
    }

    @Test
    void differentNumbers_doNotCollide() {
        assertNotEquals(PhoneNormalizer.normalize("+998901234567"), PhoneNormalizer.normalize("+998911234567"));
    }
}
