package com.trendbench.market.controller;

import com.trendbench.market.dto.MarketStatisticsSummaryResponse;
import com.trendbench.market.service.MarketStatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/markets")
public class MarketStatisticsController {

    private final MarketStatisticsService service;

    public MarketStatisticsController(MarketStatisticsService service) {
        this.service = service;
    }

    @GetMapping("/statistics")
    public MarketStatisticsSummaryResponse getStatistics(
            @RequestParam(defaultValue = "강남구") String region,
            @RequestParam(defaultValue = "한식") String industry) {
        return service.getSummary(region, industry);
    }
}