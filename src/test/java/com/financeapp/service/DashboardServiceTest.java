package com.financeapp.service;

import com.financeapp.dto.response.DashboardSummaryResponse;
import com.financeapp.entity.Transaction;
import com.financeapp.entity.User;
import com.financeapp.enums.Role;
import com.financeapp.enums.TransactionType;
import com.financeapp.repository.TransactionRepository;
import com.financeapp.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock TransactionRepository transactionRepository;

    @InjectMocks DashboardServiceImpl dashboardService;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = User.builder()
                .id(1L).name("Admin").email("admin@finance.com").role(Role.ADMIN).build();
    }

    // ── getSummary ────────────────────────────────────────────────────────────

    @Test
    void getSummary_calculatesNetBalanceCorrectly() {
        when(transactionRepository.sumByType(TransactionType.INCOME))
                .thenReturn(new BigDecimal("10000.00"));
        when(transactionRepository.sumByType(TransactionType.EXPENSE))
                .thenReturn(new BigDecimal("3500.00"));
        when(transactionRepository.sumByCategory()).thenReturn(List.of());
        when(transactionRepository.findTop10ByDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of());
        when(transactionRepository.monthlyTrends(any())).thenReturn(List.of());

        DashboardSummaryResponse result = dashboardService.getSummary();

        assertThat(result.getTotalIncome()).isEqualByComparingTo("10000.00");
        assertThat(result.getTotalExpenses()).isEqualByComparingTo("3500.00");
        assertThat(result.getNetBalance()).isEqualByComparingTo("6500.00");
    }

    @Test
    void getSummary_withZeroTransactions_returnsZeroBalance() {
        when(transactionRepository.sumByType(TransactionType.INCOME))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumByType(TransactionType.EXPENSE))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumByCategory()).thenReturn(List.of());
        when(transactionRepository.findTop10ByDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of());
        when(transactionRepository.monthlyTrends(any())).thenReturn(List.of());

        DashboardSummaryResponse result = dashboardService.getSummary();

        assertThat(result.getNetBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getRecentActivity()).isEmpty();
        assertThat(result.getMonthlyTrends()).isEmpty();
    }

    @Test
    void getSummary_buildsCategoryTotalsMap() {
        when(transactionRepository.sumByType(any())).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.findTop10ByDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of());
        when(transactionRepository.monthlyTrends(any())).thenReturn(List.of());
        when(transactionRepository.sumByCategory()).thenReturn(List.of(
                new Object[]{"Salary",    new BigDecimal("5000.00")},
                new Object[]{"Groceries", new BigDecimal("300.00")},
                new Object[]{"Rent",      new BigDecimal("1200.00")}
        ));

        DashboardSummaryResponse result = dashboardService.getSummary();

        assertThat(result.getCategoryTotals()).hasSize(3);
        assertThat(result.getCategoryTotals().get("Salary")).isEqualByComparingTo("5000.00");
        assertThat(result.getCategoryTotals().get("Groceries")).isEqualByComparingTo("300.00");
        assertThat(result.getCategoryTotals().get("Rent")).isEqualByComparingTo("1200.00");
    }

    @Test
    void getSummary_buildsMonthlyTrendsWithNetCalculation() {
        when(transactionRepository.sumByType(any())).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumByCategory()).thenReturn(List.of());
        when(transactionRepository.findTop10ByDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of());
        when(transactionRepository.monthlyTrends(any())).thenReturn(List.of(
                new Object[]{2026, 3, TransactionType.INCOME,  new BigDecimal("5000.00")},
                new Object[]{2026, 3, TransactionType.EXPENSE, new BigDecimal("2000.00")},
                new Object[]{2026, 4, TransactionType.INCOME,  new BigDecimal("6000.00")}
        ));

        DashboardSummaryResponse result = dashboardService.getSummary();

        assertThat(result.getMonthlyTrends()).hasSize(2);

        DashboardSummaryResponse.MonthlyTrend march = result.getMonthlyTrends().get(0);
        assertThat(march.getYear()).isEqualTo(2026);
        assertThat(march.getMonth()).isEqualTo(3);
        assertThat(march.getIncome()).isEqualByComparingTo("5000.00");
        assertThat(march.getExpenses()).isEqualByComparingTo("2000.00");
        assertThat(march.getNet()).isEqualByComparingTo("3000.00");

        DashboardSummaryResponse.MonthlyTrend april = result.getMonthlyTrends().get(1);
        assertThat(april.getMonth()).isEqualTo(4);
        assertThat(april.getIncome()).isEqualByComparingTo("6000.00");
        assertThat(april.getExpenses()).isEqualByComparingTo("0.00");
        assertThat(april.getNet()).isEqualByComparingTo("6000.00");
    }

    @Test
    void getSummary_recentActivityContainsLast10Transactions() {
        List<Transaction> recent = List.of(
                makeTransaction(1L, "Salary",    TransactionType.INCOME,  new BigDecimal("5000")),
                makeTransaction(2L, "Groceries", TransactionType.EXPENSE, new BigDecimal("300"))
        );

        when(transactionRepository.sumByType(any())).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumByCategory()).thenReturn(List.of());
        when(transactionRepository.monthlyTrends(any())).thenReturn(List.of());
        when(transactionRepository.findTop10ByDeletedFalseOrderByCreatedAtDesc()).thenReturn(recent);

        DashboardSummaryResponse result = dashboardService.getSummary();

        assertThat(result.getRecentActivity()).hasSize(2);
        assertThat(result.getRecentActivity().get(0).getCategory()).isEqualTo("Salary");
        assertThat(result.getRecentActivity().get(1).getCategory()).isEqualTo("Groceries");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Transaction makeTransaction(Long id, String category,
                                        TransactionType type, BigDecimal amount) {
        return Transaction.builder()
                .id(id).category(category).type(type).amount(amount)
                .date(LocalDate.now()).createdBy(admin).deleted(false).build();
    }
}
