package com.fintrack.service;

import com.fintrack.common.ApiException;
import com.fintrack.dto.Dtos.Draft;
import com.fintrack.model.*;
import com.fintrack.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Rule-based natural language parser. Swap parse() for an LLM call later without touching the API. */
@Service
@RequiredArgsConstructor
public class QuickAddService {
    private final AccountRepository accounts;
    private final CategoryRepository categories;

    private static final Pattern AMOUNT = Pattern.compile("(?:rs\\.?|inr|₹)?\\s*(\\d+(?:,\\d{2,3})*(?:\\.\\d+)?)\\s*(k\\b)?");
    private static final Pattern AGO = Pattern.compile("(\\d+)\\s+days?\\s+ago");
    private static final Pattern INCOME_WORDS =
            Pattern.compile("\\b(salary|received|credited|earned|refund|bonus|income|cashback|freelance)\\b");
    private static final Set<String> STOP = Set.of("paid", "pay", "for", "using", "via", "on", "at", "to", "from", "the", "a",
            "an", "rs", "inr", "with", "spent", "bought", "my", "card", "got", "received", "of", "in", "by");

    private static final Map<String, String> EXPENSE_KW = new LinkedHashMap<>();
    private static final Map<String, String> INCOME_KW = new LinkedHashMap<>();
    static {
        for (String k : "swiggy zomato restaurant lunch dinner breakfast coffee cafe grocery groceries bigbasket blinkit snacks".split(" "))
            EXPENSE_KW.put(k, "Food");
        for (String k : "petrol diesel fuel uber ola rapido metro bus cab auto train flight".split(" "))
            EXPENSE_KW.put(k, "Transport");
        for (String k : "amazon flipkart myntra shopping clothes shoes".split(" ")) EXPENSE_KW.put(k, "Shopping");
        for (String k : "netflix spotify hotstar prime movie cinema game games".split(" ")) EXPENSE_KW.put(k, "Entertainment");
        for (String k : "electricity water gas wifi broadband recharge bill airtel jio".split(" ")) EXPENSE_KW.put(k, "Bills");
        EXPENSE_KW.put("rent", "Rent");
        for (String k : "doctor medicine pharmacy hospital clinic".split(" ")) EXPENSE_KW.put(k, "Health");
        for (String k : "course books fees tuition udemy".split(" ")) EXPENSE_KW.put(k, "Education");
        INCOME_KW.put("salary", "Salary");
        INCOME_KW.put("freelance", "Freelance");
    }

    public Draft parse(Long uid, String text) {
        String lower = text.trim().toLowerCase();

        // 1. date
        LocalDate date = LocalDate.now();
        if (lower.contains("day before yesterday")) {
            date = date.minusDays(2);
            lower = lower.replace("day before yesterday", " ");
        } else if (lower.contains("yesterday")) {
            date = date.minusDays(1);
            lower = lower.replace("yesterday", " ");
        } else if (AGO.matcher(lower).find()) {
            Matcher m = AGO.matcher(lower);
            m.find();
            date = date.minusDays(Integer.parseInt(m.group(1)));
            lower = m.replaceFirst(" ");
        } else {
            lower = lower.replace("today", " ");
        }

        // 2. amount
        Matcher am = AMOUNT.matcher(lower);
        if (!am.find()) throw new ApiException(HttpStatus.BAD_REQUEST, "Could not find an amount. Try: \"Swiggy 450\"");
        BigDecimal amount = new BigDecimal(am.group(1).replace(",", ""));
        if (am.group(2) != null) amount = amount.multiply(BigDecimal.valueOf(1000));
        String rest = lower.replace(am.group(), " ");

        // 3. type
        TxType type = INCOME_WORDS.matcher(lower).find() ? TxType.INCOME : TxType.EXPENSE;

        // 4. account (match by name or by its first word), default = first account
        List<Account> accs = accounts.findByUserIdOrderByIdAsc(uid);
        if (accs.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Create an account first");
        Account account = accs.get(0);
        Set<String> accountWords = new HashSet<>();
        for (Account a : accs) {
            String n = a.getName().toLowerCase();
            String first = n.split("\\s+")[0];
            if (lower.contains(n) || (first.length() > 2 && lower.matches(".*\\b" + Pattern.quote(first) + "\\b.*"))) {
                account = a;
                accountWords.addAll(Arrays.asList(n.split("\\s+")));
                break;
            }
        }

        // 5. category
        List<Category> cats = categories.findByUserIdOrderByNameAsc(uid).stream()
                .filter(c -> c.getType() == type).toList();
        Map<String, String> kw = type == TxType.INCOME ? INCOME_KW : EXPENSE_KW;
        Category category = null;
        for (var e : kw.entrySet()) {
            if (lower.matches(".*\\b" + e.getKey() + "\\b.*")) {
                category = find(cats, e.getValue());
                if (category != null) break;
            }
        }
        if (category == null) {
            for (Category c : cats) {
                if (lower.contains(c.getName().toLowerCase())) { category = c; break; }
            }
        }
        if (category == null) category = find(cats, type == TxType.INCOME ? "Other Income" : "Others");
        if (category == null && !cats.isEmpty()) category = cats.get(0);
        if (category == null) throw new ApiException(HttpStatus.BAD_REQUEST, "No categories found");

        // 6. merchant = what is left after removing filler words
        StringBuilder sb = new StringBuilder();
        for (String w : rest.replaceAll("[^a-z0-9&' ]", " ").trim().split("\\s+")) {
            if (w.isBlank() || STOP.contains(w) || accountWords.contains(w)) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        String merchant = sb.length() == 0 ? null : sb.toString();

        return new Draft(amount, type, date, account.getId(), category.getId(), merchant, text.trim());
    }

    private Category find(List<Category> cats, String name) {
        return cats.stream().filter(c -> c.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }
}
