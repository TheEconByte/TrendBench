package com.trendbench.upload.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.trendbench.global.exception.ErrorCode;
import com.trendbench.global.exception.UploadException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class PosOrderItemParserTest {

	private final PosOrderItemParser parser = new PosOrderItemParser();

	@Test
	void parseReadsCompletedOrderItemRows() {
		MockMultipartFile file = createWorkbookFile(sheet -> {
			writeHeader(sheet);
			writeOrderItemRow(sheet, 1, "2026-05-01", "완료", "12:30", "2", "18000");
		});

		List<PosOrderItem> orderItems = parser.parse(
			file,
			LocalDate.of(2026, 5, 1),
			LocalDate.of(2026, 5, 31)
		);

		assertThat(orderItems).hasSize(1);
		PosOrderItem orderItem = orderItems.get(0);
		assertThat(orderItem.orderDate()).isEqualTo(LocalDate.of(2026, 5, 1));
		assertThat(orderItem.orderTime()).isEqualTo("12:30:00");
		assertThat(orderItem.orderNo()).isEqualTo("ORD-1");
		assertThat(orderItem.orderChannel()).isEqualTo("포스");
		assertThat(orderItem.paymentStatus()).isEqualTo("완료");
		assertThat(orderItem.productName()).isEqualTo("김치찌개");
		assertThat(orderItem.productCode()).isEqualTo("P001");
		assertThat(orderItem.productCategory()).isEqualTo("식사");
		assertThat(orderItem.quantity()).isEqualTo(2);
		assertThat(orderItem.productPrice()).isEqualTo(9000);
		assertThat(orderItem.netSales()).isEqualTo(18000);
		assertThat(orderItem.vatAmount()).isEqualTo(1636);
	}

	@Test
	void parseExcludesNonCompletedPaymentRows() {
		MockMultipartFile file = createWorkbookFile(sheet -> {
			writeHeader(sheet);
			writeOrderItemRow(sheet, 1, "2026-05-01", "취소", "12:30", "2", "18000");
		});

		List<PosOrderItem> orderItems = parser.parse(
			file,
			LocalDate.of(2026, 5, 1),
			LocalDate.of(2026, 5, 31)
		);

		assertThat(orderItems).isEmpty();
	}

	@Test
	void parseFailsWhenRequiredColumnIsMissing() {
		MockMultipartFile file = createWorkbookFile(sheet -> {
			Row headerRow = sheet.createRow(0);
			headerRow.createCell(0).setCellValue("주문기준일자");
			headerRow.createCell(1).setCellValue("결제상태");
		});

		assertThatThrownBy(() -> parser.parse(
			file,
			LocalDate.of(2026, 5, 1),
			LocalDate.of(2026, 5, 31)
		))
			.isInstanceOfSatisfying(UploadException.class, exception -> {
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MISSING_REQUIRED_COLUMN);
				assertThat(exception.getMessage()).isEqualTo("상품 주문 상세내역 필수 컬럼 '주문시작시각'이 존재하지 않습니다.");
				assertThat(exception.getDetails()).containsEntry("columnName", "주문시작시각");
			});
	}

	@Test
	void parseFailsWhenOrderDateFormatIsInvalid() {
		MockMultipartFile file = createWorkbookFile(sheet -> {
			writeHeader(sheet);
			writeOrderItemRow(sheet, 1, "2026년 5월 1일", "완료", "12:30", "2", "18000");
		});

		assertThatThrownBy(() -> parser.parse(
			file,
			LocalDate.of(2026, 5, 1),
			LocalDate.of(2026, 5, 31)
		))
			.isInstanceOfSatisfying(UploadException.class, exception -> {
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_DATE_FORMAT);
				assertThat(exception.getDetails()).containsEntry("columnName", "주문기준일자");
			});
	}

	@Test
	void parseFailsWhenQuantityIsInvalid() {
		MockMultipartFile file = createWorkbookFile(sheet -> {
			writeHeader(sheet);
			writeOrderItemRow(sheet, 1, "2026-05-01", "완료", "12:30", "0", "18000");
		});

		assertThatThrownBy(() -> parser.parse(
			file,
			LocalDate.of(2026, 5, 1),
			LocalDate.of(2026, 5, 31)
		))
			.isInstanceOfSatisfying(UploadException.class, exception -> {
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_NUMERIC_VALUE);
				assertThat(exception.getDetails()).containsEntry("columnName", "수량");
			});
	}

	@Test
	void parseFailsWhenOrderDateIsOutOfReportPeriod() {
		MockMultipartFile file = createWorkbookFile(sheet -> {
			writeHeader(sheet);
			writeOrderItemRow(sheet, 1, "2026-06-01", "완료", "12:30", "2", "18000");
		});

		assertThatThrownBy(() -> parser.parse(
			file,
			LocalDate.of(2026, 5, 1),
			LocalDate.of(2026, 5, 31)
		))
			.isInstanceOfSatisfying(UploadException.class, exception -> {
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OUT_OF_REPORT_PERIOD);
				assertThat(exception.getDetails()).containsEntry("orderDate", "2026-06-01");
			});
	}

	private MockMultipartFile createWorkbookFile(Consumer<Sheet> orderItemWriter) {
		try (
			XSSFWorkbook workbook = new XSSFWorkbook();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
		) {
			workbook.createSheet("데이터 기준");
			workbook.createSheet("결제 합계");
			Sheet sheet = workbook.createSheet("상품 주문 상세내역");
			orderItemWriter.accept(sheet);
			workbook.write(outputStream);
			return new MockMultipartFile(
				"file",
				"매출리포트.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				outputStream.toByteArray()
			);
		} catch (IOException exception) {
			throw new IllegalStateException("테스트 XLSX 파일을 만들 수 없습니다.", exception);
		}
	}

	private void writeHeader(Sheet sheet) {
		String[] headers = {
			"주문기준일자",
			"결제상태",
			"주문시작시각",
			"주문채널",
			"주문번호",
			"상품명",
			"상품코드",
			"카테고리",
			"옵션",
			"수량",
			"상품가격",
			"옵션가격",
			"상품할인 금액",
			"주문할인 금액",
			"실판매금액",
			"부가세액"
		};
		Row headerRow = sheet.createRow(0);
		for (int index = 0; index < headers.length; index++) {
			headerRow.createCell(index).setCellValue(headers[index]);
		}
	}

	private void writeOrderItemRow(
		Sheet sheet,
		int rowIndex,
		String orderDate,
		String paymentStatus,
		String orderTime,
		String quantity,
		String netSales
	) {
		Row row = sheet.createRow(rowIndex);
		row.createCell(0).setCellValue(orderDate);
		row.createCell(1).setCellValue(paymentStatus);
		row.createCell(2).setCellValue(orderTime);
		row.createCell(3).setCellValue("포스");
		row.createCell(4).setCellValue("ORD-1");
		row.createCell(5).setCellValue("김치찌개");
		row.createCell(6).setCellValue("P001");
		row.createCell(7).setCellValue("식사");
		row.createCell(8).setCellValue("");
		row.createCell(9).setCellValue(quantity);
		row.createCell(10).setCellValue("9000");
		row.createCell(11).setCellValue("0");
		row.createCell(12).setCellValue("0");
		row.createCell(13).setCellValue("0");
		row.createCell(14).setCellValue(netSales);
		row.createCell(15).setCellValue("1636");
	}
}
