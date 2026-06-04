package com.konradciborowski.personalbudget.configs;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI personalBudgetOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Simple Budget Management API")
                        .version("1.0.0")
                        .description("REST API for managing personal budget accounts and transactions."));
    }
}