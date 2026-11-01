package com.fintrack.controller;

import com.fintrack.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.YearMonth;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService service;

    @GetMapping("/summary")
    public Map<String, Object> summary(@AuthenticationPrincipal Long uid, @RequestParam(required = false) String month) {
        return service.summary(uid, month == null ? YearMonth.now() : YearMonth.parse(month));
    }
}
