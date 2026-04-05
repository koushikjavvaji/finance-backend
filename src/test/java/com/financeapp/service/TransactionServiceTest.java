package com.financeapp.service;

import com.financeapp.dto.request.TransactionRequest;
import com.financeapp.dto.response.TransactionResponse;
import com.financeapp.entity.Transaction;
import com.financeapp.entity.User;
import com.financeapp.enums.Role;
import com.financeapp.enums.TransactionType;
import com.financeapp.exception.ResourceNotFoundException;
import com.financeapp.repository.TransactionRepository;
import com.financeapp.repository.UserRepository;
import com.financeapp.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock UserRepository        userRepository;

    @InjectMocks TransactionServiceImpl transactionService;

    private User creator;
    private Transaction sampleTransaction;

    @BeforeEach
    void setUp() {
        creator = User.builder()
                .id(1L).name("Admin").email("admin@finance.com")
                .role(Role.ADMIN).build();

        sampleTransaction = Transaction.builder()
                .id(1L)
                .amount(new BigDecimal("1500.00"))
                .type(TransactionType.INCOME)
                .category("Salary")
                .date(LocalDate.now())
                .notes("Monthly salary")
                .createdBy(creator)
                .deleted(false)
                .build();
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    void getAll_returnsPageOfTransactionResponses() {
        Page<Transaction> page = new PageImpl<>(List.of(sampleTransaction));
        when(transactionRepository.findWithFilters(any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        Page<TransactionResponse> result = transactionService.getAll(
                null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCategory()).isEqualTo("Salary");
        assertThat(result.getContent().get(0).getAmount()).isEqualByComparingTo("1500.00");
    }

    @Test
    void getAll_withBlankSearch_treatsAsNull() {
        Page<Transaction> page = new PageImpl<>(List.of(sampleTransaction));
        when(transactionRepository.findWithFilters(
                isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(page);

        // passing blank string for search — should be converted to null
        transactionService.getAll(null, null, null, null, "   ", PageRequest.of(0, 20));

        verify(transactionRepository).findWithFilters(
                isNull(), isNull(), isNull(), isNull(), isNull(), any());
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    void getById_withValidId_returnsResponse() {
        when(transactionRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(sampleTransaction));

        TransactionResponse result = transactionService.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getType()).isEqualTo(TransactionType.INCOME);
        assertThat(result.getCreatedBy()).isEqualTo("Admin");
    }

    @Test
    void getById_withDeletedOrMissingId_throwsResourceNotFoundException() {
        when(transactionRepository.findByIdAndDeletedFalse(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_withValidRequest_savesAndReturnsResponse() {
        TransactionRequest req = new TransactionRequest();
        req.setAmount(new BigDecimal("500.00"));
        req.setType(TransactionType.EXPENSE);
        req.setCategory("Groceries");
        req.setDate(LocalDate.now());
        req.setNotes("Weekly groceries");

        Transaction saved = Transaction.builder()
                .id(2L).amount(new BigDecimal("500.00"))
                .type(TransactionType.EXPENSE).category("Groceries")
                .date(LocalDate.now()).notes("Weekly groceries")
                .createdBy(creator).deleted(false).build();

        when(userRepository.findByEmail("admin@finance.com")).thenReturn(Optional.of(creator));
        when(transactionRepository.save(any())).thenReturn(saved);

        TransactionResponse result = transactionService.create(req, "admin@finance.com");

        assertThat(result.getCategory()).isEqualTo("Groceries");
        assertThat(result.getType()).isEqualTo(TransactionType.EXPENSE);
        verify(transactionRepository).save(argThat(t ->
                t.getCategory().equals("Groceries") && !t.isDeleted()
        ));
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_withValidId_updatesAllFields() {
        TransactionRequest req = new TransactionRequest();
        req.setAmount(new BigDecimal("2000.00"));
        req.setType(TransactionType.INCOME);
        req.setCategory("Salary");
        req.setDate(LocalDate.now());
        req.setNotes("Updated salary");

        when(transactionRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(sampleTransaction));
        when(transactionRepository.save(sampleTransaction))
                .thenReturn(sampleTransaction);

        TransactionResponse result = transactionService.update(1L, req);

        assertThat(result.getAmount()).isEqualByComparingTo("2000.00");
        assertThat(result.getNotes()).isEqualTo("Updated salary");
        verify(transactionRepository).save(sampleTransaction);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_setsDeletedFlagInsteadOfRemoving() {
        when(transactionRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(sampleTransaction));
        when(transactionRepository.save(sampleTransaction))
                .thenReturn(sampleTransaction);

        transactionService.delete(1L);

        assertThat(sampleTransaction.isDeleted()).isTrue();
        verify(transactionRepository).save(sampleTransaction);
        // Crucially, delete() on the repo should NOT be called
        verify(transactionRepository, never()).delete(any());
        verify(transactionRepository, never()).deleteById(any());
    }

    @Test
    void delete_withMissingId_throwsResourceNotFoundException() {
        when(transactionRepository.findByIdAndDeletedFalse(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getDistinctCategories ─────────────────────────────────────────────────

    @Test
    void getDistinctCategories_returnsListFromRepository() {
        when(transactionRepository.findDistinctCategories())
                .thenReturn(List.of("Dining", "Rent", "Salary"));

        List<String> result = transactionService.getDistinctCategories();

        assertThat(result).containsExactly("Dining", "Rent", "Salary");
    }
}
