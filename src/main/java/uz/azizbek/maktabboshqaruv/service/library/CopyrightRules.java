package uz.azizbek.maktabboshqaruv.service.library;

import java.util.Collection;
import java.util.Objects;

/**
 * When a work becomes free to hand out — the rule the library checks for "Jamoat mulki".
 *
 * <p>QONUNIY ASOS — O'zbekiston Respublikasining «Mualliflik huquqi va turdosh huquqlar
 * to'g'risida»gi qonuni:
 * <ul>
 *   <li><b>35-modda.</b> Mualliflik huquqi muallifning butun hayoti davomida va vafotidan keyin
 *       70 yil amal qiladi. Muddat uni boshlash uchun asos bo'lgan voqea yuz bergan yildan keyingi
 *       yilning 1-yanvaridan hisoblanadi. Muallif vafotidan keyin birinchi marta chop etilgan asar
 *       nashr etilganidan keyin 70 yil davomida himoya qilinadi.</li>
 *   <li><b>37-modda.</b> Muddat tugagach asar ijtimoiy mulkka (jamoat mulkiga) o'tadi va undan
 *       erkin foydalanish mumkin — lekin muallif nomi saqlanishi va asar buzilmasligi shart.</li>
 * </ul>
 *
 * <p>So for an author who died in year D the work is protected through 31 December of D + 70 and is
 * free from 1 January of D + 71 ("deathYear + 70 &lt; this year"). A work first published after the
 * author's death (in year P) is protected through P + 70. With several authors the one who died
 * last decides. The library therefore always shows the author's name and never edits the text.
 */
public final class CopyrightRules {

    public static final int TERM_YEARS = 70;

    private CopyrightRules() {
    }

    /** Last year of protection for a death (or posthumous publication) in {@code year}. */
    public static int protectedUntil(int year) {
        return year + TERM_YEARS;
    }

    /** First year the work is free ({@code protectedUntil + 1}). */
    public static int publicFrom(int year) {
        return year + TERM_YEARS + 1;
    }

    public static boolean isFree(int year, int currentYear) {
        return protectedUntil(year) < currentYear;
    }

    /** The death year that decides for several authors: the latest one; null when any is unknown. */
    public static Integer decidingDeathYear(Collection<Integer> deathYears) {
        if (deathYears.isEmpty() || deathYears.stream().anyMatch(Objects::isNull)) return null;
        return deathYears.stream().max(Integer::compare).orElse(null);
    }

    /** An author's state for the badges: free since a year, protected until a year, or unknown. */
    public record Status(String state, Integer year) {
        public static final String FREE = "FREE";
        public static final String PROTECTED = "PROTECTED";
        public static final String UNKNOWN = "UNKNOWN";

        public boolean free() {
            return FREE.equals(state);
        }
    }

    public static Status status(Integer deathYear, int currentYear) {
        if (deathYear == null) return new Status(Status.UNKNOWN, null);
        return isFree(deathYear, currentYear)
                ? new Status(Status.FREE, publicFrom(deathYear))
                : new Status(Status.PROTECTED, protectedUntil(deathYear));
    }
}
