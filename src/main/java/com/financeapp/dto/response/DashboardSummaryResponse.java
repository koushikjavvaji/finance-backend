package com.financeapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {

    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netBalance;

    /** category → total amount */
    private Map<String, BigDecimal> categoryTotals;

    /** Recent 10 transactions */
    private List<TransactionResponse> recentActivity;

    /** Monthly trends: list of MonthlyTrend objects */
    private List<MonthlyTrend> monthlyTrends;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyTrend {
        private int year;
        private int month;
        private BigDecimal income;
        private BigDecimal expenses;
        private BigDecimal net;
    }
}
