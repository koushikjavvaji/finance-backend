package com.financeapp.controller;

import com.financeapp.dto.request.TransactionRequest;
import com.financeapp.dto.response.ApiResponse;
import com.financeapp.dto.response.TransactionResponse;
import com.financeapp.enums.TransactionType;
import com.financeapp.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Transactions", description = "Financial record management")
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(summary = "List transactions",
               description = "Paginated list with optional filters. VIEWER, ANALYST, ADMIN.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getAll(
            @Parameter(description = "Filter by type") @RequestParam(required = false) TransactionType type,
            @Parameter(description = "Filter by exact category") @RequestParam(required = false) String category,
            @Parameter(description = "Start date (yyyy-MM-dd)") @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date (yyyy-MM-dd)") @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Keyword search across category and notes")
                @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.getAll(type, category, from, to, search, pageable)));
    }

    @Operation(summary = "Get transaction by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.getById(id)));
    }

    @Operation(summary = "Get distinct categories", description = "Useful for populating filter dropdowns.")
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(transactionService.getDistinctCategories()));
    }

    @Operation(summary = "Create transaction", description = "ADMIN only.")
    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> create(
            @Valid @RequestBody TransactionRequest request,
            Authentication authentication) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        transactionService.create(request, authentication.getName()),
                        "Transaction created successfully"));
    }

    @Operation(summary = "Update transaction", description = "ADMIN only.")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success(transactionService.update(id, request), "Transaction updated successfully"));
    }

    @Operation(summary = "Delete transaction",
               description = "Soft delete — record is hidden but preserved for audit. ADMIN only.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Transaction deleted successfully"));
    }
}
