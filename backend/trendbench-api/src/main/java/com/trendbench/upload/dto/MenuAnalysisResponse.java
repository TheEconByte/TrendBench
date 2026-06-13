package com.trendbench.upload.dto;

import java.util.List;

public record MenuAnalysisResponse(
	Long storeId,
	Long uploadId,
	List<MenuAnalysisItemResponse> items,
	String topMenu
) {
}
