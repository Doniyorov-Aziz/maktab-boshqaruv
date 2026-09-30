package uz.azizbek.maktabboshqaruv.telegram;

import java.security.SecureRandom;

/**
 * Parent-link codes: 10 characters from a 55-symbol alphabet (~58 bits of
 * entropy) drawn from SecureRandom, so they cannot be guessed or enumerated.
 * Look-alike characters (0/O, 1/l/I) are left out because parents may have to
 * type a code from a printed sheet. Every symbol is legal in a Telegram
 * {@code ?start=} deep-link parameter.
 */
public final class LinkCodeGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    public static final int LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private LinkCodeGenerator() {
    }

    public static String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    /** Cheap shape check before touching the database, so junk input never reaches a query. */
    public static boolean looksValid(String code) {
        if (code == null || code.length() < 8 || code.length() > 32) return false;
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (!Character.isLetterOrDigit(c) || c > 127) return false;
        }
        return true;
    }
}
