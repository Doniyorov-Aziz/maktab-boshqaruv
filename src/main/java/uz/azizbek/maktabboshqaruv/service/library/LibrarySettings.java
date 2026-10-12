package uz.azizbek.maktabboshqaruv.service.library;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** Library limits from application.properties ({@code app.library.*}); every value has a default. */
@Component
public class LibrarySettings {

    private final long maxPdfBytes;
    private final long maxCoverBytes;
    private final int dailyDownloadLimit;
    private final int searchIdleMinutes;
    private final DayOfWeek weeklyDay;
    private final LocalTime weeklyTime;

    public LibrarySettings(@Value("${app.library.max-pdf-mb:50}") int maxPdfMb,
                           @Value("${app.library.max-cover-mb:2}") int maxCoverMb,
                           @Value("${app.library.daily-download-limit:20}") int dailyDownloadLimit,
                           @Value("${app.library.search-idle-minutes:10}") int searchIdleMinutes,
                           @Value("${app.library.weekly-day:FRIDAY}") DayOfWeek weeklyDay,
                           @Value("${app.library.weekly-time:19:00}") LocalTime weeklyTime) {
        this.maxPdfBytes = maxPdfMb * 1024L * 1024L;
        this.maxCoverBytes = maxCoverMb * 1024L * 1024L;
        this.dailyDownloadLimit = dailyDownloadLimit;
        this.searchIdleMinutes = searchIdleMinutes;
        this.weeklyDay = weeklyDay;
        this.weeklyTime = weeklyTime;
    }

    public long maxPdfBytes() {
        return maxPdfBytes;
    }

    public long maxCoverBytes() {
        return maxCoverBytes;
    }

    public int dailyDownloadLimit() {
        return dailyDownloadLimit;
    }

    public int searchIdleMinutes() {
        return searchIdleMinutes;
    }

    public DayOfWeek weeklyDay() {
        return weeklyDay;
    }

    public LocalTime weeklyTime() {
        return weeklyTime;
    }
}
