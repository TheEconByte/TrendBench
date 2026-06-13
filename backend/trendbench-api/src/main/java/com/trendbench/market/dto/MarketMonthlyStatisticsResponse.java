package com.trendbench.market.dto;

import java.util.List;

public record MarketMonthlyStatisticsResponse(
        String region,
        String industry,
        String businessArea,
        List<MonthlyItem> items
) {
    public record MonthlyItem(
            String period,          // YYYY-MM
            long avgSales,
            long avgPaymentCount,
            int storeCount,
            int openCount,
            int closeCount,
            double salesGrowthRate,
            double saturationScore,
            String status
    ) {}
}