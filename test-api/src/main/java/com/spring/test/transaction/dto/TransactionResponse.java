package com.spring.test.transaction.dto;

import com.spring.test.transaction.entity.TransactionStatus;
import com.spring.test.transaction.entity.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        Long id,
        String transactionCode,
        String fromAccount,
        String toAccount,
        BigDecimal amount,
        String currency,
        TransactionType type,
        TransactionStatus status,
        String description,
        Instant createdAt,
        Instant updatedAt
) {}
