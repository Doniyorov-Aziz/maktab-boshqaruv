package uz.azizbek.maktabboshqaruv.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uz.azizbek.maktabboshqaruv.config.GuestPolicy;
import uz.azizbek.maktabboshqaruv.service.DemoVisitService;

import java.util.List;
import java.util.Map;

/** The public demo's visitor count: the guest's browser reports pages, the administrator reads the numbers. */
@RestController
public class DemoVisitController {

    private final DemoVisitService visits;

    public DemoVisitController(DemoVisitService visits) {
        this.visits = visits;
    }

    public record Ping(String visitorId, String ref, List<String> paths) {
    }

    /** The only write a guest may make. Anyone else (staff testing the demo) is not counted. */
    @PostMapping("/api/demo/visit/ping")
    public Map<String, String> ping(@RequestBody Ping body, Authentication auth, HttpServletRequest http) {
        if (!GuestPolicy.isGuest(auth)) return Map.of("result", DemoVisitService.Result.IGNORED.name());
        DemoVisitService.Result r = visits.ping(body.visitorId(), body.ref(), body.paths(),
                http.getHeader("User-Agent"), http.getRemoteAddr());
        return Map.of("result", r.name());
    }

    /** Home page widget: "👀 Demo tashriflari: 20 kishi · bugun 3". */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/demo/visits/summary")
    public DemoVisitService.Summary summary() {
        return visits.summary();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/demo/visits/stats")
    public DemoVisitService.Stats stats() {
        return visits.stats();
    }
}
