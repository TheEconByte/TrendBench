package com.trendbench.market.exception;

public class MarketStatisticsNotFoundException extends RuntimeException {
    public MarketStatisticsNotFoundException(String region, String industry) {
        super("해당 지역/업종의 통계를 찾을 수 없습니다: " + region + " / " + industry);
    }
}