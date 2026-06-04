package com.trendbench.upload.parser;

import com.trendbench.global.exception.ErrorCode;
import com.trendbench.global.exception.UploadException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
public class PosDataBasisParser {

	private static final String DATA_BASIS_SHEET_NAME = "데이터 기준";
	private static final String START_DATE_LABEL = "시작일자";
	private static final String END_DATE_LABEL = "종료일자";
	private static final String SETTLEMENT_BASIS_LABEL = "매출 정산 기준";
	private static final String AGGREGATION_UNIT_LABEL = "집계 단위";
	private static final String DAILY_AGGREGATION_UNIT = "일간";
	private static final List<String> DATA_BASIS_LABELS = List.of(
		START_DATE_LABEL,
		END_DATE_LABEL,
		SETTLEMENT_BASIS_LABEL,
		AGGREGATION_UNIT_LABEL
	);
	private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
		DateTimeFormatter.ofPattern("yyyy-MM-dd"),
		DateTimeFormatter.ofPattern("yyyy.MM.dd"),
		DateTimeFormatter.ofPattern("yyyy/MM/dd")
	);

	public PosDataBasis parse(MultipartFile file) {
		try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
			Sheet sheet = workbook.getSheet(DATA_BASIS_SHEET_NAME);
			if (sheet == null) {
				throw dataBasisException(
					ErrorCode.INVALID_DATA_BASIS_SHEET,
					"'데이터 기준' 시트를 읽을 수 없습니다.",
					Map.of("sheetName", DATA_BASIS_SHEET_NAME)
				);
			}

			LocalDate reportStartDate = parseRequiredDate(sheet, START_DATE_LABEL, "reportStartDate");
			LocalDate reportEndDate = parseRequiredDate(sheet, END_DATE_LABEL, "reportEndDate");
			String settlementBasis = parseRequiredText(sheet, SETTLEMENT_BASIS_LABEL, "settlementBasis");
			String aggregationUnit = parseRequiredText(sheet, AGGREGATION_UNIT_LABEL, "aggregationUnit");

			validateReportPeriod(reportStartDate, reportEndDate);
			validateAggregationUnit(aggregationUnit);

			return new PosDataBasis(reportStartDate, reportEndDate, settlementBasis, aggregationUnit);
		} catch (IOException | IllegalArgumentException exception) {
			throw dataBasisException(
				ErrorCode.INVALID_DATA_BASIS_SHEET,
				"'데이터 기준' 시트를 읽을 수 없습니다.",
				Map.of("sheetName", DATA_BASIS_SHEET_NAME)
			);
		}
	}

	private LocalDate parseRequiredDate(Sheet sheet, String label, String fieldName) {
		Cell cell = findValueCell(sheet, label)
			.orElseThrow(() -> missingValueException(label, fieldName));

		try {
			if (cell.getCellType() == CellType.NUMERIC && DateUtil.isValidExcelDate(cell.getNumericCellValue())) {
				if (DateUtil.isCellDateFormatted(cell)) {
					return cell.getLocalDateTimeCellValue().toLocalDate();
				}
				return DateUtil.getLocalDateTime(cell.getNumericCellValue()).toLocalDate();
			}

			String value = formatCell(cell);
			if (!StringUtils.hasText(value)) {
				throw missingValueException(label, fieldName);
			}
			return parseDateText(value);
		} catch (DateTimeParseException exception) {
			throw dataBasisException(
				ErrorCode.INVALID_DATA_BASIS_SHEET,
				"데이터 기준 값 '" + label + "'의 날짜 형식이 올바르지 않습니다.",
				Map.of("field", fieldName, "label", label)
			);
		}
	}

	private String parseRequiredText(Sheet sheet, String label, String fieldName) {
		Cell cell = findValueCell(sheet, label)
			.orElseThrow(() -> missingValueException(label, fieldName));
		String value = formatCell(cell);
		if (!StringUtils.hasText(value)) {
			throw missingValueException(label, fieldName);
		}
		return value;
	}

	private Optional<Cell> findValueCell(Sheet sheet, String label) {
		String normalizedLabel = normalizeLabel(label);
		for (Row row : sheet) {
			for (Cell cell : row) {
				if (normalizedLabel.equals(normalizeLabel(formatCell(cell)))) {
					Optional<Cell> rightCell = firstNonBlankCellToRight(row, cell.getColumnIndex());
					if (rightCell.isPresent() && !isDataBasisLabel(rightCell.get())) {
						return rightCell;
					}
					return firstNonBlankCellBelow(sheet, row.getRowNum(), cell.getColumnIndex()).or(() -> rightCell);
				}
			}
		}
		return Optional.empty();
	}

	private Optional<Cell> firstNonBlankCellToRight(Row row, int labelColumnIndex) {
		for (int index = labelColumnIndex + 1; index < row.getLastCellNum(); index++) {
			Cell cell = row.getCell(index);
			if (cell != null && StringUtils.hasText(formatCell(cell))) {
				return Optional.of(cell);
			}
		}
		return Optional.empty();
	}

	private Optional<Cell> firstNonBlankCellBelow(Sheet sheet, int labelRowIndex, int labelColumnIndex) {
		for (int index = labelRowIndex + 1; index <= sheet.getLastRowNum(); index++) {
			Row row = sheet.getRow(index);
			if (row == null) {
				continue;
			}
			Cell cell = row.getCell(labelColumnIndex);
			if (cell != null && StringUtils.hasText(formatCell(cell))) {
				return Optional.of(cell);
			}
		}
		return Optional.empty();
	}

	private boolean isDataBasisLabel(Cell cell) {
		String normalizedCellValue = normalizeLabel(formatCell(cell));
		return DATA_BASIS_LABELS.stream()
			.map(this::normalizeLabel)
			.anyMatch(normalizedCellValue::equals);
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

	private void validateReportPeriod(LocalDate reportStartDate, LocalDate reportEndDate) {
		if (reportStartDate.isAfter(reportEndDate)) {
			throw dataBasisException(
				ErrorCode.INVALID_REPORT_PERIOD,
				"리포트 시작일은 종료일보다 늦을 수 없습니다.",
				Map.of(
					"reportStartDate", reportStartDate.toString(),
					"reportEndDate", reportEndDate.toString()
				)
			);
		}
	}

	private void validateAggregationUnit(String aggregationUnit) {
		if (!DAILY_AGGREGATION_UNIT.equals(aggregationUnit)) {
			throw dataBasisException(
				ErrorCode.INVALID_AGGREGATION_UNIT,
				"집계 단위는 MVP 기준 '" + DAILY_AGGREGATION_UNIT + "'이어야 합니다.",
				Map.of(
					"aggregationUnit", aggregationUnit,
					"allowedAggregationUnit", DAILY_AGGREGATION_UNIT
				)
			);
		}
	}

	private UploadException missingValueException(String label, String fieldName) {
		return dataBasisException(
			ErrorCode.MISSING_DATA_BASIS_VALUE,
			"데이터 기준 값 '" + label + "'이 존재하지 않습니다.",
			Map.of("field", fieldName, "label", label)
		);
	}

	private UploadException dataBasisException(ErrorCode errorCode, String message, Map<String, Object> details) {
		return new UploadException(errorCode, message, HttpStatus.BAD_REQUEST, details);
	}

	private String formatCell(Cell cell) {
		return new DataFormatter(Locale.KOREA).formatCellValue(cell).trim();
	}

	private String normalizeLabel(String value) {
		return value.replaceAll("\\s+", "").replace(":", "").trim();
	}
}
