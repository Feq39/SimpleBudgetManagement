package com.konradciborowski.personalbudget.services;

import com.konradciborowski.personalbudget.controllers.StatsController;
import com.konradciborowski.personalbudget.database.entities.AccountEntity;
import com.konradciborowski.personalbudget.database.entities.TransactionEntity;
import com.konradciborowski.personalbudget.database.repositories.AccountRepository;
import com.konradciborowski.personalbudget.database.repositories.TransactionRepository;
import com.konradciborowski.personalbudget.dtos.StatsDto;
import com.konradciborowski.personalbudget.enums.TransactionType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StatsService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public StatsService(AccountRepository accountRepository,TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }


    public StatsController.Result getAccountStats(String accountName) {
        Optional<AccountEntity> accountEntityOpt = accountRepository.findByName(accountName);
        if (accountEntityOpt.isEmpty()) {
            return new StatsController.Failure();
        }
        AccountEntity account = accountEntityOpt.get();
        List<TransactionEntity> transactions = transactionRepository.findByAccount(account);
        BigDecimal totalIncome = transactions.stream()
                .filter(e -> e.getType().equals(TransactionType.INCOME))
                .map(TransactionEntity::getAmount)
                .reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal totalExpenses = transactions.stream()
                .filter(e -> e.getType().equals(TransactionType.EXPENSE))
                .map(TransactionEntity::getAmount)
                .reduce(BigDecimal.ZERO,BigDecimal::add);
        Map<String, BigDecimal> expensesByCategory = transactions.stream()
                .filter(transaction -> transaction.getType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        TransactionEntity::getCategory,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                TransactionEntity::getAmount,
                                BigDecimal::add
                        )
                ));
        return new StatsController.Success(new StatsDto(totalIncome,totalExpenses,expensesByCategory));
    }
}
