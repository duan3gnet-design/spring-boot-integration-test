package com.spring.test.transaction.dto;

import com.spring.test.transaction.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransactionRequest(
        @NotBlank(message = "transactionCode is required")
        @Size(max = 64, message = "transactionCode must be at most 64 characters")
        String transactionCode,

        @Size(max = 64, message = "fromAccount must be at most 64 characters")
        String fromAccount,

        @Size(max = 64, message = "toAccount must be at most 64 characters")
        String toAccount,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.0001", message = "amount must be greater than 0")
        BigDecimal amount,

        @Size(min = 3, max = 3, message = "currency must be a 3-letter code")
        String currency,

        @NotNull(message = "type is required")
        TransactionType type,

        @Size(max = 500, message = "description must be at most 500 characters")
        String description
) {}
