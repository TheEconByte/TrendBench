package com.trendbench.upload.dto;

public record MenuAnalysisItemResponse(
	String productName,
	String productCategory,
	long totalSales,
	long totalQuantity,
	double salesShare
) {
}
