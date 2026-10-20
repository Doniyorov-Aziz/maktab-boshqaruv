package uz.azizbek.maktabboshqaruv.util;

/**
 * An Uzbek phone number as people read it: "+998 90 111 22 33", whatever way it was typed
 * ("998901112233", "+998(90)111-22-33", "90 111 22 33"). Anything else is returned as typed
 * (trimmed), never lost. The same rule as formatPhone() in the frontend's utils/tashkent.js.
 */
public final class PhoneFormat {

    private PhoneFormat() {
    }

    public static String format(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() == 9) digits = "998" + digits;
        if (digits.length() != 12 || !digits.startsWith("998")) return phone.strip();
        return "+998 " + digits.substring(3, 5) + " " + digits.substring(5, 8) + " "
                + digits.substring(8, 10) + " " + digits.substring(10);
    }
}
