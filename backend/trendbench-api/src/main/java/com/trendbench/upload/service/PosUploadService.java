package com.trendbench.upload.service;

import com.trendbench.global.exception.ErrorCode;
import com.trendbench.global.exception.UploadException;
import com.trendbench.upload.dto.SalesUploadResponse;
import com.trendbench.upload.entity.SalesUpload;
import com.trendbench.upload.parser.PosDataBasis;
import com.trendbench.upload.parser.PosDataBasisParser;
import com.trendbench.upload.repository.SalesUploadRepository;
import com.trendbench.upload.validation.PosSheetValidator;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PosUploadService {

	private static final String XLSX_EXTENSION = ".xlsx";
	private static final String XLSX_FILE_TYPE = "xlsx";

	private final SalesUploadRepository salesUploadRepository;
	private final PosSheetValidator posSheetValidator;
	private final PosDataBasisParser posDataBasisParser;

	public PosUploadService(
		SalesUploadRepository salesUploadRepository,
		PosSheetValidator posSheetValidator,
		PosDataBasisParser posDataBasisParser
	) {
		this.salesUploadRepository = salesUploadRepository;
		this.posSheetValidator = posSheetValidator;
		this.posDataBasisParser = posDataBasisParser;
	}

	@Transactional(noRollbackFor = UploadException.class)
	public SalesUploadResponse createUpload(MultipartFile file, Long storeId) {
		validateStoreId(storeId);
		String originalFileName = validateFile(file);

		SalesUpload salesUpload = new SalesUpload(storeId, originalFileName, XLSX_FILE_TYPE);
		SalesUpload savedUpload = salesUploadRepository.save(salesUpload);

		validateRequiredSheets(file, savedUpload);
		parseDataBasis(file, savedUpload);

		return SalesUploadResponse.from(savedUpload);
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
			salesUploadRepository.save(salesUpload);
		} catch (UploadException exception) {
			salesUpload.markFailed(exception.getMessage());
			salesUploadRepository.save(salesUpload);
			throw exception;
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
		if (!trimmedFileName.toLowerCase(Locale.ROOT).endsWith(XLSX_EXTENSION)) {
			throw new UploadException(
				ErrorCode.INVALID_FILE_TYPE,
				"POS 매출리포트 XLSX 파일만 업로드할 수 있습니다.",
				HttpStatus.BAD_REQUEST,
				Map.of("allowedExtension", XLSX_EXTENSION)
			);
		}

		return trimmedFileName;
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
