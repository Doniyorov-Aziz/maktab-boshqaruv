package uz.azizbek.maktabboshqaruv.service.library;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Search normalisation for book titles and authors. Both the stored key and the parent's
 * query go through {@link #normalize}, so the way something is typed does not matter:
 * <ul>
 *   <li>case — "O'TKAN KUNLAR" = "o'tkan kunlar";</li>
 *   <li>the apostrophe of o' and g' — ', ‘, ’, ʻ, ʼ, ` or none at all;</li>
 *   <li>the script — Uzbek and Russian Cyrillic is written in Latin letters first
 *       ("Ўткан кунлар" → "o'tkan kunlar").</li>
 * </ul>
 */
public final class LibraryText {

    private static final Map<Character, String> CYRILLIC = Map.ofEntries(
            Map.entry('а', "a"), Map.entry('б', "b"), Map.entry('в', "v"), Map.entry('г', "g"), Map.entry('д', "d"),
            Map.entry('е', "e"), Map.entry('ё', "yo"), Map.entry('ж', "j"), Map.entry('з', "z"), Map.entry('и', "i"),
            Map.entry('й', "y"), Map.entry('к', "k"), Map.entry('л', "l"), Map.entry('м', "m"), Map.entry('н', "n"),
            Map.entry('о', "o"), Map.entry('п', "p"), Map.entry('р', "r"), Map.entry('с', "s"), Map.entry('т', "t"),
            Map.entry('у', "u"), Map.entry('ф', "f"), Map.entry('х', "x"), Map.entry('ц', "ts"), Map.entry('ч', "ch"),
            Map.entry('ш', "sh"), Map.entry('щ', "sh"), Map.entry('ъ', ""), Map.entry('ы', "i"), Map.entry('ь', ""),
            Map.entry('э', "e"), Map.entry('ю', "yu"), Map.entry('я', "ya"), Map.entry('ў', "o"), Map.entry('қ', "q"),
            Map.entry('ғ', "g"), Map.entry('ҳ', "h"));

    private LibraryText() {
    }

    /** Lower-case Latin letters and digits separated by single spaces; apostrophes dropped. */
    public static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(n.length() + 8);
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            String latin = CYRILLIC.get(c);
            if (latin != null) {
                sb.append(latin);
            } else if (Character.isLetterOrDigit(c)) {
                sb.append(c);
            } else if (isApostrophe(c)) {
                // o' = o‘ = oʻ = o — dropped, not a word break
            } else {
                sb.append(' ');
            }
        }
        return sb.toString().trim().replaceAll("\\s+", " ");
    }

    private static boolean isApostrophe(char c) {
        return c == '\'' || c == '‘' || c == '’' || c == 'ʻ' || c == 'ʼ' || c == '`' || c == '´' || c == 'ʹ';
    }

    /** The stored search key of a book: title and author together. */
    public static String key(String title, String author) {
        return normalize((title == null ? "" : title) + " " + (author == null ? "" : author));
    }

    /** Three LIKE patterns, one per word of the query (missing words match anything). */
    public static List<String> likePatterns(String query) {
        List<String> words = new ArrayList<>();
        for (String w : normalize(query).split(" ")) {
            if (!w.isBlank() && words.size() < 3) words.add("%" + escapeLike(w) + "%");
        }
        while (words.size() < 3) words.add("%");
        return words;
    }

    private static String escapeLike(String w) {
        // normalised words hold only letters and digits, so there is nothing to escape; kept for safety
        return w.replace("%", "").replace("_", "");
    }
}
