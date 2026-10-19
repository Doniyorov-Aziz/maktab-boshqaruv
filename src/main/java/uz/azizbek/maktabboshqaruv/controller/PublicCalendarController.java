package uz.azizbek.maktabboshqaruv.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uz.azizbek.maktabboshqaruv.service.pages.CalendarPageService;

/**
 * The calendar subscription link for Google / Apple calendars: no sign-in, the secret token in the
 * path is the key ("…/api/public/calendar/AbC…xyz.ics"). A revoked or unknown token is a 404.
 */
@RestController
public class PublicCalendarController {

    private final CalendarPageService calendar;

    public PublicCalendarController(CalendarPageService calendar) {
        this.calendar = calendar;
    }

    @GetMapping("/api/public/calendar/{file:.+}")
    public ResponseEntity<byte[]> feed(@PathVariable String file, @RequestHeader(value = "Host", required = false) String host) {
        String token = file.endsWith(".ics") ? file.substring(0, file.length() - 4) : file;
        return PagesController.ics(calendar.feed(token, PagesController.hostOf(host)), "taqvim.ics");
    }
}
