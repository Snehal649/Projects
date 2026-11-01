package com.fintrack.service;

import com.fintrack.model.Account;
import com.fintrack.model.TxType;
import com.fintrack.repo.AccountRepository;
import com.fintrack.repo.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final TransactionRepository txRepo;
    private final AccountRepository accounts;
    private final BudgetService budgetService;

    public Map<String, Object> summary(Long uid, YearMonth ym) {
        LocalDate from = ym.atDay(1), to = ym.atEndOfMonth();
        BigDecimal income = txRepo.sum(uid, TxType.INCOME, from, to);
        BigDecimal expense = txRepo.sum(uid, TxType.EXPENSE, from, to);
        BigDecimal netWorth = accounts.findByUserIdOrderByIdAsc(uid).stream()
                .map(Account::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> byCategory = new ArrayList<>();
        for (Object[] r : txRepo.sumByCategory(uid, TxType.EXPENSE, from, to)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("categoryId", r[0]);
            m.put("name", r[1]);
            m.put("amount", r[2]);
            byCategory.add(m);
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth m = ym.minusMonths(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", m.toString());
            row.put("income", txRepo.sum(uid, TxType.INCOME, m.atDay(1), m.atEndOfMonth()));
            row.put("expense", txRepo.sum(uid, TxType.EXPENSE, m.atDay(1), m.atEndOfMonth()));
            trend.add(row);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("income", income);
        out.put("expense", expense);
        out.put("savings", income.subtract(expense));
        out.put("netWorth", netWorth);
        out.put("byCategory", byCategory);
        out.put("trend", trend);
        out.put("budgets", budgetService.progress(uid, ym.getMonthValue(), ym.getYear()));
        out.put("recent", txRepo.search(uid, from, to, null, null, null, PageRequest.of(0, 6)).getContent());
        return out;
    }
}
