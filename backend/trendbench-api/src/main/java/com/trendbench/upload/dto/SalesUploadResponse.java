package com.trendbench.upload.dto;

import com.trendbench.upload.entity.SalesUpload;

public record SalesUploadResponse(
	Long uploadId,
	Long storeId,
	String fileName,
	String status,
	String message
) {
	public static SalesUploadResponse from(SalesUpload salesUpload) {
		return new SalesUploadResponse(
			salesUpload.getUploadId(),
			salesUpload.getStoreId(),
			salesUpload.getOriginalFileName(),
			salesUpload.getStatus().name(),
			"업로드가 접수되었습니다."
		);
	}
}
