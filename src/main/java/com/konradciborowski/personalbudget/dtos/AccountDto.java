package com.konradciborowski.personalbudget.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record AccountDto(
        @NotBlank(message = "Account name is required")
        String name,
        @NotBlank(message = "Balance is required")
        @PositiveOrZero(message = "Balance must be positive or zero")
        BigDecimal balance) {
}
