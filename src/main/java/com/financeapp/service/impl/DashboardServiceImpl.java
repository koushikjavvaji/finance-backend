package com.financeapp.service.impl;

import com.financeapp.dto.response.DashboardSummaryResponse;
import com.financeapp.dto.response.DashboardSummaryResponse.MonthlyTrend;
import com.financeapp.dto.response.TransactionResponse;
import com.financeapp.enums.TransactionType;
import com.financeapp.repository.TransactionRepository;
import com.financeapp.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final TransactionRepository transactionRepository;

    @Override
    public DashboardSummaryResponse getSummary() {
        BigDecimal totalIncome   = transactionRepository.sumByType(TransactionType.INCOME);
        BigDecimal totalExpenses = transactionRepository.sumByType(TransactionType.EXPENSE);
        BigDecimal netBalance    = totalIncome.subtract(totalExpenses);

        Map<String, BigDecimal> categoryTotals = buildCategoryTotals();
        List<TransactionResponse> recentActivity = buildRecentActivity();
        List<MonthlyTrend> monthlyTrends = buildMonthlyTrends();

        return DashboardSummaryResponse.builder()
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netBalance(netBalance)
                .categoryTotals(categoryTotals)
                .recentActivity(recentActivity)
                .monthlyTrends(monthlyTrends)
                .build();
    }

    // ── Private builders ───────────────────────────────────────────────────────

    private Map<String, BigDecimal> buildCategoryTotals() {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        transactionRepository.sumByCategory()
                .forEach(row -> totals.put((String) row[0], (BigDecimal) row[1]));
        return totals;
    }

    private List<TransactionResponse> buildRecentActivity() {
        return transactionRepository
                .findTop10ByDeletedFalseOrderByCreatedAtDesc()
                .stream()
                .map(t -> TransactionResponse.builder()
                        .id(t.getId())
                        .amount(t.getAmount())
                        .type(t.getType())
                        .category(t.getCategory())
                        .date(t.getDate())
                        .notes(t.getNotes())
                        .createdBy(t.getCreatedBy().getName())
                        .createdAt(t.getCreatedAt())
                        .updatedAt(t.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private List<MonthlyTrend> buildMonthlyTrends() {
        // Go back 12 months from start of this month
        LocalDate since = LocalDate.now().minusMonths(12).withDayOfMonth(1);

        // Use a linked map to preserve chronological order
        Map<String, MonthlyTrend> trendMap = new LinkedHashMap<>();

        transactionRepository.monthlyTrends(since).forEach(row -> {
            int year  = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            TransactionType type = (TransactionType) row[2];
            BigDecimal amount    = (BigDecimal) row[3];

            String key = year + "-" + String.format("%02d", month);

            trendMap.computeIfAbsent(key, k -> MonthlyTrend.builder()
                    .year(year).month(month)
                    .income(BigDecimal.ZERO)
                    .expenses(BigDecimal.ZERO)
                    .net(BigDecimal.ZERO)
                    .build());

            MonthlyTrend trend = trendMap.get(key);
            if (type == TransactionType.INCOME) {
                trend.setIncome(trend.getIncome().add(amount));
            } else {
                trend.setExpenses(trend.getExpenses().add(amount));
            }
            trend.setNet(trend.getIncome().subtract(trend.getExpenses()));
        });

        return new ArrayList<>(trendMap.values());
    }
}
