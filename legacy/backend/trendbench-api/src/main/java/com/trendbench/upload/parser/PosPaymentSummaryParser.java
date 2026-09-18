package com.trendbench.upload.parser;

import com.trendbench.global.exception.ErrorCode;
import com.trendbench.global.exception.UploadException;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
public class PosPaymentSummaryParser {

	private static final String PAYMENT_SUMMARY_SHEET_NAME = "결제 합계";
	private static final String SALES_DATE_COLUMN = "기간";
	private static final String GROSS_SALES_COLUMN = "결제금액";
	private static final String VAT_AMOUNT_COLUMN = "부가세";
	private static final String PAYMENT_COUNT_COLUMN = "결제건수";
	private static final String CASH_SALES_COLUMN = "현금";
	private static final String CARD_SALES_COLUMN = "카드";
	private static final String QR_SALES_COLUMN = "QR결제";
	private static final String BANK_TRANSFER_SALES_COLUMN = "계좌이체";
	private static final String PREPAID_SALES_COLUMN = "선불지급수단";
	private static final String OTHER_SALES_COLUMN = "기타";
	private static final List<String> REQUIRED_COLUMNS = List.of(
		SALES_DATE_COLUMN,
		GROSS_SALES_COLUMN,
		VAT_AMOUNT_COLUMN,
		PAYMENT_COUNT_COLUMN,
		CASH_SALES_COLUMN,
		CARD_SALES_COLUMN,
		QR_SALES_COLUMN,
		BANK_TRANSFER_SALES_COLUMN,
		PREPAID_SALES_COLUMN,
		OTHER_SALES_COLUMN
	);
	private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
		DateTimeFormatter.ofPattern("yyyy-MM-dd"),
		DateTimeFormatter.ofPattern("yyyy.MM.dd"),
		DateTimeFormatter.ofPattern("yyyy/MM/dd")
	);

	public List<PosPaymentSummary> parse(
		MultipartFile file,
		LocalDate reportStartDate,
		LocalDate reportEndDate
	) {
		try (Workbook workbook = PosWorkbookReader.open(file)) {
			Sheet sheet = workbook.getSheet(PAYMENT_SUMMARY_SHEET_NAME);
			if (sheet == null) {
				throw paymentSummaryException(
					ErrorCode.MISSING_REQUIRED_SHEET,
					"필수 시트 '" + PAYMENT_SUMMARY_SHEET_NAME + "'이 존재하지 않습니다.",
					Map.of("sheetName", PAYMENT_SUMMARY_SHEET_NAME)
				);
			}

			Header header = findHeader(sheet);
			List<PosPaymentSummary> summaries = new ArrayList<>();
			for (int rowIndex = header.rowIndex() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
				Row row = sheet.getRow(rowIndex);
				if (row == null || isBlankRow(row) || !hasTextAt(row, header.columnIndexes().get(SALES_DATE_COLUMN))) {
					continue;
				}
				summaries.add(parseRow(row, header.columnIndexes(), reportStartDate, reportEndDate));
			}
			return summaries;
		} catch (IOException | IllegalArgumentException exception) {
			throw paymentSummaryException(
				ErrorCode.INVALID_XLSX_FILE,
				"업로드된 엑셀 파일을 읽을 수 없습니다.",
				null
			);
		}
	}

	private Header findHeader(Sheet sheet) {
		for (Row row : sheet) {
			Map<String, Integer> columnIndexes = new HashMap<>();
			for (Cell cell : row) {
				String normalizedValue = normalize(formatCell(cell));
				for (String requiredColumn : REQUIRED_COLUMNS) {
					if (normalize(requiredColumn).equals(normalizedValue)) {
						columnIndexes.put(requiredColumn, cell.getColumnIndex());
					}
				}
			}
			if (columnIndexes.containsKey(SALES_DATE_COLUMN)) {
				collectHeaderColumnsFromNextRow(sheet, row.getRowNum(), columnIndexes);
				validateRequiredColumns(columnIndexes);
				return new Header(row.getRowNum(), columnIndexes);
			}
		}

		throw missingColumnException(SALES_DATE_COLUMN);
	}

	private void collectHeaderColumnsFromNextRow(Sheet sheet, int headerRowIndex, Map<String, Integer> columnIndexes) {
		Row nextRow = sheet.getRow(headerRowIndex + 1);
		if (nextRow == null) {
			return;
		}
		for (Cell cell : nextRow) {
			String normalizedValue = normalize(formatCell(cell));
			for (String requiredColumn : REQUIRED_COLUMNS) {
				if (normalize(requiredColumn).equals(normalizedValue)) {
					columnIndexes.put(requiredColumn, cell.getColumnIndex());
				}
			}
		}
	}

	private void validateRequiredColumns(Map<String, Integer> columnIndexes) {
		for (String requiredColumn : REQUIRED_COLUMNS) {
			if (!columnIndexes.containsKey(requiredColumn)) {
				throw missingColumnException(requiredColumn);
			}
		}
	}

	private PosPaymentSummary parseRow(
		Row row,
		Map<String, Integer> columnIndexes,
		LocalDate reportStartDate,
		LocalDate reportEndDate
	) {
		LocalDate salesDate = parseRequiredDate(row, columnIndexes.get(SALES_DATE_COLUMN), SALES_DATE_COLUMN);
		validateReportPeriod(salesDate, reportStartDate, reportEndDate);

		return new PosPaymentSummary(
			salesDate,
			parseRequiredNonNegativeLong(row, columnIndexes.get(GROSS_SALES_COLUMN), GROSS_SALES_COLUMN),
			parseRequiredNonNegativeLong(row, columnIndexes.get(VAT_AMOUNT_COLUMN), VAT_AMOUNT_COLUMN),
			parseRequiredNonNegativeLong(row, columnIndexes.get(PAYMENT_COUNT_COLUMN), PAYMENT_COUNT_COLUMN),
			parseRequiredNonNegativeLong(row, columnIndexes.get(CASH_SALES_COLUMN), CASH_SALES_COLUMN),
			parseRequiredNonNegativeLong(row, columnIndexes.get(CARD_SALES_COLUMN), CARD_SALES_COLUMN),
			parseRequiredNonNegativeLong(row, columnIndexes.get(QR_SALES_COLUMN), QR_SALES_COLUMN),
			parseRequiredNonNegativeLong(row, columnIndexes.get(BANK_TRANSFER_SALES_COLUMN), BANK_TRANSFER_SALES_COLUMN),
			parseRequiredNonNegativeLong(row, columnIndexes.get(PREPAID_SALES_COLUMN), PREPAID_SALES_COLUMN),
			parseRequiredNonNegativeLong(row, columnIndexes.get(OTHER_SALES_COLUMN), OTHER_SALES_COLUMN)
		);
	}

	private LocalDate parseRequiredDate(Row row, int columnIndex, String columnName) {
		Cell cell = row.getCell(columnIndex);
		if (cell == null || !StringUtils.hasText(formatCell(cell))) {
			throw invalidDateException(columnName);
		}

		try {
			if (cell.getCellType() == CellType.NUMERIC && DateUtil.isValidExcelDate(cell.getNumericCellValue())) {
				return DateUtil.getLocalDateTime(cell.getNumericCellValue()).toLocalDate();
			}
			return parseDateText(formatCell(cell));
		} catch (DateTimeParseException exception) {
			throw invalidDateException(columnName);
		}
	}

	private LocalDate parseDateText(String value) {
		String normalizedValue = value.trim();
		int whitespaceIndex = normalizedValue.indexOf(' ');
		if (whitespaceIndex > 0) {
			normalizedValue = normalizedValue.substring(0, whitespaceIndex);
		}

		for (DateTimeFormatter formatter : DATE_FORMATTERS) {
			try {
				return LocalDate.parse(normalizedValue, formatter);
			} catch (DateTimeParseException ignored) {
			}
		}
		throw new DateTimeParseException("Unsupported date format", value, 0);
	}

	private long parseRequiredNonNegativeLong(Row row, int columnIndex, String columnName) {
		Cell cell = row.getCell(columnIndex);
		String value = cell == null ? "" : formatCell(cell);
		if (!StringUtils.hasText(value)) {
			throw invalidNumericException(columnName, value);
		}

		try {
			BigDecimal number = new BigDecimal(normalizeNumericValue(value));
			if (number.signum() < 0) {
				throw invalidNumericException(columnName, value);
			}
			return number.longValueExact();
		} catch (ArithmeticException | NumberFormatException exception) {
			throw invalidNumericException(columnName, value);
		}
	}

	private String normalizeNumericValue(String value) {
		return value
			.replace(",", "")
			.replace("원", "")
			.trim();
	}

	private void validateReportPeriod(LocalDate salesDate, LocalDate reportStartDate, LocalDate reportEndDate) {
		if (salesDate.isBefore(reportStartDate) || salesDate.isAfter(reportEndDate)) {
			throw paymentSummaryException(
				ErrorCode.OUT_OF_REPORT_PERIOD,
				"결제 합계의 기간 값이 리포트 기간 밖에 있습니다.",
				Map.of(
					"salesDate", salesDate.toString(),
					"reportStartDate", reportStartDate.toString(),
					"reportEndDate", reportEndDate.toString()
				)
			);
		}
	}

	private boolean isBlankRow(Row row) {
		for (Cell cell : row) {
			if (StringUtils.hasText(formatCell(cell))) {
				return false;
			}
		}
		return true;
	}

	private boolean hasTextAt(Row row, int columnIndex) {
		Cell cell = row.getCell(columnIndex);
		return cell != null && StringUtils.hasText(formatCell(cell));
	}

	private UploadException missingColumnException(String columnName) {
		return paymentSummaryException(
			ErrorCode.MISSING_REQUIRED_COLUMN,
			"결제 합계 필수 컬럼 '" + columnName + "'이 존재하지 않습니다.",
			Map.of("sheetName", PAYMENT_SUMMARY_SHEET_NAME, "columnName", columnName)
		);
	}

	private UploadException invalidDateException(String columnName) {
		return paymentSummaryException(
			ErrorCode.INVALID_DATE_FORMAT,
			"결제 합계 컬럼 '" + columnName + "'의 날짜 형식이 올바르지 않습니다.",
			Map.of("sheetName", PAYMENT_SUMMARY_SHEET_NAME, "columnName", columnName)
		);
	}

	private UploadException invalidNumericException(String columnName, String value) {
		return paymentSummaryException(
			ErrorCode.INVALID_NUMERIC_VALUE,
			"결제 합계 컬럼 '" + columnName + "'의 숫자 값이 올바르지 않습니다.",
			Map.of("sheetName", PAYMENT_SUMMARY_SHEET_NAME, "columnName", columnName, "value", value)
		);
	}

	private UploadException paymentSummaryException(
		ErrorCode errorCode,
		String message,
		Map<String, Object> details
	) {
		return new UploadException(errorCode, message, HttpStatus.BAD_REQUEST, details);
	}

	private String formatCell(Cell cell) {
		return new DataFormatter(Locale.KOREA).formatCellValue(cell).trim();
	}

	private String normalize(String value) {
		return value.replaceAll("\\s+", "").trim();
	}

	private record Header(int rowIndex, Map<String, Integer> columnIndexes) {
	}
}
