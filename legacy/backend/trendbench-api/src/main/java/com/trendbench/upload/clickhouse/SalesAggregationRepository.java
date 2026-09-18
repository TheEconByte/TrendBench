package com.trendbench.upload.clickhouse;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SalesAggregationRepository {

	private final JdbcTemplate clickHouseJdbcTemplate;

	public SalesAggregationRepository(@Qualifier("clickHouseJdbcTemplate") JdbcTemplate clickHouseJdbcTemplate) {
		this.clickHouseJdbcTemplate = clickHouseJdbcTemplate;
	}

	public void aggregateUpload(Long storeId, Long uploadId) {
		insertDailySales(storeId, uploadId);
		insertMenuSalesDaily(storeId, uploadId);
		insertHourlySales(storeId, uploadId);
		insertWeekdaySales(storeId, uploadId);
	}

	private void insertDailySales(Long storeId, Long uploadId) {
		String sql = """
			INSERT INTO fact_daily_sales (
				store_id,
				upload_id,
				sale_date,
				total_revenue,
				total_quantity,
				total_orders,
				avg_order_value
			)
			SELECT
				store_id,
				upload_id,
				order_date AS sale_date,
				sum(net_sales) AS total_revenue,
				sum(quantity) AS total_quantity,
				countDistinct(order_no) AS total_orders,
				if(total_orders = 0, 0, total_revenue / total_orders) AS avg_order_value
			FROM raw_order_items
			WHERE store_id = ? AND upload_id = ?
			GROUP BY store_id, upload_id, order_date
			""";
		clickHouseJdbcTemplate.update(sql, storeId, uploadId);
	}

	private void insertMenuSalesDaily(Long storeId, Long uploadId) {
		String sql = """
			INSERT INTO menu_sales_daily (
				store_id,
				upload_id,
				sale_date,
				product_name,
				product_category,
				total_quantity,
				total_sales,
				sales_share
			)
			WITH daily_totals AS (
				SELECT
					order_date,
					sum(net_sales) AS daily_total_sales
				FROM raw_order_items
				WHERE store_id = ? AND upload_id = ?
				GROUP BY order_date
			)
			SELECT
				items.store_id,
				items.upload_id,
				items.order_date AS sale_date,
				items.product_name,
				items.product_category,
				sum(items.quantity) AS total_quantity,
				sum(items.net_sales) AS total_sales,
				if(any(daily_totals.daily_total_sales) = 0, 0, total_sales / any(daily_totals.daily_total_sales)) AS sales_share
			FROM raw_order_items AS items
			INNER JOIN daily_totals ON items.order_date = daily_totals.order_date
			WHERE items.store_id = ? AND items.upload_id = ?
			GROUP BY
				items.store_id,
				items.upload_id,
				items.order_date,
				items.product_name,
				items.product_category
			""";
		clickHouseJdbcTemplate.update(sql, storeId, uploadId, storeId, uploadId);
	}

	private void insertHourlySales(Long storeId, Long uploadId) {
		String sql = """
			INSERT INTO hourly_sales (
				store_id,
				upload_id,
				sale_date,
				sales_hour,
				total_sales,
				total_quantity,
				order_count
			)
			SELECT
				store_id,
				upload_id,
				order_date AS sale_date,
				toHour(parseDateTimeBestEffort(order_time)) AS sales_hour,
				sum(net_sales) AS total_sales,
				sum(quantity) AS total_quantity,
				countDistinct(order_no) AS order_count
			FROM raw_order_items
			WHERE store_id = ? AND upload_id = ?
			GROUP BY store_id, upload_id, order_date, sales_hour
			""";
		clickHouseJdbcTemplate.update(sql, storeId, uploadId);
	}

	private void insertWeekdaySales(Long storeId, Long uploadId) {
		String sql = """
			INSERT INTO weekday_sales (
				store_id,
				upload_id,
				weekday,
				weekday_name,
				total_sales,
				total_quantity,
				order_count,
				avg_sales
			)
			WITH daily_sales AS (
				SELECT
					store_id,
					upload_id,
					order_date,
					toDayOfWeek(order_date) AS weekday,
					sum(net_sales) AS daily_total_sales,
					sum(quantity) AS daily_total_quantity,
					countDistinct(order_no) AS daily_order_count
				FROM raw_order_items
				WHERE store_id = ? AND upload_id = ?
				GROUP BY store_id, upload_id, order_date
			)
			SELECT
				store_id,
				upload_id,
				weekday,
				multiIf(
					weekday = 1, '월요일',
					weekday = 2, '화요일',
					weekday = 3, '수요일',
					weekday = 4, '목요일',
					weekday = 5, '금요일',
					weekday = 6, '토요일',
					weekday = 7, '일요일',
					'알 수 없음'
				) AS weekday_name,
				sum(daily_total_sales) AS total_sales,
				sum(daily_total_quantity) AS total_quantity,
				sum(daily_order_count) AS order_count,
				avg(daily_total_sales) AS avg_sales
			FROM daily_sales
			GROUP BY store_id, upload_id, weekday
			""";
		clickHouseJdbcTemplate.update(sql, storeId, uploadId);
	}
}
