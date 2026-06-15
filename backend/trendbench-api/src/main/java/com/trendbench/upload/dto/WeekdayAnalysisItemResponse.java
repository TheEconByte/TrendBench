package com.trendbench.upload.dto;

public record WeekdayAnalysisItemResponse(
	int weekday,
	String weekdayName,
	long totalSales,
	long totalQuantity,
	long orderCount,
	double avgSales
) {
}
