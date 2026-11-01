package com.fintrack.controller;

import com.fintrack.dto.Dtos.BudgetRequest;
import com.fintrack.model.Budget;
import com.fintrack.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {
    private final BudgetService service;

    @GetMapping
    public List<Map<String, Object>> list(@AuthenticationPrincipal Long uid,
                                          @RequestParam(required = false) Integer month,
                                          @RequestParam(required = false) Integer year) {
        LocalDate now = LocalDate.now();
        return service.progress(uid, month != null ? month : now.getMonthValue(), year != null ? year : now.getYear());
    }

    @PostMapping
    public Map<String, Object> upsert(@AuthenticationPrincipal Long uid, @Valid @RequestBody BudgetRequest r) {
        Budget b = service.upsert(uid, r);
        return Map.of("id", b.getId());
    }

    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal Long uid, @PathVariable Long id) { service.delete(uid, id); }
}
