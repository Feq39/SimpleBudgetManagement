package com.konradciborowski.personalbudget.controllers;

import com.konradciborowski.personalbudget.dtos.ListOfTransactionsResponseDto;
import com.konradciborowski.personalbudget.dtos.TransactionRequestDto;
import com.konradciborowski.personalbudget.results.TransactionDeletionStatus;
import com.konradciborowski.personalbudget.services.TransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@RestController
@RequestMapping("v1/transactions/{account_name}")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }


    @GetMapping
    public ListOfTransactionsResponseDto getTransactions(
            @PathVariable(name = "account_name")
            @NotBlank
            @Size(max = 100, message = "Account name must be at most 100 characters")
            String accountName,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            @RequestParam(required = false)
            @Size(max = 100, message = "Category must be at most 100 characters")
            String category
    ) {
        return transactionService.getTransactions(accountName,from,to,category);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public String addTransaction(@Valid @RequestBody TransactionRequestDto transactionRequestDto) {
        TransactionCreationResult res = transactionService.createTransaction(transactionRequestDto);
        switch (res) {
            case Failure failure -> {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            case Success success -> {
                return success.uuid;
            }
        }


    }

    @DeleteMapping("/{uuid}")
    public void deleteTransaction(@PathVariable(name = "uuid") @NotNull @Size(max = 100) String uuid) {
        TransactionDeletionStatus res = transactionService.deleteTransaction(uuid);
        if (res.equals(TransactionDeletionStatus.TRANSACTION_DOES_NOT_EXIST)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
    public sealed interface TransactionCreationResult permits Success, Failure {
    }

    public record Success(String uuid) implements TransactionCreationResult {
    }

    public record Failure( ) implements
            TransactionCreationResult {
    }
}

