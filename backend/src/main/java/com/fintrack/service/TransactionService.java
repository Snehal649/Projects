package com.fintrack.service;

import com.fintrack.common.ApiException;
import com.fintrack.dto.Dtos.TransactionRequest;
import com.fintrack.model.*;
import com.fintrack.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository txRepo;
    private final AccountRepository accounts;
    private final CategoryRepository categories;
    private final UserRepository users;

    public Page<Transaction> search(Long uid, LocalDate from, LocalDate to, Long cat, Long acc,
                                    TxType type, int page, int size) {
        LocalDate f = from != null ? from : LocalDate.of(1970, 1, 1);
        LocalDate t = to != null ? to : LocalDate.of(2100, 1, 1);
        return txRepo.search(uid, f, t, cat, acc, type, PageRequest.of(page, Math.min(size, 100)));
    }

    @Transactional
    public Transaction create(Long uid, TransactionRequest r) {
        Transaction t = new Transaction();
        t.setUser(users.getReferenceById(uid));
        fill(uid, t, r);
        adjustBalance(t.getAccount(), t.getType(), t.getAmount(), 1);
        return txRepo.save(t);
    }

    @Transactional
    public Transaction update(Long uid, Long id, TransactionRequest r) {
        Transaction t = txRepo.findByIdAndUserId(id, uid).orElseThrow(() -> ApiException.notFound("Transaction"));
        adjustBalance(t.getAccount(), t.getType(), t.getAmount(), -1);   // undo the old effect
        fill(uid, t, r);
        adjustBalance(t.getAccount(), t.getType(), t.getAmount(), 1);    // apply the new effect
        return txRepo.save(t);
    }

    @Transactional
    public void delete(Long uid, Long id) {
        Transaction t = txRepo.findByIdAndUserId(id, uid).orElseThrow(() -> ApiException.notFound("Transaction"));
        adjustBalance(t.getAccount(), t.getType(), t.getAmount(), -1);
        txRepo.delete(t);
    }

    private void fill(Long uid, Transaction t, TransactionRequest r) {
        t.setAccount(accounts.findByIdAndUserId(r.accountId(), uid).orElseThrow(() -> ApiException.notFound("Account")));
        Category c = categories.findByIdAndUserId(r.categoryId(), uid).orElseThrow(() -> ApiException.notFound("Category"));
        t.setCategory(c);
        t.setAmount(r.amount());
        t.setType(r.type());
        t.setDate(r.date());
        t.setMerchant(r.merchant() == null || r.merchant().isBlank() ? null : r.merchant().trim());
        t.setNote(r.note());
    }

    /** sign = +1 applies the transaction to the balance, -1 reverses it. */
    private void adjustBalance(Account a, TxType type, BigDecimal amount, int sign) {
        BigDecimal delta = type == TxType.INCOME ? amount : amount.negate();
        a.setBalance(a.getBalance().add(delta.multiply(BigDecimal.valueOf(sign))));
    }
}
