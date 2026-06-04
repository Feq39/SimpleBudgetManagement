package com.konradciborowski.personalbudget.database.specifications;

import com.konradciborowski.personalbudget.database.entities.TransactionEntity;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class TransactionSpecification {

    private TransactionSpecification() {
    }

    public static Specification<TransactionEntity> hasAccountName(String accountName) {
        return (root, query, criteriaBuilder) -> {
            if (accountName == null) {
                return null;
            }

            return criteriaBuilder.equal(
                    root.get("account").get("name"),
                    accountName
            );
        };
    }

    public static Specification<TransactionEntity> hasCategory(String category) {
        return (root, query, criteriaBuilder) -> {
            if (category == null) {
                return null;
            }

            return criteriaBuilder.equal(
                    root.get("category"),
                    category
            );
        };
    }

    public static Specification<TransactionEntity> dateFrom(LocalDate from) {
        return (root, query, criteriaBuilder) -> {
            if (from == null) {
                return null;
            }

            return criteriaBuilder.greaterThanOrEqualTo(
                    root.get("date"),
                    from
            );
        };
    }

    public static Specification<TransactionEntity> dateTo(LocalDate to) {
        return (root, query, criteriaBuilder) -> {
            if (to == null) {
                return null;
            }

            return criteriaBuilder.lessThanOrEqualTo(
                    root.get("date"),
                    to
            );
        };
    }
}