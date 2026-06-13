package com.trendbench.upload.clickhouse;

import com.trendbench.upload.dto.DailySalesItemResponse;
import com.trendbench.upload.dto.HourlyAnalysisItemResponse;
import com.trendbench.upload.dto.MenuAnalysisItemResponse;
import com.trendbench.upload.dto.WeekdayAnalysisItemResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PosAnalysisRepository {

	private final JdbcTemplate clickHouseJdbcTemplate;

	public PosAnalysisRepository(@Qualifier("clickHouseJdbcTemplate") JdbcTemplate clickHouseJdbcTemplate) {
		this.clickHouseJdbcTemplate = clickHouseJdbcTemplate;
	}

	public List<DailySalesItemResponse> findDailySales(Long storeId, Long uploadId) {
		String sql = """
			SELECT
				sale_date,
				total_revenue,
				total_quantity,
				total_orders,
				avg_order_value
			FROM fact_daily_sales
			WHERE store_id = ? AND upload_id = ?
			ORDER BY sale_date
			""";
		return clickHouseJdbcTemplate.query(sql, (rs, rowNum) -> new DailySalesItemResponse(
			rs.getDate("sale_date").toLocalDate(),
			rs.getLong("total_revenue"),
			rs.getLong("total_quantity"),
			rs.getLong("total_orders"),
			rs.getDouble("avg_order_value")
		), storeId, uploadId);
	}

	public List<MenuAnalysisItemResponse> findMenuAnalysis(Long storeId, Long uploadId) {
		String sql = """
			SELECT
				product_name,
				product_category,
				sum(total_sales) AS total_sales,
				sum(total_quantity) AS total_quantity
			FROM menu_sales_daily
			WHERE store_id = ? AND upload_id = ?
			GROUP BY product_name, product_category
			ORDER BY total_sales DESC, product_name
			""";
		return clickHouseJdbcTemplate.query(sql, (rs, rowNum) -> new MenuAnalysisItemResponse(
			rs.getString("product_name"),
			rs.getString("product_category"),
			rs.getLong("total_sales"),
			rs.getLong("total_quantity"),
			0
		), storeId, uploadId);
	}

	public List<HourlyAnalysisItemResponse> findHourlyAnalysis(Long storeId, Long uploadId) {
		String sql = """
			SELECT
				sales_hour,
				sum(total_sales) AS total_sales,
				sum(total_quantity) AS total_quantity,
				sum(order_count) AS order_count
			FROM hourly_sales
			WHERE store_id = ? AND upload_id = ?
			GROUP BY sales_hour
			ORDER BY sales_hour
			""";
		return clickHouseJdbcTemplate.query(sql, (rs, rowNum) -> new HourlyAnalysisItemResponse(
			rs.getInt("sales_hour"),
			rs.getLong("total_sales"),
			rs.getLong("total_quantity"),
			rs.getLong("order_count")
		), storeId, uploadId);
	}

	public List<WeekdayAnalysisItemResponse> findWeekdayAnalysis(Long storeId, Long uploadId) {
		String sql = """
			SELECT
				weekday,
				weekday_name,
				total_sales,
				total_quantity,
				order_count,
				avg_sales
			FROM weekday_sales
			WHERE store_id = ? AND upload_id = ?
			ORDER BY weekday
			""";
		return clickHouseJdbcTemplate.query(sql, (rs, rowNum) -> new WeekdayAnalysisItemResponse(
			rs.getInt("weekday"),
			rs.getString("weekday_name"),
			rs.getLong("total_sales"),
			rs.getLong("total_quantity"),
			rs.getLong("order_count"),
			rs.getDouble("avg_sales")
		), storeId, uploadId);
	}
}
