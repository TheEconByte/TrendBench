package com.trendbench.upload.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public record DailySalesItemResponse(
	@JsonFormat(pattern = "yyyy-MM-dd")
	LocalDate saleDate,
	long totalRevenue,
	long totalQuantity,
	long totalOrders,
	double avgOrderValue
) {
}
