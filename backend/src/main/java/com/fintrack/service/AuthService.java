package com.fintrack.service;

import com.fintrack.common.ApiException;
import com.fintrack.dto.Dtos.*;
import com.fintrack.model.*;
import com.fintrack.repo.*;
import com.fintrack.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final CategoryRepository categories;
    private final AccountRepository accounts;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    private static final List<String> EXPENSE_CATS =
            List.of("Food", "Transport", "Shopping", "Entertainment", "Bills", "Rent", "Health", "Education", "Others");
    private static final List<String> INCOME_CATS = List.of("Salary", "Freelance", "Other Income");

    @Transactional
    public AuthResponse register(RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email)) throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        User u = new User();
        u.setName(r.name().trim());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(r.password()));
        users.save(u);

        EXPENSE_CATS.forEach(n -> seed(u, n, TxType.EXPENSE));
        INCOME_CATS.forEach(n -> seed(u, n, TxType.INCOME));

        Account cash = new Account();
        cash.setUser(u); cash.setName("Cash"); cash.setType(AccountType.CASH); cash.setBalance(BigDecimal.ZERO);
        accounts.save(cash);
        return new AuthResponse(jwt.generate(u.getId()), u.getName(), u.getEmail());
    }

    public AuthResponse login(LoginRequest r) {
        User u = users.findByEmail(r.email().trim().toLowerCase())
                .filter(x -> encoder.matches(r.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return new AuthResponse(jwt.generate(u.getId()), u.getName(), u.getEmail());
    }

    private void seed(User u, String name, TxType type) {
        Category c = new Category();
        c.setUser(u); c.setName(name); c.setType(type);
        categories.save(c);
    }
}
