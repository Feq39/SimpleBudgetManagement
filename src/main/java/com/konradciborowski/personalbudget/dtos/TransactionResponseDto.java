package com.konradciborowski.personalbudget.dtos;

import com.konradciborowski.personalbudget.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponseDto(
        String uuid,
        BigDecimal amount,
        TransactionType type,
        String category,
        String description,
        LocalDate date,
        String accountName) {
}
