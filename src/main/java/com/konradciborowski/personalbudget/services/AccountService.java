package com.konradciborowski.personalbudget.services;

import com.konradciborowski.personalbudget.database.entities.AccountEntity;
import com.konradciborowski.personalbudget.database.entities.TransactionEntity;
import com.konradciborowski.personalbudget.database.repositories.AccountRepository;
import com.konradciborowski.personalbudget.database.repositories.TransactionRepository;
import com.konradciborowski.personalbudget.dtos.AccountDto;
import com.konradciborowski.personalbudget.dtos.ListOfAccountNamesResponseDto;
import com.konradciborowski.personalbudget.results.AccountCreationStatus;
import com.konradciborowski.personalbudget.results.AccountDeletionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public AccountCreationStatus createAccount(AccountDto accountDto) {
        Optional<AccountEntity> accountOpt = accountRepository.findByName(accountDto.name());
        if (accountOpt.isPresent()) {
            return AccountCreationStatus.ACCOUNT_ALREADY_EXISTS;
        }
        AccountEntity account = new AccountEntity(accountDto.name(), accountDto.balance());
        accountRepository.save(account);
        return AccountCreationStatus.CREATED;
    }

    public ListOfAccountNamesResponseDto getAllAccountNames() {
        List<AccountEntity> accounts = accountRepository.findAll();
        return new ListOfAccountNamesResponseDto(
                accounts.stream()
                        .map(AccountEntity::getName)
                        .toList());
    }

    public Optional<AccountDto> getAccountDetails(String accountName) {
        Optional<AccountEntity> accountEntityOpt = accountRepository.findByName(accountName);
        if (accountEntityOpt.isEmpty()) {
            return Optional.empty();
        }
        AccountEntity accountEntity = accountEntityOpt.get();
        return Optional.of(new AccountDto(accountEntity.getName(), accountEntity.getBalance()));

    }

    @Transactional
    public AccountDeletionStatus deleteAccount(String accountName) {
        Optional<AccountEntity> accountEntityOpt = accountRepository.findWithLockingByName(accountName);
        if (accountEntityOpt.isEmpty()) {
            return AccountDeletionStatus.ACCOUNT_DOES_NOT_EXIST;
        }
        AccountEntity account = accountEntityOpt.get();
        List<TransactionEntity> accountTransactions = transactionRepository.findByAccount(account);
        if (!accountTransactions.isEmpty()) {
            return AccountDeletionStatus.ACCOUNT_HAS_EXISTING_TRANSACTIONS;
        }
        accountRepository.delete(account);
        return AccountDeletionStatus.DELETED;
    }

    public boolean doesAccountExist(String accountName) {
        return accountRepository.findByName(accountName).isPresent();
    }
}
