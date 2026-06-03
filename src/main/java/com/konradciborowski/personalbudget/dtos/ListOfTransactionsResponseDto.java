package com.konradciborowski.personalbudget.dtos;

import java.util.List;

public record ListOfTransactionsResponseDto(List<TransactionResponseDto> transactions) {
}
