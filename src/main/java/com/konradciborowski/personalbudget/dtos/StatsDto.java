package com.konradciborowski.personalbudget.dtos;

import java.math.BigDecimal;
import java.util.Map;

public record StatsDto(
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        Map<String,BigDecimal> expensesByCategory
        ) {
}
