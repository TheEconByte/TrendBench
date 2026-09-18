package com.trendbench.upload.validation;

import com.trendbench.global.exception.ErrorCode;
import com.trendbench.global.exception.UploadException;
import com.trendbench.upload.parser.PosWorkbookReader;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class PosSheetValidator {

	private static final List<String> REQUIRED_SHEETS = List.of(
		"데이터 기준",
		"결제 합계",
		"상품 주문 상세내역"
	);

	public void validateRequiredSheets(MultipartFile file) {
		Set<String> sheetNames = readSheetNames(file);

		for (String requiredSheet : REQUIRED_SHEETS) {
			if (!sheetNames.contains(requiredSheet)) {
				throw new UploadException(
					ErrorCode.MISSING_REQUIRED_SHEET,
					"필수 시트 '" + requiredSheet + "'이 존재하지 않습니다.",
					HttpStatus.BAD_REQUEST,
					Map.of(
						"sheetName", requiredSheet,
						"requiredSheets", REQUIRED_SHEETS
					)
				);
			}
		}
	}

	private Set<String> readSheetNames(MultipartFile file) {
		try (Workbook workbook = PosWorkbookReader.open(file)) {
			Set<String> sheetNames = new LinkedHashSet<>();
			for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
				sheetNames.add(workbook.getSheetName(index));
			}
			return sheetNames;
		} catch (IOException | IllegalArgumentException exception) {
			throw new UploadException(
				ErrorCode.INVALID_XLSX_FILE,
				"업로드된 엑셀 파일을 읽을 수 없습니다.",
				HttpStatus.BAD_REQUEST,
				null
			);
		}
	}
}
