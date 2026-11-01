package com.fintrack.dto;

import com.fintrack.model.AccountType;
import com.fintrack.model.TxType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class Dtos {
    private Dtos() {}

    public record RegisterRequest(@NotBlank String name, @NotBlank @Email String email,
                                  @NotBlank @Size(min = 6, message = "must be at least 6 characters") String password) {}
    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
    public record AuthResponse(String token, String name, String email) {}

    public record AccountRequest(@NotBlank String name, @NotNull AccountType type, BigDecimal balance) {}
    public record CategoryRequest(@NotBlank String name, @NotNull TxType type, String icon) {}

    public record TransactionRequest(@NotNull Long accountId, @NotNull Long categoryId,
                                     @NotNull @DecimalMin("0.01") BigDecimal amount,
                                     @NotNull TxType type, @NotNull LocalDate date,
                                     String merchant, String note) {}

    public record BudgetRequest(@NotNull Long categoryId, @NotNull @DecimalMin("0.01") BigDecimal amount,
                                @NotNull @Min(1) @Max(12) Integer month, @NotNull Integer year) {}

    public record GoalRequest(@NotBlank String name, @NotNull @DecimalMin("1") BigDecimal targetAmount,
                              LocalDate deadline) {}
    public record AmountRequest(@NotNull @DecimalMin("0.01") BigDecimal amount) {}

    public record QuickAddRequest(@NotBlank String text) {}
    public record Draft(BigDecimal amount, TxType type, LocalDate date, Long accountId,
                        Long categoryId, String merchant, String note) {}
}
