package com.konradciborowski.personalbudget.controllers;

import com.konradciborowski.personalbudget.dtos.AccountDto;
import com.konradciborowski.personalbudget.dtos.ListOfAccountNamesResponseDto;
import com.konradciborowski.personalbudget.results.AccountCreationStatus;
import com.konradciborowski.personalbudget.results.AccountDeletionStatus;
import com.konradciborowski.personalbudget.services.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController
@RequestMapping(path = "v1/accounts")
public class AccountsController {


    private final AccountService accountService;

    public AccountsController(AccountService accountService) {
        this.accountService = accountService;
    }


    @PostMapping(path = "/create")
    public void createAccount(@Valid @RequestBody AccountDto accountDto) {
        AccountCreationStatus status = accountService.createAccount(accountDto);
        if (status.equals(AccountCreationStatus.ACCOUNT_ALREADY_EXISTS)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account already exists");
        }
        throw new ResponseStatusException(HttpStatus.CREATED);
    }

    @GetMapping
    public ListOfAccountNamesResponseDto getAllAccountNames() {
        return accountService.getAllAccountNames();
    }

    @GetMapping("/{name}")
    public AccountDto getAccountDetails(
            @PathVariable(name = "name")
            @NotBlank(message = "Account name is required")
            @Size(max = 100, message = "Account name must be at most 100 characters")
            String name) {
        Optional<AccountDto> responseOpt = accountService.getAccountDetails(name);
        if (responseOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return responseOpt.get();
    }

    @DeleteMapping("/{name}")
    public void deleteAccount(
            @PathVariable(name = "name")
            @NotBlank(message = "Account name is required")
            @Size(max = 100, message = "Account name must be at most 100 characters")
            String name) {
        AccountDeletionStatus deletionStatus = accountService.deleteAccount(name);
        if (deletionStatus.equals(AccountDeletionStatus.ACCOUNT_DOES_NOT_EXIST)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (deletionStatus.equals(AccountDeletionStatus.ACCOUNT_HAS_EXISTING_TRANSACTIONS)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        throw new ResponseStatusException(HttpStatus.OK);

    }
}
