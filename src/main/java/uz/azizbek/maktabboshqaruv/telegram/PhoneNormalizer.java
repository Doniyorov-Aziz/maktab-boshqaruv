package uz.azizbek.maktabboshqaruv.telegram;

/**
 * Brings Uzbek phone numbers typed in any common way to one canonical
 * "+998XXXXXXXXX" form, so the number Telegram shares ("998901234567", no plus)
 * matches what staff typed into guardian_phone ("+998 90 123-45-67",
 * "90 123 45 67", "8 90 1234567", ...).
 */
public final class PhoneNormalizer {

    private PhoneNormalizer() {
    }

    /** @return canonical "+998XXXXXXXXX", "+<digits>" for foreign numbers, or null if unusable. */
    public static String normalize(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("\\D", "");
        if (digits.isEmpty()) return null;

        if (digits.length() == 12 && digits.startsWith("998")) {
            return "+" + digits;
        }
        if (digits.length() == 9) {
            return "+998" + digits;
        }
        // Old domestic dialing: 8 + 9-digit number
        if (digits.length() == 10 && digits.startsWith("8")) {
            return "+998" + digits.substring(1);
        }
        // International prefix typed as 00998...
        if (digits.length() == 14 && digits.startsWith("00998")) {
            return "+" + digits.substring(2);
        }
        if (digits.length() >= 10 && digits.length() <= 15) {
            return "+" + digits;
        }
        return null;
    }

    /** Last 7 digits — a cheap, index-friendly pre-filter before exact normalized comparison. */
    public static String tail(String normalized) {
        if (normalized == null || normalized.length() < 7) return null;
        return normalized.substring(normalized.length() - 7);
    }
}
