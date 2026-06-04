package com.konradciborowski.personalbudget;

import com.konradciborowski.personalbudget.database.repositories.AccountRepository;
import com.konradciborowski.personalbudget.database.repositories.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class PersonalBudgetIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("budgetsdb")
            .withUsername("budgets_user")
            .withPassword("Qwerty123+");
    @Autowired
    MockMvc mockMvc;
    @Autowired
    AccountRepository accountRepository;
    @Autowired
    TransactionRepository transactionRepository;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void cleanDatabase() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void shouldCreateAccountAndReturnDetails() throws Exception {
        createAccount("Main", "100.00")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/v1/accounts/Main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Main"))
                .andExpect(jsonPath("$.balance").value(100.00));
    }

    @Test
    void shouldReturnAllAccounts() throws Exception {
        createAccount("Main", "100.00")
                .andExpect(status().isCreated());

        createAccount("Savings", "500.00")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/v1/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts", hasSize(2)))
                .andExpect(jsonPath("$.accounts[0]").value("Main"))
                .andExpect(jsonPath("$.accounts[1]").value("Savings"));
    }

    @Test
    void shouldReturnConflictWhenAccountAlreadyExists() throws Exception {
        createAccount("Main", "100.00")
                .andExpect(status().isCreated());

        createAccount("Main", "200.00")
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturnNotFoundWhenAccountDoesNotExist() throws Exception {
        mockMvc.perform(get("/v1/accounts/DoesNotExist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteAccountWithoutTransactions() throws Exception {
        createAccount("Main", "100.00")
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/v1/accounts/Main"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/accounts/Main"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldNotDeleteAccountWithTransactions() throws Exception {
        createAccount("Main", "100.00")
                .andExpect(status().isCreated());

        addTransaction("Main", "30.00", "EXPENSE", "Food", "Lunch", "2026-06-03")
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/v1/accounts/Main"))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldAddIncomeTransactionAndIncreaseBalance() throws Exception {
        createAccount("Main", "100.00")
                .andExpect(status().isCreated());

        addTransaction("Main", "250.00", "INCOME", "Salary", "June salary", "2026-06-03")
                .andExpect(status().isCreated())
                .andExpect(content().string(notNullValue()));

        mockMvc.perform(get("/v1/accounts/Main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(350.00));
    }

    @Test
    void shouldAddExpenseTransactionAndDecreaseBalance() throws Exception {
        createAccount("Main", "100.00")
                .andExpect(status().isCreated());

        addTransaction("Main", "30.00", "EXPENSE", "Food", "Lunch", "2026-06-03")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/v1/accounts/Main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(70.00));
    }

    @Test
    void shouldDeleteTransactionAndRollbackBalance() throws Exception {
        createAccount("Main", "100.00")
                .andExpect(status().isCreated());

        String uuid = addTransaction("Main", "30.00", "EXPENSE", "Food", "Lunch", "2026-06-03")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        mockMvc.perform(delete("/v1/transactions/Main/{uuid}", uuid))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/accounts/Main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100.00));
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistingTransaction() throws Exception {
        createAccount("Main", "100.00")
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/v1/transactions/Main/non-existing-uuid"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnNotFoundWhenAddingTransactionToNonExistingAccount() throws Exception {
        addTransaction("MissingAccount", "10.00", "EXPENSE", "Food", "Lunch", "2026-06-03")
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnAllTransactionsForAccountWithoutFilters() throws Exception {
        createAccount("Main", "100.00")
                .andExpect(status().isCreated());

        addTransaction("Main", "30.00", "EXPENSE", "Food", "Lunch", "2026-06-03")
                .andExpect(status().isCreated());

        addTransaction("Main", "20.00", "EXPENSE", "Transport", "Bus", "2026-06-04")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/v1/transactions/Main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(2)));
    }

    @Test
    void shouldFilterTransactionsByCategory() throws Exception {
        createAccount("Main", "100.00")
                .andExpect(status().isCreated());

        addTransaction("Main", "30.00", "EXPENSE", "Food", "Lunch", "2026-06-03")
                .andExpect(status().isCreated());

        addTransaction("Main", "20.00", "EXPENSE", "Transport", "Bus", "2026-06-03")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/v1/transactions/Main")
                        .param("category", "Food"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(1)))
                .andExpect(jsonPath("$.transactions[0].category").value("Food"));
    }

    @Test
    void shouldFilterTransactionsByFromDate() throws Exception {
        createAccount("Main", "100.00")
                .andExpect(status().isCreated());

        addTransaction("Main", "10.00", "EXPENSE", "Food", "Old", "2026-06-01")
                .andExpect(status().isCreated());

        addTransaction("Main", "20.00", "EXPENSE", "Food", "New", "2026-06-05")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/v1/transactions/Main")
                        .param("from", "2026-06-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(1)))
                .andExpect(jsonPath("$.transactions[0].description").value("New"));
    }

    @Test
    void shouldFilterTransactionsByToDate() throws Exception {
        createAccount("Main", "100.00")
                .andExpect(status().isCreated());

        addTransaction("Main", "10.00", "EXPENSE", "Food", "Old", "2026-06-01")
                .andExpect(status().isCreated());

        addTransaction("Main", "20.00", "EXPENSE", "Food", "New", "2026-06-05")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/v1/transactions/Main")
                        .param("to", "2026-06-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(1)))
                .andExpect(jsonPath("$.transactions[0].description").value("Old"));
    }

    @Test
    void shouldFilterTransactionsByDateRangeAndCategory() throws Exception {
        createAccount("Main", "100.00")
                .andExpect(status().isCreated());

        addTransaction("Main", "10.00", "EXPENSE", "Food", "Old food", "2026-06-01")
                .andExpect(status().isCreated());

        addTransaction("Main", "20.00", "EXPENSE", "Food", "Matching food", "2026-06-03")
                .andExpect(status().isCreated());

        addTransaction("Main", "30.00", "EXPENSE", "Transport", "Matching date but wrong category", "2026-06-03")
                .andExpect(status().isCreated());

        addTransaction("Main", "40.00", "EXPENSE", "Food", "Too new food", "2026-06-10")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/v1/transactions/Main")
                        .param("from", "2026-06-02")
                        .param("to", "2026-06-05")
                        .param("category", "Food"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(1)))
                .andExpect(jsonPath("$.transactions[0].description").value("Matching food"));
    }

    @Test
    void shouldReturnNotFoundWhenListingTransactionsForNonExistingAccount() throws Exception {
        mockMvc.perform(get("/v1/transactions/MissingAccount"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnStatsForAccount() throws Exception {
        createAccount("Main", "100.00")
                .andExpect(status().isCreated());

        addTransaction("Main", "1000.00", "INCOME", "Salary", "June salary", "2026-06-01")
                .andExpect(status().isCreated());

        addTransaction("Main", "30.00", "EXPENSE", "Food", "Lunch", "2026-06-02")
                .andExpect(status().isCreated());

        addTransaction("Main", "20.00", "EXPENSE", "Transport", "Bus", "2026-06-03")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/v1/stats/Main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(1000.00))
                .andExpect(jsonPath("$.totalExpenses").value(50.00))
                .andExpect(jsonPath("$.expensesByCategory.Food").value(30.00))
                .andExpect(jsonPath("$.expensesByCategory.Transport").value(20.00));
    }

    @Test
    void shouldReturnZeroStatsForAccountWithoutTransactions() throws Exception {
        createAccount("Main", "100.00")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/v1/stats/Main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(0))
                .andExpect(jsonPath("$.totalExpenses").value(0))
                .andExpect(jsonPath("$.expensesByCategory").isMap())
                .andExpect(jsonPath("$.expensesByCategory").isEmpty());
    }

    @Test
    void shouldRejectAccountWithBlankName() throws Exception {
        String body = """
                {
                  "name": "",
                  "balance": 100.00
                }
                """;

        mockMvc.perform(post("/v1/accounts/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectAccountWithMissingName() throws Exception {
        String body = """
                {
                  "balance": 100.00
                }
                """;

        mockMvc.perform(post("/v1/accounts/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectAccountWithNegativeBalance() throws Exception {
        String body = """
                {
                  "name": "Main",
                  "balance": -1.00
                }
                """;

        mockMvc.perform(post("/v1/accounts/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectAccountWithMissingBalance() throws Exception {
        String body = """
                {
                  "name": "Main"
                }
                """;

        mockMvc.perform(post("/v1/accounts/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectTransactionWithZeroAmount() throws Exception {
        createAccount("Main", "100.00")
                .andExpect(status().isCreated());

        addTransaction("Main", "0.00", "EXPENSE", "Food", "Invalid", "2026-06-03")
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectTransactionWithNegativeAmount() throws Exception {
        createAccount("Main", "100.00")
                .andExpect(status().isCreated());

        addTransaction("Main", "-10.00", "EXPENSE", "Food", "Invalid", "2026-06-03")
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectTransactionWithMissingAmount() throws Exception {
        createAccount("Main", "100.00")
                .andExpect(status().isCreated());

        String body = """
                {
                  "type": "EXPENSE",
                  "category": "Food",
                  "description": "Invalid",
                  "date": "2026-06-03",
                  "accountName": "Main"
                }
                """;

        mockMvc.perform(post("/v1/transactions/Main")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectTransactionWithMissingType() throws Exception {
        createAccount("Main", "100.00")
                .andExpect(status().isCreated());

        String body = """
                {
                  "amount": 10.00,
                  "category": "Food",
                  "description": "Invalid",
                  "date": "2026-06-03",
                  "accountName": "Main"
                }
                """;

        mockMvc.perform(post("/v1/transactions/Main")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectTransactionWithInvalidType() throws Exception {
        createAccount("Main", "100.00")
                .andExpect(status().isCreated());

        String body = """
                {
                  "amount": 10.00,
                  "type": "INVALID_TYPE",
                  "category": "Food",
                  "description": "Invalid",
                  "date": "2026-06-03",
                  "accountName": "Main"
                }
                """;

        mockMvc.perform(post("/v1/transactions/Main")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectTransactionWithBlankCategory() throws Exception {
        createAccount("Main", "100.00")
                .andExpect(status().isCreated());

        addTransaction("Main", "10.00", "EXPENSE", "", "Invalid", "2026-06-03")
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectTransactionWithMissingCategory() throws Exception {
        createAccount("Main", "100.00")
                .andExpect(status().isCreated());

        String body = """
                {
                  "amount": 10.00,
                  "type": "EXPENSE",
                  "description": "Invalid",
                  "date": "2026-06-03",
                  "accountName": "Main"
                }
                """;

        mockMvc.perform(post("/v1/transactions/Main")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectTransactionWithMissingDate() throws Exception {
        createAccount("Main", "100.00")
                .andExpect(status().isCreated());

        String body = """
                {
                  "amount": 10.00,
                  "type": "EXPENSE",
                  "category": "Food",
                  "description": "Invalid",
                  "accountName": "Main"
                }
                """;

        mockMvc.perform(post("/v1/transactions/Main")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectTransactionWithInvalidDateFormat() throws Exception {
        createAccount("Main", "100.00")
                .andExpect(status().isCreated());

        String body = """
                {
                  "amount": 10.00,
                  "type": "EXPENSE",
                  "category": "Food",
                  "description": "Invalid",
                  "date": "03-06-2026",
                  "accountName": "Main"
                }
                """;

        mockMvc.perform(post("/v1/transactions/Main")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    private ResultActions createAccount(String name, String balance) throws Exception {
        String body = """
                {
                  "name": "%s",
                  "balance": %s
                }
                """.formatted(name, balance);

        return mockMvc.perform(post("/v1/accounts/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions addTransaction(
            String accountName,
            String amount,
            String type,
            String category,
            String description,
            String date
    ) throws Exception {
        String body = """
                {
                  "amount": %s,
                  "type": "%s",
                  "category": "%s",
                  "description": "%s",
                  "date": "%s",
                  "accountName": "%s"
                }
                """.formatted(amount, type, category, description, date, accountName);

        return mockMvc.perform(post("/v1/transactions/{accountName}", accountName)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void shouldExportTransactionsToCsv() throws Exception {
        createAccount("Main", "100.00")
                .andExpect(status().isCreated());

        addTransaction("Main", "30.00", "EXPENSE", "Food", "Lunch", "2026-06-03")
                .andExpect(status().isCreated());

        addTransaction("Main", "20.00", "EXPENSE", "Transport", "Bus", "2026-06-04")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/v1/transactions/Main/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "uuid,amount,type,category,description,date,accountName"
                )))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "30.00,EXPENSE,Food,Lunch,2026-06-03,Main"
                )))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "20.00,EXPENSE,Transport,Bus,2026-06-04,Main"
                )));
    }

    @Test
    void shouldExportFilteredTransactionsToCsv() throws Exception {
        createAccount("Main", "100.00")
                .andExpect(status().isCreated());

        addTransaction("Main", "10.00", "EXPENSE", "Food", "Old food", "2026-06-01")
                .andExpect(status().isCreated());

        addTransaction("Main", "20.00", "EXPENSE", "Food", "Matching food", "2026-06-03")
                .andExpect(status().isCreated());

        addTransaction("Main", "30.00", "EXPENSE", "Transport", "Wrong category", "2026-06-03")
                .andExpect(status().isCreated());

        addTransaction("Main", "40.00", "EXPENSE", "Food", "Too new food", "2026-06-10")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/v1/transactions/Main/export")
                        .param("from", "2026-06-02")
                        .param("to", "2026-06-05")
                        .param("category", "Food"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "uuid,amount,type,category,description,date,accountName"
                )))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "20.00,EXPENSE,Food,Matching food,2026-06-03,Main"
                )))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Old food")
                )))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Wrong category")
                )))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Too new food")
                )));
    }
}