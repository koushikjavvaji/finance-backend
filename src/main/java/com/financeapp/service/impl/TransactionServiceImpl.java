package com.financeapp.service.impl;

import com.financeapp.dto.request.TransactionRequest;
import com.financeapp.dto.response.TransactionResponse;
import com.financeapp.entity.Transaction;
import com.financeapp.entity.User;
import com.financeapp.enums.TransactionType;
import com.financeapp.exception.ResourceNotFoundException;
import com.financeapp.repository.TransactionRepository;
import com.financeapp.repository.UserRepository;
import com.financeapp.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository        userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAll(TransactionType type,
                                            String category,
                                            LocalDate from,
                                            LocalDate to,
                                            String keyword,
                                            Pageable pageable) {
        // Treat blank strings as null so JPQL "IS NULL" branch is taken
        String cleanCategory = (category != null && category.isBlank()) ? null : category;
        String cleanKeyword  = (keyword  != null && keyword.isBlank())  ? null : keyword;

        return transactionRepository
                .findWithFilters(type, cleanCategory, from, to, cleanKeyword, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    public TransactionResponse create(TransactionRequest request, String creatorEmail) {
        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + creatorEmail));

        Transaction transaction = Transaction.builder()
                .amount(request.getAmount())
                .type(request.getType())
                .category(request.getCategory())
                .date(request.getDate())
                .notes(request.getNotes())
                .createdBy(creator)
                .build();

        return toResponse(transactionRepository.save(transaction));
    }

    @Override
    public TransactionResponse update(Long id, TransactionRequest request) {
        Transaction transaction = findOrThrow(id);

        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setCategory(request.getCategory());
        transaction.setDate(request.getDate());
        transaction.setNotes(request.getNotes());

        return toResponse(transactionRepository.save(transaction));
    }

    @Override
    public void delete(Long id) {
        Transaction transaction = findOrThrow(id);
        transaction.setDeleted(true);   // soft delete — preserves audit trail
        transactionRepository.save(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getDistinctCategories() {
        return transactionRepository.findDistinctCategories();
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private Transaction findOrThrow(Long id) {
        return transactionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
    }

    private TransactionResponse toResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .amount(t.getAmount())
                .type(t.getType())
                .category(t.getCategory())
                .date(t.getDate())
                .notes(t.getNotes())
                .createdBy(t.getCreatedBy().getName())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
