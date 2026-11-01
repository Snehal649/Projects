package com.fintrack.controller;

import com.fintrack.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    private final AnalyticsService service;

    @GetMapping("/safe-to-spend")
    public Map<String, Object> safeToSpend(@AuthenticationPrincipal Long uid) { return service.safeToSpend(uid); }

    @GetMapping("/forecast")
    public Map<String, Object> forecast(@AuthenticationPrincipal Long uid) { return service.forecast(uid); }

    @GetMapping("/anomalies")
    public List<Map<String, Object>> anomalies(@AuthenticationPrincipal Long uid) { return service.anomalies(uid); }

    @GetMapping("/health-score")
    public Map<String, Object> health(@AuthenticationPrincipal Long uid) { return service.healthScore(uid); }

    @GetMapping("/subscriptions")
    public List<Map<String, Object>> subscriptions(@AuthenticationPrincipal Long uid) { return service.subscriptions(uid); }
}
