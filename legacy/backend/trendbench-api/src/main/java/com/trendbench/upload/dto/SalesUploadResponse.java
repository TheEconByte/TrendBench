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
			messageFor(salesUpload)
		);
	}

	private static String messageFor(SalesUpload salesUpload) {
		return switch (salesUpload.getStatus()) {
			case SUCCESS -> "업로드 처리가 완료되었습니다.";
			case FAILED -> "업로드 처리에 실패했습니다.";
			default -> "업로드가 접수되었습니다.";
		};
	}
}
