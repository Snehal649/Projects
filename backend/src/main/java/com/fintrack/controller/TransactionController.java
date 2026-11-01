package com.fintrack.controller;

import com.fintrack.dto.Dtos.*;
import com.fintrack.model.Transaction;
import com.fintrack.model.TxType;
import com.fintrack.service.QuickAddService;
import com.fintrack.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService service;
    private final QuickAddService quickAdd;

    @GetMapping
    public Map<String, Object> list(@AuthenticationPrincipal Long uid,
                                    @RequestParam(required = false) LocalDate from,
                                    @RequestParam(required = false) LocalDate to,
                                    @RequestParam(required = false) Long category,
                                    @RequestParam(required = false) Long account,
                                    @RequestParam(required = false) TxType type,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        Page<Transaction> p = service.search(uid, from, to, category, account, type, page, size);
        return Map.of("content", p.getContent(), "page", p.getNumber(),
                "totalPages", p.getTotalPages(), "totalElements", p.getTotalElements());
    }

    @PostMapping
    public Transaction create(@AuthenticationPrincipal Long uid, @Valid @RequestBody TransactionRequest r) {
        return service.create(uid, r);
    }

    @PutMapping("/{id}")
    public Transaction update(@AuthenticationPrincipal Long uid, @PathVariable Long id,
                              @Valid @RequestBody TransactionRequest r) {
        return service.update(uid, id, r);
    }

    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal Long uid, @PathVariable Long id) {
        service.delete(uid, id);
    }

    /** Natural-language preview: "Paid 1200 for petrol yesterday using HDFC" -> draft transaction (not saved). */
    @PostMapping("/quick-add/parse")
    public Draft parse(@AuthenticationPrincipal Long uid, @Valid @RequestBody QuickAddRequest r) {
        return quickAdd.parse(uid, r.text());
    }
}
