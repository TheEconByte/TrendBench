package com.trendbench.upload.dto;

import java.util.List;

public record WeekdayAnalysisResponse(
	Long storeId,
	Long uploadId,
	List<WeekdayAnalysisItemResponse> items,
	String strongWeekday,
	String weakWeekday
) {
}
