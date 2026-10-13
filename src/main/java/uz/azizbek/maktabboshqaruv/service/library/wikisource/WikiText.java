package uz.azizbek.maktabboshqaruv.service.library.wikisource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wikitext of Wikisource pages: reading an author page (years, the list of works), a work page
 * (its chapter subpages) and turning a page into clean text — templates, references, links, wiki
 * formatting and HTML removed, paragraphs and poem lines kept. Nothing of the text itself is changed.
 */
public final class WikiText {

    /** A work linked from an author page: page title, its label, year, and the heading it is under. */
    public record WorkLink(String title, String label, Integer year, String section) {
    }

    /** A chapter of a work: its subpage and the heading to print. */
    public record ChapterLink(String title, String heading) {
    }

    /** "##" marks a heading line inside a cleaned text; paragraphs are separated by blank lines. */
    public static final String HEADING = "## ";

    private static final Pattern LINK = Pattern.compile("\\[\\[([^\\]|]+)(?:\\|([^\\]]*))?]]");
    private static final Pattern YEAR = Pattern.compile("\\((?:[^()]*?)(\\d{4})(?:[^()]*?)\\)");
    private static final Pattern HEADING_LINE = Pattern.compile("^(={2,6})\\s*(.*?)\\s*\\1\\s*$");
    /** templates whose last parameter is text to keep ({{center|…}}, {{h|level=3|…}}) */
    private static final Set<String> TEXT_TEMPLATES = Set.of("center", "c", "larger", "smaller", "bold", "b", "i", "big",
            "right", "sc", "nowrap", "h", "x-larger", "small", "block center", "uc", "sm");
    /**
     * Markers of a page that is not finished: a maintenance template, or a line that says only that.
     * The word inside the prose ("to'y tayyorlanmoqda") does not count.
     */
    private static final Pattern UNFINISHED = Pattern.compile(
            "\\{\\{\\s*(tayyorlanmoqda|incomplete|unfinished|tugallanmagan|неоконченн|неполн|в процессе)"
                    + "|^\\s*[(\\[]?\\s*(tayyorlanmoqda|в процессе подготовки)[^\\n]{0,20}$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);

    private WikiText() {
    }

    // ------------------------------------------------------------------ author page

    /** {@code {{Author | deathyear = 1938}}} → 1938 (also "dates = 1894 - 1938"). */
    public static Integer templateYear(String wikitext, String param) {
        Matcher m = Pattern.compile("\\|\\s*" + param + "\\s*=\\s*(\\d{3,4})").matcher(wikitext);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    /** The works listed as bullets ("*[[Title|Label]] (1926)") on an author page, under their headings. */
    public static List<WorkLink> works(String wikitext) {
        List<WorkLink> works = new ArrayList<>();
        String section = null;
        for (String raw : wikitext.split("\n")) {
            String line = raw.strip();
            Matcher h = HEADING_LINE.matcher(line);
            if (h.matches()) {
                section = stripMarkup(h.group(2));
                continue;
            }
            if (!line.startsWith("*") && !line.startsWith("#")) continue;
            Matcher l = LINK.matcher(line);
            if (!l.find()) continue;
            String target = l.group(1).strip();
            if (target.isEmpty() || target.contains(":") || target.startsWith("#")) continue; // other namespaces, wikipedia
            String label = l.group(2) != null && !l.group(2).isBlank() ? l.group(2).strip() : target;
            Matcher y = YEAR.matcher(line.substring(l.end()));
            Integer year = y.find() ? Integer.valueOf(y.group(1)) : null;
            if (works.stream().noneMatch(w -> w.title().equals(target))) {
                works.add(new WorkLink(target.replace('_', ' '), stripMarkup(label), year, section));
            }
        }
        return works;
    }

    // ------------------------------------------------------------------ work page

    /**
     * The chapter subpages a work page lists ("*[[O'tkan kunlar/01|01]]. Otabek…" or "[[/02|II]]"), in
     * order, with the text after the link as part of the heading.
     */
    public static List<ChapterLink> chapters(String title, String wikitext) {
        List<ChapterLink> list = new ArrayList<>();
        // footnotes and tags never become part of a chapter heading
        String noNotes = wikitext.replaceAll("(?is)<ref[^>]*/>", "").replaceAll("(?is)<ref[^>]*>.*?</ref>", "")
                .replaceAll("<[^>]+>", "");
        for (String raw : noNotes.split("\n")) {
            Matcher l = LINK.matcher(raw);
            while (l.find()) {
                String target = l.group(1).strip();
                String full = target.startsWith("/") ? title + target : target;
                if (!full.startsWith(title + "/")) continue;
                String label = l.group(2) != null && !l.group(2).isBlank() ? l.group(2).strip()
                        : full.substring(title.length() + 1);
                String rest = stripMarkup(raw.substring(l.end())).replaceFirst("^[.)\\s:—–-]+", "").strip();
                String heading = rest.isEmpty() ? stripMarkup(label) : stripMarkup(label) + ". " + rest;
                if (list.stream().noneMatch(c -> c.title().equals(full))) list.add(new ChapterLink(full, heading));
            }
        }
        return list;
    }

    public static boolean unfinished(String wikitext) {
        return UNFINISHED.matcher(wikitext).find();
    }

    // ------------------------------------------------------------------ cleaning

    /** The readable text of a page: paragraphs separated by blank lines, headings prefixed with "## ". */
    public static String clean(String wikitext) {
        String t = wikitext.replace("\r", "");
        t = t.replaceAll("(?s)<!--.*?-->", "");
        t = t.replaceAll("(?is)<ref[^>]*/>", "").replaceAll("(?is)<ref[^>]*>.*?</ref>", "");
        t = t.replaceAll("(?is)<references\\s*/?>", "");
        t = removeTemplates(t);
        t = t.replaceAll("(?i)__[A-Z]+__", "");
        t = t.replaceAll("(?i)\\[\\[(?:category|turkum|категория|file|fayl|файл|image|rasm|изображение):[^\\]]*]]", "");
        t = t.replaceAll("(?i)</?poem[^>]*>", "\n");
        t = t.replaceAll("(?i)<br\\s*/?>", "\n");
        t = t.replaceAll("<[^>]+>", "");
        StringBuilder out = new StringBuilder();
        for (String raw : t.split("\n", -1)) {
            String line = raw.strip();
            Matcher h = HEADING_LINE.matcher(line);
            if (h.matches()) {
                out.append("\n").append(HEADING).append(stripMarkup(h.group(2))).append("\n\n");
                continue;
            }
            if (line.startsWith("*") || line.startsWith("#")) line = "• " + line.replaceFirst("^[*#:;]+\\s*", "");
            else if (line.startsWith(":")) line = line.replaceFirst("^:+\\s*", "");
            out.append(stripMarkup(line)).append("\n");
        }
        // collapse 3+ newlines to a paragraph break, trim
        return out.toString().replaceAll("\n{3,}", "\n\n").strip();
    }

    /** Links, bold/italic quotes, external links and HTML entities of one line. */
    static String stripMarkup(String s) {
        String t = s;
        Matcher l = LINK.matcher(t);
        StringBuilder sb = new StringBuilder();
        while (l.find()) {
            String target = l.group(1);
            String text = l.group(2) != null ? l.group(2) : target.replaceFirst("^[a-zA-Z]{1,3}:", "").replaceFirst("^/", "");
            l.appendReplacement(sb, Matcher.quoteReplacement(text));
        }
        l.appendTail(sb);
        t = sb.toString();
        t = t.replaceAll("\\[(?:https?:)?//[^\\s\\]]+\\s+([^\\]]+)]", "$1").replaceAll("\\[(?:https?:)?//[^\\]]+]", "");
        t = t.replace("'''''", "").replace("'''", "").replace("''", "");
        t = t.replace("&nbsp;", " ").replace("&thinsp;", " ").replace("&mdash;", "—").replace("&ndash;", "–")
                .replace("&laquo;", "«").replace("&raquo;", "»").replace("&quot;", "\"").replace("&lt;", "<")
                .replace("&gt;", ">").replace("&amp;", "&");
        t = t.replace("\u00AD", ""); // soft hyphens
        return t.replaceAll("[ \\t]{2,}", " ").strip();
    }

    /** Removes templates from the inside out; text templates ({{center|x}}) leave their text. */
    static String removeTemplates(String text) {
        String t = text;
        Pattern innermost = Pattern.compile("\\{\\{([^{}]*)}}");
        for (int guard = 0; guard < 50; guard++) {
            Matcher m = innermost.matcher(t);
            if (!m.find()) break;
            StringBuilder sb = new StringBuilder();
            m.reset();
            while (m.find()) {
                String body = m.group(1);
                String[] parts = body.split("\\|");
                String name = parts[0].strip().toLowerCase(Locale.ROOT);
                String keep = "";
                if (TEXT_TEMPLATES.contains(name) && parts.length > 1) {
                    String last = parts[parts.length - 1];
                    keep = last.contains("=") && !last.contains(" ") ? "" : last.replaceFirst("^\\s*\\w+\\s*=", "").strip();
                }
                m.appendReplacement(sb, Matcher.quoteReplacement(keep));
            }
            m.appendTail(sb);
            t = sb.toString();
        }
        // a table left over from layout templates
        return t.replaceAll("(?s)\\{\\|.*?\\|}", "");
    }
}
