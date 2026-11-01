package com.fintrack.controller;

import com.fintrack.common.ApiException;
import com.fintrack.dto.Dtos.AccountRequest;
import com.fintrack.model.Account;
import com.fintrack.repo.AccountRepository;
import com.fintrack.repo.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountRepository repo;
    private final UserRepository users;

    @GetMapping
    public List<Account> list(@AuthenticationPrincipal Long uid) { return repo.findByUserIdOrderByIdAsc(uid); }

    @PostMapping
    public Account create(@AuthenticationPrincipal Long uid, @Valid @RequestBody AccountRequest r) {
        Account a = new Account();
        a.setUser(users.getReferenceById(uid));
        a.setName(r.name());
        a.setType(r.type());
        a.setBalance(r.balance() == null ? BigDecimal.ZERO : r.balance());
        return repo.save(a);
    }

    @PutMapping("/{id}")
    public Account rename(@AuthenticationPrincipal Long uid, @PathVariable Long id, @Valid @RequestBody AccountRequest r) {
        Account a = repo.findByIdAndUserId(id, uid).orElseThrow(() -> ApiException.notFound("Account"));
        a.setName(r.name());
        a.setType(r.type());
        return repo.save(a);
    }

    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal Long uid, @PathVariable Long id) {
        repo.delete(repo.findByIdAndUserId(id, uid).orElseThrow(() -> ApiException.notFound("Account")));
    }
}
