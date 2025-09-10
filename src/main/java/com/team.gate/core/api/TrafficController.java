package com.team.gate.core.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.*;

@RestController
public class TrafficController {

    @GetMapping("/api/traffic")
    public Map<String, Object> traffic(
            @RequestParam(value = "minutes", defaultValue = "10") int minutes,
            @RequestParam(value = "step", defaultValue = "60") int stepSeconds
    ) {
        minutes = Math.max(1, Math.min(minutes, 120));
        stepSeconds = Math.max(5, Math.min(stepSeconds, 300));

        long now = Instant.now().toEpochMilli();
        long stepMs = stepSeconds * 1000L;
        int pointsCount = (int) Math.max(1, (minutes * 60L) / stepSeconds);

        List<Map<String, Object>> points = new ArrayList<>(pointsCount);
        Random rnd = new Random();
        for (int i = pointsCount - 1; i >= 0; i--) {
            long t = now - i * stepMs;
            double phase = (pointsCount - i) / 6.0;
            double base = 120 + 5 * Math.sin(phase);
            double jitter = rnd.nextGaussian() * 4;
            long value = Math.max(0, Math.round(base + jitter));
            points.add(Map.of("t", t, "v", value));
        }

        Map<String, Object> series = Map.of(
                "label", "requests_per_min",
                "points", points
        );
        return Map.of("series", List.of(series));
    }
}
