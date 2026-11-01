package com.fintrack.service;

import com.fintrack.common.ApiException;
import com.fintrack.dto.Dtos.BudgetRequest;
import com.fintrack.model.*;
import com.fintrack.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BudgetService {
    private final BudgetRepository budgets;
    private final TransactionRepository txRepo;
    private final CategoryRepository categories;
    private final UserRepository users;

    /** Budgets for a month with spent amount and percentage used (UI alerts at 80% and 100%). */
    public List<Map<String, Object>> progress(Long uid, int month, int year) {
        YearMonth ym = YearMonth.of(year, month);
        Map<Long, BigDecimal> spent = new HashMap<>();
        for (Object[] r : txRepo.sumByCategory(uid, TxType.EXPENSE, ym.atDay(1), ym.atEndOfMonth()))
            spent.put((Long) r[0], (BigDecimal) r[2]);

        List<Map<String, Object>> out = new ArrayList<>();
        for (Budget b : budgets.findByUserIdAndMonthAndYear(uid, month, year)) {
            BigDecimal s = spent.getOrDefault(b.getCategory().getId(), BigDecimal.ZERO);
            double pct = b.getAmount().signum() == 0 ? 0 :
                    s.multiply(BigDecimal.valueOf(100)).divide(b.getAmount(), 1, RoundingMode.HALF_UP).doubleValue();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", b.getId());
            m.put("categoryId", b.getCategory().getId());
            m.put("category", b.getCategory().getName());
            m.put("budget", b.getAmount());
            m.put("spent", s);
            m.put("percent", pct);
            out.add(m);
        }
        return out;
    }

    @Transactional
    public Budget upsert(Long uid, BudgetRequest r) {
        Category c = categories.findByIdAndUserId(r.categoryId(), uid)
                .orElseThrow(() -> ApiException.notFound("Category"));
        Budget b = budgets.findByUserIdAndCategoryIdAndMonthAndYear(uid, c.getId(), r.month(), r.year())
                .orElseGet(() -> {
                    Budget n = new Budget();
                    n.setUser(users.getReferenceById(uid));
                    n.setCategory(c);
                    n.setMonth(r.month());
                    n.setYear(r.year());
                    return n;
                });
        b.setAmount(r.amount());
        return budgets.save(b);
    }

    @Transactional
    public void delete(Long uid, Long id) {
        budgets.delete(budgets.findByIdAndUserId(id, uid).orElseThrow(() -> ApiException.notFound("Budget")));
    }
}
