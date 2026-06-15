package com.trendbench.upload.dto;

public record HourlyAnalysisItemResponse(
	int salesHour,
	long totalSales,
	long totalQuantity,
	long orderCount
) {
}
