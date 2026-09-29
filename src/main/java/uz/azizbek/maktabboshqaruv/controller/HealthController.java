package uz.azizbek.maktabboshqaruv.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Unauthenticated liveness check the frontend polls before showing the
 * login form, so a down backend surfaces as a clear banner instead of a
 * spinner that never resolves.
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
