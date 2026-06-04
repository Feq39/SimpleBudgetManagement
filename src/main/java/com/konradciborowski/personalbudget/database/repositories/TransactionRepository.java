package com.konradciborowski.personalbudget.database.repositories;

import com.konradciborowski.personalbudget.database.entities.AccountEntity;
import com.konradciborowski.personalbudget.database.entities.TransactionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long>, JpaSpecificationExecutor<TransactionEntity> {
    List<TransactionEntity> findByAccount(AccountEntity account);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TransactionEntity> findWithLockingByUuid(String uuid);

}
