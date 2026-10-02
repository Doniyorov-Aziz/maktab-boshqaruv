package uz.azizbek.maktabboshqaruv.bot.image;

import uz.azizbek.maktabboshqaruv.bot.image.BannerRenderer.*;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Every section banner renders without errors, at 1280×640, in the section's color. */
class BannerRendererTest {

    private static BufferedImage read(byte[] png) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(png));
    }

    @Test
    void everySectionHasAColorAndIcon_andTheBriefsColors() {
        for (String s : List.of("home", "schedule", "attendance", "grades", "report", "behavior", "announcements",
                "events", "teachers", "write", "absence", "school", "settings", "children", "welcome")) {
            assertTrue(BannerService.SECTIONS.contains(s), s);
            assertNotNull(BannerService.COLORS.get(s), s);
        }
        assertEquals(new Color(0x4F46E5), BannerService.COLORS.get("schedule"));
        assertEquals(new Color(0x10B981), BannerService.COLORS.get("attendance"));
        assertEquals(new Color(0x0EA5E9), BannerService.COLORS.get("grades"));
        assertEquals(new Color(0x7C3AED), BannerService.COLORS.get("report"));
        assertEquals(new Color(0xEC4899), BannerService.COLORS.get("announcements"));
        assertEquals(new Color(0xF59E0B), BannerService.COLORS.get("events"));
        assertEquals(new Color(0x06B6D4), BannerService.COLORS.get("teachers"));
        assertEquals(new Color(0xEF4444), BannerService.COLORS.get("absence"));
        assertEquals(new Color(0x64748B), BannerService.COLORS.get("settings"));
    }

    @Test
    void allPanelKinds_andIcons_renderAt1280x640() throws IOException {
        List<String> statuses = new ArrayList<>(Collections.nCopies(30, "PRESENT"));
        statuses.set(9, "ABSENT");
        statuses.set(13, "LATE");
        statuses.set(7, "EXCUSED");
        statuses.set(5, "NONE");
        List<Panel> panels = List.of(
                new Stats("30-sentabr, chorshanba", List.of(new Stat("Bugungi darslar", "5", null),
                        new Stat("Bugungi holat", "Kechikdi", new Color(0xF59E0B)))),
                new Rows("Ertaga · 1-oktabr", List.of(new Row("08:30", "1. Ona tili", "12-xona"),
                        new Row("09:25", "2. O'zbek adabiyoti — juda uzun fan nomi kesilishi kerak", "Sport zali")), "—"),
                new Rows("Bo'sh ro'yxat", List.of(), "Ertaga dars yo'q — dam olish kuni"),
                new BannerRenderer.Calendar("Sentabr 2026", List.of("Du", "Se", "Ch", "Pa", "Ju", "Sh", "Ya"),
                        LocalDate.of(2026, 9, 1), statuses, LocalDate.of(2026, 9, 30)),
                new Bars("Fanlar bo'yicha o'rtacha", List.of(new Bar("Ona tili", 4.4, "UP"), new Bar("Musiqa", 3.0, "DOWN"),
                        new Bar("Jismoniy tarbiya", 5.0, "FLAT")), "Hali baho yo'q"),
                new Bars("Bo'sh", List.of(), "Hali baho yo'q"));
        int i = 0;
        for (Icon icon : Icon.values()) {
            Panel panel = panels.get(i++ % panels.size());
            byte[] png = BannerRenderer.render(new Banner(new Color(0x10B981), icon,
                    "Davomat — g'ayrat o'zbekcha ўғқҳ", "Elyor Berdiyev · 1-A", "87%", "davomat · Sentabr 2026",
                    "1-maktab · 30-sentabr, chorshanba", panel,
                    Map.of("PRESENT", "Keldi", "LATE", "Kechikdi", "ABSENT", "Kelmadi", "EXCUSED", "Sababli")));
            BufferedImage img = read(png);
            assertEquals(BannerRenderer.W, img.getWidth(), icon.name());
            assertEquals(BannerRenderer.H, img.getHeight(), icon.name());
            assertEquals(1280, img.getWidth());
            assertEquals(640, img.getHeight());
        }
    }

    @Test
    void backgroundUsesTheSectionColor() throws IOException {
        Color red = new Color(0xEF4444);
        BufferedImage img = read(BannerRenderer.render(new Banner(red, Icon.CROSS, "Sababli ariza", "Ali · 5-A",
                null, null, null, null, null)));
        Color corner = new Color(img.getRGB(2, 2));
        assertTrue(Math.abs(corner.getRed() - red.getRed()) < 12 && Math.abs(corner.getGreen() - red.getGreen()) < 12,
                "top-left corner should be the section color, was " + corner);
    }
}
