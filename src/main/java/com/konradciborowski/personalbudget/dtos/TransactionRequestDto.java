package com.konradciborowski.personalbudget.dtos;

import com.konradciborowski.personalbudget.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequestDto(
        @Positive(message = "Amount must be greater than zero")
        @NotNull(message = "Amount is required")
        BigDecimal amount,
        @NotNull(message = "Transaction type is required")
        TransactionType type,
        @NotBlank
        @Size(max = 100, message = "Category must be at most 100 characters")
        String category,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,
        @NotNull(message = "Transaction date is required")
        LocalDate date,

        @NotBlank(message = "Account name is required")
        @Size(max = 100, message = "Account name must be at most 100 characters")
        String accountName
) {

}
