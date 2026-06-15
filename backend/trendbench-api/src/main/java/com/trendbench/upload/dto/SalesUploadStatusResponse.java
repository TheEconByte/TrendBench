package com.trendbench.upload.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.trendbench.upload.entity.SalesUpload;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SalesUploadStatusResponse(
	Long uploadId,
	Long storeId,
	String status,
	@JsonFormat(pattern = "yyyy-MM-dd")
	LocalDate reportStartDate,
	@JsonFormat(pattern = "yyyy-MM-dd")
	LocalDate reportEndDate,
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	LocalDateTime processedAt,
	String errorMessage
) {
	public static SalesUploadStatusResponse from(SalesUpload salesUpload) {
		return new SalesUploadStatusResponse(
			salesUpload.getUploadId(),
			salesUpload.getStoreId(),
			salesUpload.getStatus().name(),
			salesUpload.getReportStartDate(),
			salesUpload.getReportEndDate(),
			salesUpload.getProcessedAt(),
			salesUpload.getErrorMessage()
		);
	}
}
