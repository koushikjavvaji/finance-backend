package com.financeapp.service;

import com.financeapp.dto.request.TransactionRequest;
import com.financeapp.dto.response.TransactionResponse;
import com.financeapp.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface TransactionService {

    Page<TransactionResponse> getAll(TransactionType type,
                                     String category,
                                     LocalDate from,
                                     LocalDate to,
                                     String keyword,
                                     Pageable pageable);

    TransactionResponse getById(Long id);

    TransactionResponse create(TransactionRequest request, String creatorEmail);

    TransactionResponse update(Long id, TransactionRequest request);

    void delete(Long id);

    List<String> getDistinctCategories();
}
