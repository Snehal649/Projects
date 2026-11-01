package com.fintrack.controller;

import com.fintrack.common.ApiException;
import com.fintrack.dto.Dtos.*;
import com.fintrack.model.Goal;
import com.fintrack.repo.GoalRepository;
import com.fintrack.repo.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {
    private final GoalRepository repo;
    private final UserRepository users;

    @GetMapping
    public List<Goal> list(@AuthenticationPrincipal Long uid) { return repo.findByUserId(uid); }

    @PostMapping
    public Goal create(@AuthenticationPrincipal Long uid, @Valid @RequestBody GoalRequest r) {
        Goal g = new Goal();
        g.setUser(users.getReferenceById(uid));
        g.setName(r.name());
        g.setTargetAmount(r.targetAmount());
        g.setDeadline(r.deadline());
        return repo.save(g);
    }

    @PostMapping("/{id}/contribute")
    public Goal contribute(@AuthenticationPrincipal Long uid, @PathVariable Long id, @Valid @RequestBody AmountRequest r) {
        Goal g = find(uid, id);
        g.setSavedAmount(g.getSavedAmount().add(r.amount()));
        return repo.save(g);
    }

    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal Long uid, @PathVariable Long id) { repo.delete(find(uid, id)); }

    /** What-if simulator: "if I save X per month, when do I reach the goal?" */
    @GetMapping("/{id}/simulate")
    public Map<String, Object> simulate(@AuthenticationPrincipal Long uid, @PathVariable Long id,
                                        @RequestParam BigDecimal monthly) {
        Goal g = find(uid, id);
        BigDecimal remaining = g.getTargetAmount().subtract(g.getSavedAmount()).max(BigDecimal.ZERO);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("remaining", remaining);
        if (monthly.signum() > 0) {
            long months = remaining.divide(monthly, 0, RoundingMode.CEILING).longValue();
            LocalDate eta = LocalDate.now().plusMonths(months);
            out.put("monthsNeeded", months);
            out.put("completionDate", eta);
            out.put("onTrack", g.getDeadline() == null || !eta.isAfter(g.getDeadline()));
        } else {
            out.put("monthsNeeded", null);
            out.put("completionDate", null);
            out.put("onTrack", false);
        }
        if (g.getDeadline() != null && g.getDeadline().isAfter(LocalDate.now())) {
            long left = Math.max(1, ChronoUnit.MONTHS.between(LocalDate.now(), g.getDeadline()));
            out.put("requiredMonthly", remaining.divide(BigDecimal.valueOf(left), 0, RoundingMode.CEILING));
        } else {
            out.put("requiredMonthly", null);
        }
        return out;
    }

    private Goal find(Long uid, Long id) {
        return repo.findByIdAndUserId(id, uid).orElseThrow(() -> ApiException.notFound("Goal"));
    }
}
