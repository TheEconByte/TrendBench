package com.trendbench.upload.dto;

import java.util.List;

public record HourlyAnalysisResponse(
	Long storeId,
	Long uploadId,
	List<HourlyAnalysisItemResponse> items,
	Integer peakHour
) {
}
