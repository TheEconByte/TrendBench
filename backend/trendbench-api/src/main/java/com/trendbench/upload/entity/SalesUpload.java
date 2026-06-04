package com.trendbench.upload.entity;

import com.trendbench.upload.domain.SalesUploadStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales_uploads")
public class SalesUpload {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "upload_id")
	private Long uploadId;

	@Column(name = "store_id", nullable = false)
	private Long storeId;

	@Column(name = "original_file_name", length = 255)
	private String originalFileName;

	@Column(name = "file_type", length = 20)
	private String fileType = "xlsx";

	@Column(name = "report_start_date")
	private LocalDate reportStartDate;

	@Column(name = "report_end_date")
	private LocalDate reportEndDate;

	@Column(name = "settlement_basis", length = 50)
	private String settlementBasis;

	@Column(name = "aggregation_unit", length = 30)
	private String aggregationUnit;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private SalesUploadStatus status = SalesUploadStatus.PENDING;

	@Column(name = "error_message", columnDefinition = "TEXT")
	private String errorMessage;

	@Column(name = "uploaded_at")
	private LocalDateTime uploadedAt;

	@Column(name = "processed_at")
	private LocalDateTime processedAt;

	protected SalesUpload() {
	}

	public SalesUpload(Long storeId, String originalFileName) {
		this.storeId = storeId;
		this.originalFileName = originalFileName;
	}

	public SalesUpload(Long storeId, String originalFileName, String fileType) {
		this.storeId = storeId;
		this.originalFileName = originalFileName;
		this.fileType = fileType;
	}

	public void markFailed(String errorMessage) {
		this.status = SalesUploadStatus.FAILED;
		this.errorMessage = errorMessage;
		this.processedAt = LocalDateTime.now();
	}

	@PrePersist
	void prePersist() {
		if (fileType == null) {
			fileType = "xlsx";
		}
		if (status == null) {
			status = SalesUploadStatus.PENDING;
		}
		if (uploadedAt == null) {
			uploadedAt = LocalDateTime.now();
		}
	}

	public Long getUploadId() {
		return uploadId;
	}

	public Long getStoreId() {
		return storeId;
	}

	public String getOriginalFileName() {
		return originalFileName;
	}

	public String getFileType() {
		return fileType;
	}

	public LocalDate getReportStartDate() {
		return reportStartDate;
	}

	public LocalDate getReportEndDate() {
		return reportEndDate;
	}

	public String getSettlementBasis() {
		return settlementBasis;
	}

	public String getAggregationUnit() {
		return aggregationUnit;
	}

	public SalesUploadStatus getStatus() {
		return status;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public LocalDateTime getUploadedAt() {
		return uploadedAt;
	}

	public LocalDateTime getProcessedAt() {
		return processedAt;
	}
}
