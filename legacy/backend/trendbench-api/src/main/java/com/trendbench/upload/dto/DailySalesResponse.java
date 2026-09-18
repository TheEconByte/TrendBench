package com.trendbench.upload.dto;

import java.util.List;

public record DailySalesResponse(
	Long storeId,
	Long uploadId,
	List<DailySalesItemResponse> items
) {
}
