package com.spring.test.transaction.dto;

import com.spring.test.transaction.entity.TransactionStatus;
import jakarta.validation.constraints.NotNull;

public record TransactionStatusRequest(
        @NotNull(message = "status is required")
        TransactionStatus status
) {}
