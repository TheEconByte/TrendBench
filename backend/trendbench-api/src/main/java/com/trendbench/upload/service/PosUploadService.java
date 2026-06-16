package com.trendbench.upload.service;

import com.trendbench.global.exception.ErrorCode;
import com.trendbench.global.exception.UploadException;
import com.trendbench.upload.dto.SalesUploadResponse;
import com.trendbench.upload.dto.SalesUploadStatusResponse;
import com.trendbench.upload.entity.SalesUpload;
import com.trendbench.upload.clickhouse.RawOrderItemRepository;
import com.trendbench.upload.clickhouse.SalesAggregationRepository;
import com.trendbench.upload.parser.PosDataBasis;
import com.trendbench.upload.parser.PosDataBasisParser;
import com.trendbench.upload.parser.PosOrderItem;
import com.trendbench.upload.parser.PosOrderItemParser;
import com.trendbench.upload.parser.PosPaymentSummaryParser;
import com.trendbench.upload.repository.SalesUploadRepository;
import com.trendbench.upload.validation.PosSheetValidator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PosUploadService {

	private static final List<String> ALLOWED_EXCEL_EXTENSIONS = List.of(".xlsx", ".xls");

	private final SalesUploadRepository salesUploadRepository;
	private final PosSheetValidator posSheetValidator;
	private final PosDataBasisParser posDataBasisParser;
	private final PosPaymentSummaryParser posPaymentSummaryParser;
	private final PosOrderItemParser posOrderItemParser;
	private final RawOrderItemRepository rawOrderItemRepository;
	private final SalesAggregationRepository salesAggregationRepository;

	public PosUploadService(
		SalesUploadRepository salesUploadRepository,
		PosSheetValidator posSheetValidator,
		PosDataBasisParser posDataBasisParser,
		PosPaymentSummaryParser posPaymentSummaryParser,
		PosOrderItemParser posOrderItemParser,
		RawOrderItemRepository rawOrderItemRepository,
		SalesAggregationRepository salesAggregationRepository
	) {
		this.salesUploadRepository = salesUploadRepository;
		this.posSheetValidator = posSheetValidator;
		this.posDataBasisParser = posDataBasisParser;
		this.posPaymentSummaryParser = posPaymentSummaryParser;
		this.posOrderItemParser = posOrderItemParser;
		this.rawOrderItemRepository = rawOrderItemRepository;
		this.salesAggregationRepository = salesAggregationRepository;
	}

	@Transactional(noRollbackFor = UploadException.class)
	public SalesUploadResponse createUpload(MultipartFile file, Long storeId) {
		validateStoreId(storeId);
		String originalFileName = validateFile(file);

		SalesUpload salesUpload = new SalesUpload(storeId, originalFileName, fileType(originalFileName));
		SalesUpload savedUpload = salesUploadRepository.save(salesUpload);
		savedUpload.markParsing();
		salesUploadRepository.save(savedUpload);

		validateRequiredSheets(file, savedUpload);
		parseDataBasis(file, savedUpload);
		parsePaymentSummary(file, savedUpload);
		parseOrderItems(file, savedUpload);
		savedUpload.markSuccess();
		salesUploadRepository.save(savedUpload);

		return SalesUploadResponse.from(savedUpload);
	}

	@Transactional(readOnly = true)
	public SalesUploadStatusResponse getUploadStatus(Long uploadId) {
		SalesUpload salesUpload = salesUploadRepository.findById(uploadId)
			.orElseThrow(() -> new UploadException(
				ErrorCode.UPLOAD_NOT_FOUND,
				"업로드 이력을 찾을 수 없습니다.",
				HttpStatus.NOT_FOUND,
				Map.of("uploadId", uploadId)
			));
		return SalesUploadStatusResponse.from(salesUpload);
	}

	private void validateRequiredSheets(MultipartFile file, SalesUpload salesUpload) {
		try {
			posSheetValidator.validateRequiredSheets(file);
		} catch (UploadException exception) {
			salesUpload.markFailed(exception.getMessage());
			salesUploadRepository.save(salesUpload);
			throw exception;
		}
	}

	private void parseDataBasis(MultipartFile file, SalesUpload salesUpload) {
		try {
			PosDataBasis dataBasis = posDataBasisParser.parse(file);
			salesUpload.updateReportMetadata(
				dataBasis.reportStartDate(),
				dataBasis.reportEndDate(),
				dataBasis.settlementBasis(),
				dataBasis.aggregationUnit()
			);
		} catch (UploadException exception) {
			salesUpload.markFailed(exception.getMessage());
			salesUploadRepository.save(salesUpload);
			throw exception;
		}
	}

	private void parsePaymentSummary(MultipartFile file, SalesUpload salesUpload) {
		try {
			posPaymentSummaryParser.parse(
				file,
				salesUpload.getReportStartDate(),
				salesUpload.getReportEndDate()
			);
		} catch (UploadException exception) {
			salesUpload.markFailed(exception.getMessage());
			salesUploadRepository.save(salesUpload);
			throw exception;
		}
	}

	private void parseOrderItems(MultipartFile file, SalesUpload salesUpload) {
		try {
			List<PosOrderItem> orderItems = posOrderItemParser.parse(
				file,
				salesUpload.getReportStartDate(),
				salesUpload.getReportEndDate()
			);
			rawOrderItemRepository.batchInsert(salesUpload.getStoreId(), salesUpload.getUploadId(), orderItems);
			salesAggregationRepository.aggregateUpload(salesUpload.getStoreId(), salesUpload.getUploadId());
		} catch (UploadException exception) {
			salesUpload.markFailed(exception.getMessage());
			salesUploadRepository.save(salesUpload);
			throw exception;
		} catch (RuntimeException exception) {
			String message = "POS 주문 상세내역 저장에 실패했습니다.";
			salesUpload.markFailed(message);
			salesUploadRepository.save(salesUpload);
			throw new UploadException(
				ErrorCode.INTERNAL_SERVER_ERROR,
				message,
				HttpStatus.INTERNAL_SERVER_ERROR,
				Map.of("uploadId", salesUpload.getUploadId())
			);
		}
	}

	private void validateStoreId(Long storeId) {
		if (storeId == null) {
			throw invalidUploadRequest("storeId", "storeId는 필수입니다.");
		}
		if (storeId <= 0) {
			throw invalidUploadRequest("storeId", "storeId는 양수여야 합니다.");
		}
	}

	private String validateFile(MultipartFile file) {
		if (file == null) {
			throw invalidUploadRequest("file", "업로드 파일은 필수입니다.");
		}
		if (file.isEmpty()) {
			throw new UploadException(
				ErrorCode.EMPTY_FILE,
				"빈 파일은 업로드할 수 없습니다.",
				HttpStatus.BAD_REQUEST,
				Map.of("field", "file")
			);
		}

		String originalFileName = file.getOriginalFilename();
		if (!StringUtils.hasText(originalFileName)) {
			throw invalidUploadRequest("fileName", "원본 파일명은 필수입니다.");
		}

		String trimmedFileName = originalFileName.trim();
		String lowerCaseFileName = trimmedFileName.toLowerCase(Locale.ROOT);
		if (ALLOWED_EXCEL_EXTENSIONS.stream().noneMatch(lowerCaseFileName::endsWith)) {
			throw new UploadException(
				ErrorCode.INVALID_FILE_TYPE,
				"POS 매출리포트 엑셀 파일만 업로드할 수 있습니다.",
				HttpStatus.BAD_REQUEST,
				Map.of("allowedExtensions", ALLOWED_EXCEL_EXTENSIONS)
			);
		}

		return trimmedFileName;
	}

	private String fileType(String fileName) {
		if (fileName.toLowerCase(Locale.ROOT).endsWith(".xls")) {
			return "xls";
		}
		return "xlsx";
	}

	private UploadException invalidUploadRequest(String field, String message) {
		return new UploadException(
			ErrorCode.INVALID_UPLOAD_REQUEST,
			message,
			HttpStatus.BAD_REQUEST,
			Map.of("field", field)
		);
	}
}
