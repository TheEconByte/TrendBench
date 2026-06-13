package com.trendbench.market.dto;

public record MarketStatisticsSummaryResponse(
        String region,
        String industry,
        String businessArea,
        String period,          // YYYY-MM
        long avgSales,
        long avgPaymentCount,
        int storeCount,
        int openCount,
        int closeCount,
        double salesGrowthRate,
        double storeGrowthRate,
        double saturationScore,
        String status
) {}