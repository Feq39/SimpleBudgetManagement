package com.konradciborowski.personalbudget.services;

import com.konradciborowski.personalbudget.controllers.TransactionController;
import com.konradciborowski.personalbudget.database.entities.AccountEntity;
import com.konradciborowski.personalbudget.database.entities.TransactionEntity;
import com.konradciborowski.personalbudget.database.repositories.AccountRepository;
import com.konradciborowski.personalbudget.database.repositories.TransactionRepository;
import com.konradciborowski.personalbudget.database.specifications.TransactionSpecification;
import com.konradciborowski.personalbudget.dtos.ListOfTransactionsResponseDto;
import com.konradciborowski.personalbudget.dtos.TransactionRequestDto;
import com.konradciborowski.personalbudget.dtos.TransactionResponseDto;
import com.konradciborowski.personalbudget.enums.TransactionType;
import com.konradciborowski.personalbudget.results.TransactionDeletionStatus;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.apache.commons.text.StringEscapeUtils.escapeCsv;

@Service
public class TransactionService {


    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionService(TransactionRepository transactionRepository, AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    public ListOfTransactionsResponseDto getTransactions(
            @Nullable String accountName,
            @Nullable LocalDate from,
            @Nullable LocalDate to,
            @Nullable String category) {
        Specification<TransactionEntity> specification = Specification
                .where(TransactionSpecification.hasAccountName(accountName))
                .and(TransactionSpecification.dateFrom(from))
                .and(TransactionSpecification.dateTo(to))
                .and(TransactionSpecification.hasCategory(category));
        return new ListOfTransactionsResponseDto(
                transactionRepository
                        .findAll(specification, Sort.by(Sort.Direction.DESC, "date"))
                        .stream()
                        .map(e -> new TransactionResponseDto(
                                e.getUuid(),
                                e.getAmount(),
                                e.getType(),
                                e.getCategory(),
                                e.getDescription(),
                                e.getDate(),
                                e.getAccount().getName()
                        ))
                        .toList());

    }

    @Transactional
    public TransactionController.TransactionCreationResult createTransaction(TransactionRequestDto transactionRequestDto) {
        Optional<AccountEntity> accountEntityOpt = accountRepository.findWithLockingByName(transactionRequestDto.accountName());
        if (accountEntityOpt.isEmpty()) {
            return new TransactionController.Failure();
        }
        AccountEntity account = accountEntityOpt.get();
        String uuid = UUID.randomUUID().toString();
        TransactionEntity transaction = new TransactionEntity(
                uuid,
                transactionRequestDto.amount(),
                transactionRequestDto.type(),
                transactionRequestDto.category(),
                transactionRequestDto.description(),
                transactionRequestDto.date(),
                account
        );
        BigDecimal newBalance = account.getBalance();
        if (transaction.getType().equals(TransactionType.INCOME)) {
            newBalance = newBalance.add(transaction.getAmount());
        } else {
            newBalance = newBalance.subtract(transaction.getAmount());
        }
        account.setBalance(newBalance);
        accountRepository.save(account);
        transactionRepository.save(transaction);
        return new TransactionController.Success(uuid);
    }

    @Transactional
    public TransactionDeletionStatus deleteTransaction(String uuid) {
        Optional<TransactionEntity> transactionOpt = transactionRepository.findWithLockingByUuid(uuid);
        if (transactionOpt.isEmpty()) {
            return TransactionDeletionStatus.TRANSACTION_DOES_NOT_EXIST;
        }
        TransactionEntity transaction = transactionOpt.get();
        AccountEntity account = accountRepository.findWithLockingByName(transaction.getAccount().getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Transaction exists but related account does not exist"
                ));

        if (transaction.getType() == TransactionType.INCOME) {
            account.setBalance(account.getBalance().subtract(transaction.getAmount()));
        } else {
            account.setBalance(account.getBalance().add(transaction.getAmount()));
        }
        accountRepository.save(account);
        transactionRepository.delete(transaction);
        return TransactionDeletionStatus.DELETED;
    }

    public String exportTransactionsToCsv(
            String accountName,
            LocalDate from,
            LocalDate to,
            String category
    ) {
        ListOfTransactionsResponseDto response = getTransactions(accountName, from, to, category);

        StringBuilder csv = new StringBuilder();

        csv.append("uuid,amount,type,category,description,date,accountName\n");

        for (TransactionResponseDto transaction : response.transactions()) {
            csv.append(escapeCsv(transaction.uuid())).append(",");
            csv.append(transaction.amount()).append(",");
            csv.append(transaction.type()).append(",");
            csv.append(escapeCsv(transaction.category())).append(",");
            csv.append(escapeCsv(transaction.description())).append(",");
            csv.append(transaction.date()).append(",");
            csv.append(escapeCsv(transaction.accountName())).append("\n");
        }

        return csv.toString();
    }
}
