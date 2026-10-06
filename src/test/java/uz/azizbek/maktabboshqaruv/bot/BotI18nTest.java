package uz.azizbek.maktabboshqaruv.bot;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class BotI18nTest {

    private static final BotI18n I18N = BotI18n.get();
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-z_]+)}");

    @Test
    void everyLanguageHasExactlyTheSameKeys() {
        Set<String> uz = I18N.keys("uz");
        assertTrue(uz.size() > 300, "expected the full Uzbek text set");
        for (String lang : BotI18n.LANGUAGES) {
            Set<String> missing = new TreeSet<>(uz);
            missing.removeAll(I18N.keys(lang));
            Set<String> extra = new TreeSet<>(I18N.keys(lang));
            extra.removeAll(uz);
            assertTrue(missing.isEmpty(), lang + " tilida kalitlar yo'q: " + missing);
            assertTrue(extra.isEmpty(), lang + " tilida ortiqcha kalitlar: " + extra);
        }
    }

    @Test
    void v3TextsExistInUzbekAndRussian() {
        for (String key : new String[]{"notif.morning", "notif.weekly_caption", "notif.quiet_bundle_title",
                "notif.btn.app_details", "notif.btn.write_class_teacher", "notif.tomorrow_needs", "set.item.morning",
                "notif.weekly_subjects", "menu.webapp"}) {
            for (String lang : BotI18n.LANGUAGES) {
                String text = I18N.t(lang, key);
                assertNotEquals(key, text, lang + ": " + key + " yo'q");
                assertFalse(text.isBlank(), lang + ": " + key);
            }
        }
        assertTrue(I18N.t("uz", "notif.morning", "name", "Aziza", "count", 5).contains("Aziza</b>ning <b>5</b> ta darsi"));
        assertTrue(I18N.t("ru", "notif.morning", "name", "Aziza", "count", 5).contains("Доброе утро"));
        assertEquals("📱 Ilovani ochish", I18N.t("uz", "menu.webapp"));
    }

    @Test
    void translationsUseTheSamePlaceholders() {
        for (String key : I18N.keys("uz")) {
            Set<String> expected = placeholders(I18N.t("uz", key));
            for (String lang : new String[]{"cy", "ru"}) {
                assertEquals(expected, placeholders(I18N.t(lang, key)), lang + ": " + key);
            }
        }
    }

    @Test
    void noTranslationIsEmpty_andHtmlTagsAreBalanced() {
        for (String lang : BotI18n.LANGUAGES) {
            for (String key : I18N.keys(lang)) {
                String text = I18N.t(lang, key);
                assertFalse(text.isBlank(), lang + ": " + key);
                for (String tag : new String[]{"b", "i", "blockquote", "code"}) {
                    assertEquals(count(text, "<" + tag + ">"), count(text, "</" + tag + ">"), lang + ": " + key + " <" + tag + ">");
                }
            }
        }
    }

    @Test
    void placeholdersAreFilled_inOnePass() {
        String text = I18N.t("uz", "link.success", "name", "Ali {class}", "class", "5-A");
        assertEquals("✅ <b>Tabriklaymiz!</b> Endi <b>Ali {class}</b> (5-A sinf) haqida hamma narsa shu yerda.", text);
    }

    @Test
    void unknownLanguageAndKey_fallBack() {
        assertEquals(I18N.t("uz", "common.back"), I18N.t("de", "common.back"));
        assertEquals("no.such.key", I18N.t("uz", "no.such.key"));
    }

    @Test
    void escapedLeadingAndTrailingSpacesSurvive() {
        assertEquals("🆕 ", I18N.t("uz", "ann.new"));
        assertTrue(I18N.t("uz", "sched.break").startsWith("      ☕"));
        assertEquals("🆕 ", I18N.t("cy", "ann.new"));
    }

    @Test
    void datesInAllLanguages() {
        LocalDate d = LocalDate.of(2026, 9, 29);
        assertEquals("29-sentabr, seshanba", I18N.dateFull("uz", d));
        assertEquals("29-сентябр, сешанба", I18N.dateFull("cy", d));
        assertEquals("29 сентября, вторник", I18N.dateFull("ru", d));
    }

    @Test
    void replyKeyboardLabelsMapBackToScreens_inEveryLanguage() {
        for (String lang : BotI18n.LANGUAGES) {
            for (String[] item : Keyboards.MAIN) {
                assertEquals(item[1], Keyboards.screenFor(I18N.t(lang, item[0])), lang + ": " + item[0]);
            }
        }
    }

    private static Set<String> placeholders(String s) {
        Set<String> result = new TreeSet<>();
        Matcher m = PLACEHOLDER.matcher(s);
        while (m.find()) result.add(m.group(1));
        return result;
    }

    private static int count(String s, String needle) {
        int n = 0;
        for (int i = s.indexOf(needle); i >= 0; i = s.indexOf(needle, i + 1)) n++;
        return n;
    }
}
