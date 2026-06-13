package com.trendbench.market.clickhouse;

import com.trendbench.market.dto.MarketStatisticsSummaryResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MarketStatisticsRepository {

    private final JdbcTemplate clickHouseJdbcTemplate;

    public MarketStatisticsRepository(
            @Qualifier("clickHouseJdbcTemplate") JdbcTemplate clickHouseJdbcTemplate) {
        this.clickHouseJdbcTemplate = clickHouseJdbcTemplate;
    }

    public Optional<MarketStatisticsSummaryResponse> findLatestSummary(String region, String industry) {
        String sql = """
                SELECT
                    region_name,
                    industry_name,
                    business_area_name,
                    substring(toString(stat_month), 1, 7) AS period,
                    avg_sales,
                    avg_payment_count,
                    store_count,
                    open_count,
                    close_count,
                    sales_growth_rate,
                    store_growth_rate,
                    saturation_score,
                    status
                FROM market_statistics
                WHERE region_name = ? AND industry_name = ? AND business_area_name = '전체'
                ORDER BY stat_month DESC
                LIMIT 1
                """;

        return clickHouseJdbcTemplate.query(sql, (rs, rowNum) -> new MarketStatisticsSummaryResponse(
                rs.getString("region_name"),
                rs.getString("industry_name"),
                rs.getString("business_area_name"),
                rs.getString("period"),
                rs.getLong("avg_sales"),
                rs.getLong("avg_payment_count"),
                rs.getInt("store_count"),
                rs.getInt("open_count"),
                rs.getInt("close_count"),
                rs.getDouble("sales_growth_rate"),
                rs.getDouble("store_growth_rate"),
                rs.getDouble("saturation_score"),
                rs.getString("status")
        ), region, industry).stream().findFirst();
    }
}