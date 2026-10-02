package uz.azizbek.maktabboshqaruv.bot;

import uz.azizbek.maktabboshqaruv.telegram.TelegramJson;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.KeyboardButton;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.ReplyKeyboardMarkup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KeyboardsTest {

    @AfterEach
    void reset() {
        Keyboards.setWebAppUrl(null);
    }

    @Test
    void mainMenu_isTwoColumns_withChildrenOnAFullRow() {
        ReplyKeyboardMarkup kb = Keyboards.main("uz");
        List<List<KeyboardButton>> rows = kb.keyboard();
        assertEquals(6, rows.size());
        assertEquals(List.of("📅 Dars jadvali", "✅ Davomat"), rows.get(0).stream().map(KeyboardButton::text).toList());
        assertEquals(List.of("📘 Baholar", "📊 Hisobot"), rows.get(1).stream().map(KeyboardButton::text).toList());
        assertEquals(List.of("📢 E'lonlar", "🗓 Tadbirlar"), rows.get(2).stream().map(KeyboardButton::text).toList());
        assertEquals(List.of("👩‍🏫 O'qituvchilar", "💬 Maktabga yozish"), rows.get(3).stream().map(KeyboardButton::text).toList());
        assertEquals(List.of("🤒 Sababli ariza", "⚙️ Sozlamalar"), rows.get(4).stream().map(KeyboardButton::text).toList());
        assertEquals(List.of("👨‍👩‍👧 Farzandlarim"), rows.get(5).stream().map(KeyboardButton::text).toList());
    }

    @Test
    void mainMenu_isPersistent_resized_withPlaceholder_andColorStyles() {
        ReplyKeyboardMarkup kb = Keyboards.main("uz");
        assertTrue(kb.resizeKeyboard());
        assertTrue(kb.isPersistent());
        assertEquals("Bo'limni tanlang 👇", kb.inputFieldPlaceholder());
        assertEquals("danger", kb.keyboard().get(4).get(0).style(), "Sababli ariza — warning color");
        assertEquals("primary", kb.keyboard().get(0).get(0).style());

        String json = TelegramJson.write(kb);
        assertTrue(json.contains("\"is_persistent\":true"), json);
        assertTrue(json.contains("\"input_field_placeholder\""), json);
        assertTrue(json.contains("\"style\":\"danger\""), json);
    }

    @Test
    void miniAppRow_onTop_onlyWhenConfigured() {
        assertNull(Keyboards.main("uz").keyboard().get(0).get(0).webApp());
        Keyboards.setWebAppUrl("https://kundalik.example.uz/#/webapp");
        List<KeyboardButton> top = Keyboards.main("uz").keyboard().get(0);
        assertEquals(1, top.size());
        assertEquals("📱 Kundalikni ochish", top.get(0).text());
        assertEquals("https://kundalik.example.uz/#/webapp", top.get(0).webApp().url());
        assertEquals(7, Keyboards.main("uz").keyboard().size());
    }

    @Test
    void labels_areRecognised_inAnyCase_withOrWithoutEmoji_inAllLanguages() {
        assertEquals("att", Keyboards.screenFor("✅ Davomat"));
        assertEquals("att", Keyboards.screenFor("davomat"));
        assertEquals("att", Keyboards.screenFor("  DAVOMAT "));
        assertEquals("tch", Keyboards.screenFor("O‘qituvchilar"));
        assertEquals("tch", Keyboards.screenFor("oqituvchilar"));
        assertEquals("abs", Keyboards.screenFor("sababli ariza"));
        assertEquals("ann", Keyboards.screenFor("Elonlar"));
        assertEquals("sch", Keyboards.screenFor("jadval"));
        assertEquals("beh", Keyboards.screenFor("xulq"));
        assertEquals("home", Keyboards.screenFor("bosh menyu"));
        assertEquals("att", Keyboards.screenFor("Посещаемость"));
        assertEquals("gr", Keyboards.screenFor("📘 Баҳолар"));
        assertEquals("ch", Keyboards.screenFor("фарзандларим"));
        assertNull(Keyboards.screenFor("salom, qalaysiz?"));
        assertNull(Keyboards.screenFor("🙂"));
        assertNull(Keyboards.screenFor(null));
    }

    @Test
    void everyLanguageHasAllMenuLabels() {
        for (String lang : BotI18n.LANGUAGES) {
            for (List<KeyboardButton> row : Keyboards.main(lang).keyboard()) {
                for (KeyboardButton b : row) {
                    assertFalse(b.text().contains("kb."), lang + ": " + b.text());
                    assertNotNull(Keyboards.screenFor(b.text()), lang + ": " + b.text());
                }
            }
        }
    }
}
