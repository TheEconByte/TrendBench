package com.trendbench.upload.parser;

import com.trendbench.global.exception.ErrorCode;
import com.trendbench.global.exception.UploadException;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
public class PosOrderItemParser {

	private static final String ORDER_ITEM_SHEET_NAME = "상품 주문 상세내역";
	private static final String ORDER_DATE_COLUMN = "주문기준일자";
	private static final String PAYMENT_STATUS_COLUMN = "결제상태";
	private static final String ORDER_TIME_COLUMN = "주문시작시각";
	private static final String ORDER_CHANNEL_COLUMN = "주문채널";
	private static final String ORDER_NO_COLUMN = "주문번호";
	private static final String PRODUCT_NAME_COLUMN = "상품명";
	private static final String PRODUCT_CODE_COLUMN = "상품코드";
	private static final String PRODUCT_CATEGORY_COLUMN = "카테고리";
	private static final String OPTION_NAME_COLUMN = "옵션";
	private static final String QUANTITY_COLUMN = "수량";
	private static final String PRODUCT_PRICE_COLUMN = "상품가격";
	private static final String OPTION_PRICE_COLUMN = "옵션가격";
	private static final String PRODUCT_DISCOUNT_AMOUNT_COLUMN = "상품할인 금액";
	private static final String ORDER_DISCOUNT_AMOUNT_COLUMN = "주문할인 금액";
	private static final String NET_SALES_COLUMN = "실판매금액";
	private static final String VAT_AMOUNT_COLUMN = "부가세";
	private static final String VAT_AMOUNT_ALIAS_COLUMN = "부가세액";
	private static final String COMPLETED_PAYMENT_STATUS = "완료";
	private static final List<String> REQUIRED_COLUMNS = List.of(
		ORDER_DATE_COLUMN,
		PAYMENT_STATUS_COLUMN,
		ORDER_TIME_COLUMN,
		ORDER_CHANNEL_COLUMN,
		ORDER_NO_COLUMN,
		PRODUCT_NAME_COLUMN,
		PRODUCT_CODE_COLUMN,
		PRODUCT_CATEGORY_COLUMN,
		OPTION_NAME_COLUMN,
		QUANTITY_COLUMN,
		PRODUCT_PRICE_COLUMN,
		OPTION_PRICE_COLUMN,
		PRODUCT_DISCOUNT_AMOUNT_COLUMN,
		ORDER_DISCOUNT_AMOUNT_COLUMN,
		NET_SALES_COLUMN,
		VAT_AMOUNT_COLUMN
	);
	private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
		DateTimeFormatter.ofPattern("yyyy-MM-dd"),
		DateTimeFormatter.ofPattern("yyyy.MM.dd"),
		DateTimeFormatter.ofPattern("yyyy/MM/dd")
	);
	private static final List<DateTimeFormatter> TIME_FORMATTERS = List.of(
		DateTimeFormatter.ofPattern("HH:mm:ss"),
		DateTimeFormatter.ofPattern("HH:mm")
	);

	public List<PosOrderItem> parse(
		MultipartFile file,
		LocalDate reportStartDate,
		LocalDate reportEndDate
	) {
		try (Workbook workbook = PosWorkbookReader.open(file)) {
			Sheet sheet = workbook.getSheet(ORDER_ITEM_SHEET_NAME);
			if (sheet == null) {
				throw orderItemException(
					ErrorCode.MISSING_REQUIRED_SHEET,
					"필수 시트 '" + ORDER_ITEM_SHEET_NAME + "'이 존재하지 않습니다.",
					Map.of("sheetName", ORDER_ITEM_SHEET_NAME)
				);
			}

			Header header = findHeader(sheet);
			Set<OrderItemKey> canceledOrderItemKeys = findCanceledOrderItemKeys(sheet, header);
			List<PosOrderItem> orderItems = new ArrayList<>();
			for (int rowIndex = header.rowIndex() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
				Row row = sheet.getRow(rowIndex);
				if (row == null || isBlankRow(row) || !hasTextAt(row, header.columnIndexes().get(ORDER_DATE_COLUMN))) {
					continue;
				}
				if (!COMPLETED_PAYMENT_STATUS.equals(parseText(row, header.columnIndexes().get(PAYMENT_STATUS_COLUMN)))) {
					continue;
				}
				if (canceledOrderItemKeys.contains(orderItemKey(row, header.columnIndexes()))) {
					continue;
				}
				PosOrderItem orderItem = parseRow(row, header.columnIndexes(), reportStartDate, reportEndDate);
				orderItems.add(orderItem);
			}
			return orderItems;
		} catch (IOException | IllegalArgumentException exception) {
			throw orderItemException(
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
					if (matchesColumn(requiredColumn, normalizedValue)) {
						columnIndexes.put(requiredColumn, cell.getColumnIndex());
					}
				}
			}
			if (columnIndexes.containsKey(ORDER_DATE_COLUMN)) {
				validateRequiredColumns(columnIndexes);
				return new Header(row.getRowNum(), columnIndexes);
			}
		}

		throw missingColumnException(ORDER_DATE_COLUMN);
	}

	private Set<OrderItemKey> findCanceledOrderItemKeys(Sheet sheet, Header header) {
		Set<OrderItemKey> canceledOrderItemKeys = new HashSet<>();
		for (int rowIndex = header.rowIndex() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
			Row row = sheet.getRow(rowIndex);
			if (row == null || isBlankRow(row) || !hasTextAt(row, header.columnIndexes().get(ORDER_DATE_COLUMN))) {
				continue;
			}
			if ("취소".equals(parseText(row, header.columnIndexes().get(PAYMENT_STATUS_COLUMN)))) {
				canceledOrderItemKeys.add(orderItemKey(row, header.columnIndexes()));
			}
		}
		return canceledOrderItemKeys;
	}

	private OrderItemKey orderItemKey(Row row, Map<String, Integer> columnIndexes) {
		return new OrderItemKey(
			parseText(row, columnIndexes.get(ORDER_NO_COLUMN)),
			parseText(row, columnIndexes.get(PRODUCT_NAME_COLUMN)),
			parseText(row, columnIndexes.get(OPTION_NAME_COLUMN))
		);
	}

	private boolean matchesColumn(String requiredColumn, String normalizedValue) {
		if (normalize(requiredColumn).equals(normalizedValue)) {
			return true;
		}
		if (NET_SALES_COLUMN.equals(requiredColumn) && normalizedValue.startsWith(normalize(NET_SALES_COLUMN))) {
			return true;
		}
		return VAT_AMOUNT_COLUMN.equals(requiredColumn) && normalize(VAT_AMOUNT_ALIAS_COLUMN).equals(normalizedValue);
	}

	private void validateRequiredColumns(Map<String, Integer> columnIndexes) {
		for (String requiredColumn : REQUIRED_COLUMNS) {
			if (!columnIndexes.containsKey(requiredColumn)) {
				throw missingColumnException(requiredColumn);
			}
		}
	}

	private PosOrderItem parseRow(
		Row row,
		Map<String, Integer> columnIndexes,
		LocalDate reportStartDate,
		LocalDate reportEndDate
	) {
		LocalDate orderDate = parseRequiredDate(row, columnIndexes.get(ORDER_DATE_COLUMN), ORDER_DATE_COLUMN);
		validateReportPeriod(orderDate, reportStartDate, reportEndDate);

		return new PosOrderItem(
			orderDate,
			parseRequiredTime(row, columnIndexes.get(ORDER_TIME_COLUMN), ORDER_TIME_COLUMN),
			parseRequiredText(row, columnIndexes.get(ORDER_NO_COLUMN), ORDER_NO_COLUMN),
			parseRequiredText(row, columnIndexes.get(ORDER_CHANNEL_COLUMN), ORDER_CHANNEL_COLUMN),
			parseRequiredText(row, columnIndexes.get(PAYMENT_STATUS_COLUMN), PAYMENT_STATUS_COLUMN),
			parseRequiredText(row, columnIndexes.get(PRODUCT_NAME_COLUMN), PRODUCT_NAME_COLUMN),
			parseText(row, columnIndexes.get(PRODUCT_CODE_COLUMN)),
			parseText(row, columnIndexes.get(PRODUCT_CATEGORY_COLUMN)),
			parseText(row, columnIndexes.get(OPTION_NAME_COLUMN)),
			parseRequiredPositiveInt(row, columnIndexes.get(QUANTITY_COLUMN), QUANTITY_COLUMN),
			parseRequiredNonNegativeLong(row, columnIndexes.get(PRODUCT_PRICE_COLUMN), PRODUCT_PRICE_COLUMN),
			parseRequiredNonNegativeLong(row, columnIndexes.get(OPTION_PRICE_COLUMN), OPTION_PRICE_COLUMN),
			parseRequiredLong(row, columnIndexes.get(PRODUCT_DISCOUNT_AMOUNT_COLUMN), PRODUCT_DISCOUNT_AMOUNT_COLUMN),
			parseRequiredLong(row, columnIndexes.get(ORDER_DISCOUNT_AMOUNT_COLUMN), ORDER_DISCOUNT_AMOUNT_COLUMN),
			parseRequiredNonNegativeLong(row, columnIndexes.get(NET_SALES_COLUMN), NET_SALES_COLUMN),
			parseRequiredNonNegativeLong(row, columnIndexes.get(VAT_AMOUNT_COLUMN), VAT_AMOUNT_COLUMN)
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

	private String parseRequiredTime(Row row, int columnIndex, String columnName) {
		Cell cell = row.getCell(columnIndex);
		if (cell == null || !StringUtils.hasText(formatCell(cell))) {
			throw invalidDateException(columnName);
		}

		try {
			if (cell.getCellType() == CellType.NUMERIC && DateUtil.isValidExcelDate(cell.getNumericCellValue())) {
				return DateUtil.getLocalDateTime(cell.getNumericCellValue()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
			}
			LocalTime time = parseTimeText(formatCell(cell));
			return time.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
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

	private LocalTime parseTimeText(String value) {
		String normalizedValue = value.trim();
		int whitespaceIndex = normalizedValue.indexOf(' ');
		if (whitespaceIndex > 0) {
			normalizedValue = normalizedValue.substring(whitespaceIndex + 1);
		}
		for (DateTimeFormatter formatter : TIME_FORMATTERS) {
			try {
				return LocalTime.parse(normalizedValue, formatter);
			} catch (DateTimeParseException ignored) {
			}
		}
		throw new DateTimeParseException("Unsupported time format", value, 0);
	}

	private String parseRequiredText(Row row, int columnIndex, String columnName) {
		String value = parseText(row, columnIndex);
		if (!StringUtils.hasText(value)) {
			throw orderItemException(
				ErrorCode.INVALID_UPLOAD_REQUEST,
				"상품 주문 상세내역 컬럼 '" + columnName + "' 값은 필수입니다.",
				Map.of("sheetName", ORDER_ITEM_SHEET_NAME, "columnName", columnName)
			);
		}
		return value;
	}

	private String parseText(Row row, int columnIndex) {
		Cell cell = row.getCell(columnIndex);
		if (cell == null) {
			return "";
		}
		return formatCell(cell);
	}

	private int parseRequiredPositiveInt(Row row, int columnIndex, String columnName) {
		long value = parseRequiredLong(row, columnIndex, columnName);
		if (value <= 0 || value > Integer.MAX_VALUE) {
			throw invalidNumericException(columnName, String.valueOf(value));
		}
		return (int) value;
	}

	private long parseRequiredNonNegativeLong(Row row, int columnIndex, String columnName) {
		long value = parseRequiredLong(row, columnIndex, columnName);
		if (value < 0) {
			throw invalidNumericException(columnName, String.valueOf(value));
		}
		return value;
	}

	private long parseRequiredLong(Row row, int columnIndex, String columnName) {
		Cell cell = row.getCell(columnIndex);
		String value = cell == null ? "" : formatCell(cell);
		if (!StringUtils.hasText(value)) {
			throw invalidNumericException(columnName, value);
		}

		try {
			return new BigDecimal(normalizeNumericValue(value)).longValueExact();
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

	private void validateReportPeriod(LocalDate orderDate, LocalDate reportStartDate, LocalDate reportEndDate) {
		if (orderDate.isBefore(reportStartDate) || orderDate.isAfter(reportEndDate)) {
			throw orderItemException(
				ErrorCode.OUT_OF_REPORT_PERIOD,
				"상품 주문 상세내역의 주문기준일자가 리포트 기간 밖에 있습니다.",
				Map.of(
					"orderDate", orderDate.toString(),
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
		return orderItemException(
			ErrorCode.MISSING_REQUIRED_COLUMN,
			"상품 주문 상세내역 필수 컬럼 '" + columnName + "'이 존재하지 않습니다.",
			Map.of("sheetName", ORDER_ITEM_SHEET_NAME, "columnName", columnName)
		);
	}

	private UploadException invalidDateException(String columnName) {
		return orderItemException(
			ErrorCode.INVALID_DATE_FORMAT,
			"상품 주문 상세내역 컬럼 '" + columnName + "'의 날짜/시간 형식이 올바르지 않습니다.",
			Map.of("sheetName", ORDER_ITEM_SHEET_NAME, "columnName", columnName)
		);
	}

	private UploadException invalidNumericException(String columnName, String value) {
		return orderItemException(
			ErrorCode.INVALID_NUMERIC_VALUE,
			"상품 주문 상세내역 컬럼 '" + columnName + "'의 숫자 값이 올바르지 않습니다.",
			Map.of("sheetName", ORDER_ITEM_SHEET_NAME, "columnName", columnName, "value", value)
		);
	}

	private UploadException orderItemException(
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

	private record OrderItemKey(String orderNo, String productName, String optionName) {
	}
}
