package com.fintrack.service;

import com.fintrack.model.*;
import com.fintrack.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/** Smart features: safe-to-spend, forecast, anomaly detection, health score, subscription detection. */
@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final TransactionRepository txRepo;
    private final BudgetRepository budgetRepo;
    private final AccountRepository accounts;
    private final BudgetService budgetService;

    /** How much can I spend per day for the rest of this month? */
    public Map<String, Object> safeToSpend(Long uid) {
        LocalDate today = LocalDate.now();
        YearMonth ym = YearMonth.from(today);
        BigDecimal budget = budgetRepo.findByUserIdAndMonthAndYear(uid, ym.getMonthValue(), ym.getYear()).stream()
                .map(Budget::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        String basis = "budgets";
        if (budget.signum() == 0) {   // no budgets set: fall back to last month's spending
            YearMonth prev = ym.minusMonths(1);
            budget = txRepo.sum(uid, TxType.EXPENSE, prev.atDay(1), prev.atEndOfMonth());
            basis = "last month's spending";
        }
        BigDecimal spent = txRepo.sum(uid, TxType.EXPENSE, ym.atDay(1), today);
        int daysLeft = ym.lengthOfMonth() - today.getDayOfMonth() + 1;
        BigDecimal remaining = budget.subtract(spent);
        BigDecimal perDay = remaining.signum() > 0
                ? remaining.divide(BigDecimal.valueOf(daysLeft), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("budget", budget);
        m.put("spent", spent);
        m.put("remaining", remaining);
        m.put("daysLeft", daysLeft);
        m.put("perDay", perDay);
        m.put("basis", basis);
        return m;
    }

    /** Linear projection of month-end spending plus per-budget overspend warnings. */
    public Map<String, Object> forecast(Long uid) {
        LocalDate today = LocalDate.now();
        YearMonth ym = YearMonth.from(today);
        int elapsed = today.getDayOfMonth(), len = ym.lengthOfMonth();
        BigDecimal spent = txRepo.sum(uid, TxType.EXPENSE, ym.atDay(1), today);
        BigDecimal projected = spent.multiply(BigDecimal.valueOf(len))
                .divide(BigDecimal.valueOf(elapsed), 2, RoundingMode.HALF_UP);

        List<String> warnings = new ArrayList<>();
        for (Map<String, Object> b : budgetService.progress(uid, ym.getMonthValue(), ym.getYear())) {
            BigDecimal budget = (BigDecimal) b.get("budget");
            BigDecimal s = (BigDecimal) b.get("spent");
            String name = (String) b.get("category");
            if (s.signum() == 0) continue;
            if (s.compareTo(budget) >= 0) {
                warnings.add("You have already used your full " + name + " budget this month.");
                continue;
            }
            BigDecimal daily = s.divide(BigDecimal.valueOf(elapsed), 4, RoundingMode.HALF_UP);
            if (daily.multiply(BigDecimal.valueOf(len)).compareTo(budget) > 0) {
                int day = budget.divide(daily, 0, RoundingMode.DOWN).intValue();
                warnings.add("At this pace you will pass your " + name + " budget around day "
                        + Math.min(day, len) + ".");
            }
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("spentSoFar", spent);
        m.put("projectedExpense", projected);
        m.put("warnings", warnings);
        return m;
    }

    /** Flags expenses far above the usual for their category (z-score) and possible duplicate charges. */
    public List<Map<String, Object>> anomalies(Long uid) {
        LocalDate today = LocalDate.now();
        List<Transaction> hist = txRepo.findByUserIdAndTypeAndDateBetween(uid, TxType.EXPENSE, today.minusDays(90), today);
        Map<Long, List<Double>> byCat = hist.stream().collect(Collectors.groupingBy(
                t -> t.getCategory().getId(),
                Collectors.mapping(t -> t.getAmount().doubleValue(), Collectors.toList())));

        List<Map<String, Object>> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        hist.sort(Comparator.comparing(Transaction::getDate));
        for (Transaction t : hist) {
            boolean recent = !t.getDate().isBefore(today.minusDays(30));
            String key = (t.getMerchant() == null ? "" : t.getMerchant().toLowerCase())
                    + "|" + t.getAmount() + "|" + t.getDate();
            boolean duplicate = t.getMerchant() != null && !seen.add(key);
            if (!recent) continue;

            List<Double> vals = byCat.get(t.getCategory().getId());
            if (vals.size() >= 5) {
                double mean = vals.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                double var = vals.stream().mapToDouble(v -> (v - mean) * (v - mean)).sum() / vals.size();
                double std = Math.sqrt(var);
                double z = std == 0 ? 0 : (t.getAmount().doubleValue() - mean) / std;
                if (z > 2.5) {
                    out.add(anomaly(t, String.format("Much higher than your usual %s spend (average about %.0f)",
                            t.getCategory().getName(), mean)));
                    continue;
                }
            }
            if (duplicate) out.add(anomaly(t, "Possible duplicate: same merchant, amount and date"));
        }
        return out;
    }

    private Map<String, Object> anomaly(Transaction t, String reason) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("merchant", t.getMerchant() == null ? t.getCategory().getName() : t.getMerchant());
        m.put("amount", t.getAmount());
        m.put("date", t.getDate());
        m.put("reason", reason);
        return m;
    }

    /** 0-100 score: savings rate (40), budget adherence (30), logging consistency (15), emergency cushion (15). */
    public Map<String, Object> healthScore(Long uid) {
        LocalDate today = LocalDate.now();
        YearMonth ym = YearMonth.from(today);
        BigDecimal income = txRepo.sum(uid, TxType.INCOME, ym.atDay(1), today);
        BigDecimal expense = txRepo.sum(uid, TxType.EXPENSE, ym.atDay(1), today);

        double savingsRate = income.signum() > 0
                ? income.subtract(expense).doubleValue() / income.doubleValue() : 0;
        double savingsScore = clamp(savingsRate / 0.20) * 40;

        List<Map<String, Object>> progress = budgetService.progress(uid, ym.getMonthValue(), ym.getYear());
        double adherence = progress.isEmpty() ? 0.5 :
                progress.stream().filter(b -> (double) b.get("percent") <= 100).count() / (double) progress.size();
        double budgetScore = adherence * 30;

        long activeDays = txRepo.findByUserIdAndDateBetween(uid, today.minusDays(29), today).stream()
                .map(Transaction::getDate).distinct().count();
        double consistencyScore = clamp(activeDays / 15.0) * 15;

        BigDecimal netWorth = accounts.findByUserIdOrderByIdAsc(uid).stream()
                .map(Account::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        YearMonth prev = ym.minusMonths(1);
        double monthlyExpense = txRepo.sum(uid, TxType.EXPENSE, prev.atDay(1), prev.atEndOfMonth()).doubleValue();
        if (monthlyExpense == 0) monthlyExpense = expense.doubleValue();
        double coverageMonths = monthlyExpense > 0 ? netWorth.doubleValue() / monthlyExpense : 0;
        double cushionScore = clamp(coverageMonths / 6.0) * 15;

        List<String> tips = new ArrayList<>();
        if (savingsScore < 24) tips.add("Aim to save at least 20% of your income this month.");
        if (progress.isEmpty()) tips.add("Set category budgets so we can track your adherence.");
        else if (budgetScore < 20) tips.add("Some budgets are exceeded. Review the categories marked red.");
        if (consistencyScore < 10) tips.add("Log expenses more often. Daily logging gives better insights.");
        if (cushionScore < 10) tips.add("Build an emergency fund worth 6 months of expenses.");

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("score", (int) Math.round(savingsScore + budgetScore + consistencyScore + cushionScore));
        m.put("savingsRate", Math.round(savingsRate * 1000) / 10.0);
        m.put("emergencyMonths", Math.round(coverageMonths * 10) / 10.0);
        m.put("tips", tips);
        return m;
    }

    /** Detects roughly monthly charges with a stable amount from the same merchant. */
    public List<Map<String, Object>> subscriptions(Long uid) {
        LocalDate today = LocalDate.now();
        List<Transaction> hist = txRepo.findByUserIdAndTypeAndDateBetween(uid, TxType.EXPENSE, today.minusDays(180), today);
        Map<String, List<Transaction>> byMerchant = hist.stream()
                .filter(t -> t.getMerchant() != null && !t.getMerchant().isBlank())
                .collect(Collectors.groupingBy(t -> t.getMerchant().trim().toLowerCase()));

        List<Map<String, Object>> out = new ArrayList<>();
        for (var e : byMerchant.entrySet()) {
            List<Transaction> list = new ArrayList<>(e.getValue());
            if (list.size() < 2) continue;
            list.sort(Comparator.comparing(Transaction::getDate));
            double gaps = 0;
            for (int i = 1; i < list.size(); i++)
                gaps += ChronoUnit.DAYS.between(list.get(i - 1).getDate(), list.get(i).getDate());
            double avgGap = gaps / (list.size() - 1);
            double min = list.stream().mapToDouble(t -> t.getAmount().doubleValue()).min().orElse(0);
            double max = list.stream().mapToDouble(t -> t.getAmount().doubleValue()).max().orElse(0);
            double avg = list.stream().mapToDouble(t -> t.getAmount().doubleValue()).average().orElse(0);
            if (avgGap >= 26 && avgGap <= 35 && (max - min) <= 0.1 * avg) {
                LocalDate last = list.get(list.size() - 1).getDate();
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("merchant", list.get(0).getMerchant());
                m.put("amount", Math.round(avg * 100) / 100.0);
                m.put("lastCharged", last);
                m.put("nextExpected", last.plusDays(Math.round(avgGap)));
                m.put("yearlyCost", Math.round(avg * 12));
                out.add(m);
            }
        }
        return out;
    }

    private static double clamp(double v) { return Math.max(0, Math.min(1, v)); }
}
