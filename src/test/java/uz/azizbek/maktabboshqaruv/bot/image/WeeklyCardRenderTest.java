package uz.azizbek.maktabboshqaruv.bot.image;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The Saturday picture is a 1080×1350 PNG in both moods: "all good" and "needs attention". */
class WeeklyCardRenderTest {

    private static BotImageRenderer.WeeklyInput input(List<BotImageRenderer.SubjectBar> bars, Double average,
                                                      String best, String attention) {
        return new BotImageRenderer.WeeklyInput("Haftalik hisobot", "5-oktabr – 10-oktabr", "Ali Valiyev",
                "5-A · 1-maktab", "Davomat", 92.0, "O'rtacha baho", average, bars.size() + " ta baho",
                "Fanlar bo'yicha o'rtacha baho", bars, "Bu hafta baho qo'yilmagan",
                "Eng yaxshi fan", best, "E'tibor kerak", attention, "Barcha fanlarda", "4 va 5 baholar", "—",
                "1-maktab · Maktab Boshqaruv");
    }

    private static BufferedImage render(BotImageRenderer.WeeklyInput in, String file) throws Exception {
        byte[] png = BotImageRenderer.weeklyCard(in);
        Path dir = Path.of("build", "weekly-preview");
        Files.createDirectories(dir);
        Files.write(dir.resolve(file), png);
        return ImageIO.read(new ByteArrayInputStream(png));
    }

    @Test
    void attentionCard_isPortrait1080x1350() throws Exception {
        BufferedImage img = render(input(List.of(
                new BotImageRenderer.SubjectBar("Ona tili", 5.0, "UP"),
                new BotImageRenderer.SubjectBar("Matematika", 4.5, "FLAT"),
                new BotImageRenderer.SubjectBar("Ingliz tili", 4.0, "FLAT"),
                new BotImageRenderer.SubjectBar("Tarix", 4.0, "DOWN"),
                new BotImageRenderer.SubjectBar("Fizika", 3.5, "DOWN"),
                new BotImageRenderer.SubjectBar("Kimyo", 3.0, "DOWN")), 4.0, "Ona tili", "Kimyo"), "attention.png");
        assertEquals(1080, img.getWidth());
        assertEquals(1350, img.getHeight());
    }

    @Test
    void allGoodAndEmptyWeeks_render() throws Exception {
        BufferedImage good = render(input(List.of(new BotImageRenderer.SubjectBar("Matematika", 5.0, "UP")),
                5.0, "Matematika", null), "all-good.png");
        BufferedImage empty = render(input(List.of(), null, null, null), "empty.png");
        assertEquals(1350, good.getHeight());
        assertEquals(1350, empty.getHeight());
    }
}
