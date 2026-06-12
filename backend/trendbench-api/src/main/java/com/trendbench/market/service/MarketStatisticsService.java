package com.trendbench.market.service;

import com.trendbench.market.clickhouse.MarketStatisticsRepository;
import com.trendbench.market.dto.MarketStatisticsSummaryResponse;
import com.trendbench.market.exception.MarketStatisticsNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MarketStatisticsService {

    private final MarketStatisticsRepository repository;

    public MarketStatisticsService(MarketStatisticsRepository repository) {
        this.repository = repository;
    }

    public MarketStatisticsSummaryResponse getSummary(String region, String industry) {
        return repository.findLatestSummary(region, industry)
                .orElseThrow(() -> new MarketStatisticsNotFoundException(region, industry));
    }
}