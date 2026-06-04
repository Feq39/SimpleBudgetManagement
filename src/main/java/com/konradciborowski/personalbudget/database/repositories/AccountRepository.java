package com.konradciborowski.personalbudget.database.repositories;

import com.konradciborowski.personalbudget.database.entities.AccountEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {
    Optional<AccountEntity> findByName(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AccountEntity> findWithLockingByName(String name);
}
