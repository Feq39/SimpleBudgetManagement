package com.konradciborowski.personalbudget.controllers;

import com.konradciborowski.personalbudget.dtos.StatsDto;
import com.konradciborowski.personalbudget.services.StatsService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("v1/stats/{account_name}")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    public StatsDto getStatsForAccount(
            @PathVariable(name = "account_name")
            @NotBlank
            @Size(max = 100, message = "Account name must be at most 100 characters")
            String accountName
    ) {
        Result res = statsService.getAccountStats(accountName);
        switch (res) {
            case Success success -> {
                return success.statsDto;
            }
            case Failure failure -> {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account does not exist");
            }
        }
    }

    public sealed interface Result permits Success, Failure {
    }

    public record Success(StatsDto statsDto) implements Result {
    }

    public record Failure() implements Result {
    }
}
