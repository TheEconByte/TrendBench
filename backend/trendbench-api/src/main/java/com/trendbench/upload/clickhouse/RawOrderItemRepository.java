package com.trendbench.upload.clickhouse;

import com.trendbench.upload.parser.PosOrderItem;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RawOrderItemRepository {

	private final JdbcTemplate clickHouseJdbcTemplate;

	public RawOrderItemRepository(@Qualifier("clickHouseJdbcTemplate") JdbcTemplate clickHouseJdbcTemplate) {
		this.clickHouseJdbcTemplate = clickHouseJdbcTemplate;
	}

	public void batchInsert(Long storeId, Long uploadId, List<PosOrderItem> orderItems) {
		if (orderItems.isEmpty()) {
			return;
		}

		String sql = """
			INSERT INTO raw_order_items (
				store_id,
				upload_id,
				order_date,
				order_time,
				order_no,
				order_channel,
				payment_status,
				product_name,
				product_code,
				product_category,
				option_name,
				quantity,
				product_price,
				option_price,
				product_discount_amount,
				order_discount_amount,
				net_sales,
				vat_amount
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			""";

		clickHouseJdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int index) throws SQLException {
				PosOrderItem orderItem = orderItems.get(index);
				ps.setLong(1, storeId);
				ps.setLong(2, uploadId);
				ps.setObject(3, orderItem.orderDate());
				ps.setString(4, orderItem.orderTime());
				ps.setString(5, orderItem.orderNo());
				ps.setString(6, orderItem.orderChannel());
				ps.setString(7, orderItem.paymentStatus());
				ps.setString(8, orderItem.productName());
				ps.setString(9, orderItem.productCode());
				ps.setString(10, orderItem.productCategory());
				ps.setString(11, orderItem.optionName());
				ps.setInt(12, orderItem.quantity());
				ps.setLong(13, orderItem.productPrice());
				ps.setLong(14, orderItem.optionPrice());
				ps.setLong(15, orderItem.productDiscountAmount());
				ps.setLong(16, orderItem.orderDiscountAmount());
				ps.setLong(17, orderItem.netSales());
				ps.setLong(18, orderItem.vatAmount());
			}

			@Override
			public int getBatchSize() {
				return orderItems.size();
			}
		});
	}
}
