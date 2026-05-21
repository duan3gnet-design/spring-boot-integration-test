package com.spring.test.transaction.service;

import com.spring.test.transaction.dto.PageResponse;
import com.spring.test.transaction.dto.TransactionRequest;
import com.spring.test.transaction.dto.TransactionResponse;
import com.spring.test.transaction.dto.TransactionStatusRequest;
import com.spring.test.transaction.entity.Transaction;
import com.spring.test.transaction.entity.TransactionStatus;
import com.spring.test.transaction.entity.TransactionType;
import com.spring.test.transaction.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final Set<TransactionStatus> TERMINAL_STATUSES = EnumSet.of(
            TransactionStatus.COMPLETED,
            TransactionStatus.FAILED,
            TransactionStatus.CANCELLED
    );

    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> search(
            TransactionStatus status,
            TransactionType type,
            String fromAccount,
            String toAccount,
            Instant fromDate,
            Instant toDate,
            Pageable pageable) {
        Page<Transaction> page = transactionRepository.search(
                status, type, normalizeAccount(fromAccount), normalizeAccount(toAccount),
                fromDate, toDate, pageable);
        return toPageResponse(page);
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public TransactionResponse create(TransactionRequest req) {
        String code = req.transactionCode().trim();
        if (transactionRepository.existsByTransactionCode(code)) {
            throw new IllegalArgumentException("Transaction code already exists: " + code);
        }
        validateAccountsForType(req.type(), req.fromAccount(), req.toAccount());

        Transaction tx = new Transaction();
        tx.setTransactionCode(code);
        tx.setFromAccount(normalizeAccount(req.fromAccount()));
        tx.setToAccount(normalizeAccount(req.toAccount()));
        tx.setAmount(req.amount());
        tx.setCurrency(req.currency() != null ? req.currency().trim().toUpperCase() : "VND");
        tx.setType(req.type());
        tx.setStatus(TransactionStatus.PENDING);
        tx.setDescription(trimToNull(req.description()));

        Transaction saved = transactionRepository.save(tx);
        log.info("Created transaction id={} code={}", saved.getId(), saved.getTransactionCode());
        return toResponse(saved);
    }

    @Transactional
    public TransactionResponse update(Long id, TransactionRequest req) {
        Transaction tx = getOrThrow(id);
        if (tx.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException("Only PENDING transactions can be updated");
        }

        String code = req.transactionCode().trim();
        transactionRepository.findByTransactionCode(code).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new IllegalArgumentException("Transaction code already exists: " + code);
            }
        });

        validateAccountsForType(req.type(), req.fromAccount(), req.toAccount());

        tx.setTransactionCode(code);
        tx.setFromAccount(normalizeAccount(req.fromAccount()));
        tx.setToAccount(normalizeAccount(req.toAccount()));
        tx.setAmount(req.amount());
        tx.setCurrency(req.currency() != null ? req.currency().trim().toUpperCase() : "VND");
        tx.setType(req.type());
        tx.setDescription(trimToNull(req.description()));

        Transaction saved = transactionRepository.save(tx);
        log.info("Updated transaction id={}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public TransactionResponse updateStatus(Long id, TransactionStatusRequest req) {
        Transaction tx = getOrThrow(id);
        TransactionStatus newStatus = req.status();

        if (tx.getStatus() == newStatus) {
            return toResponse(tx);
        }
        if (TERMINAL_STATUSES.contains(tx.getStatus())) {
            throw new IllegalStateException("Cannot change status from terminal state: " + tx.getStatus());
        }
        if (newStatus == TransactionStatus.PENDING) {
            throw new IllegalArgumentException("Cannot revert to PENDING status");
        }

        tx.setStatus(newStatus);
        Transaction saved = transactionRepository.save(tx);
        log.info("Updated transaction id={} status → {}", saved.getId(), newStatus);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Transaction tx = getOrThrow(id);
        if (tx.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException("Only PENDING transactions can be deleted");
        }
        transactionRepository.delete(tx);
        log.info("Deleted transaction id={}", id);
    }

    private Transaction getOrThrow(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + id));
    }

    private void validateAccountsForType(TransactionType type, String fromAccount, String toAccount) {
        String from = normalizeAccount(fromAccount);
        String to = normalizeAccount(toAccount);

        switch (type) {
            case DEPOSIT -> {
                if (to == null) {
                    throw new IllegalArgumentException("toAccount is required for DEPOSIT");
                }
            }
            case WITHDRAWAL -> {
                if (from == null) {
                    throw new IllegalArgumentException("fromAccount is required for WITHDRAWAL");
                }
            }
            case TRANSFER, PAYMENT -> {
                if (from == null || to == null) {
                    throw new IllegalArgumentException("fromAccount and toAccount are required for " + type);
                }
                if (from.equals(to)) {
                    throw new IllegalArgumentException("fromAccount and toAccount must be different");
                }
            }
        }
    }

    private String normalizeAccount(String account) {
        if (account == null || account.isBlank()) {
            return null;
        }
        return account.trim();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private PageResponse<TransactionResponse> toPageResponse(Page<Transaction> page) {
        return new PageResponse<>(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private TransactionResponse toResponse(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getTransactionCode(),
                tx.getFromAccount(),
                tx.getToAccount(),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getType(),
                tx.getStatus(),
                tx.getDescription(),
                tx.getCreatedAt(),
                tx.getUpdatedAt()
        );
    }
}
