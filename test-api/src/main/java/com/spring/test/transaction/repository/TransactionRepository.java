package com.spring.test.transaction.repository;

import com.spring.test.transaction.entity.Transaction;
import com.spring.test.transaction.entity.TransactionStatus;
import com.spring.test.transaction.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionCode(String transactionCode);

    boolean existsByTransactionCode(String transactionCode);

    @Query("""
        SELECT t FROM Transaction t
        WHERE (:status IS NULL OR t.status = :status)
          AND (:type IS NULL OR t.type = :type)
          AND (:fromAccount IS NULL OR t.fromAccount = :fromAccount)
          AND (:toAccount IS NULL OR t.toAccount = :toAccount)
          AND (COALESCE(:fromDate, NULL) IS NULL OR t.createdAt >= :fromDate)
          AND (COALESCE(:toDate, NULL) IS NULL OR t.createdAt <= :toDate)
        """)
    Page<Transaction> search(
            @Param("status") TransactionStatus status,
            @Param("type") TransactionType type,
            @Param("fromAccount") String fromAccount,
            @Param("toAccount") String toAccount,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable);
}
