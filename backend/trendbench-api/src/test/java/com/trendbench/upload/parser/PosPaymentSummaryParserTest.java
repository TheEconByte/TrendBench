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

class PosPaymentSummaryParserTest {

	private final PosPaymentSummaryParser parser = new PosPaymentSummaryParser();

	@Test
	void parseReadsPaymentSummaryRows() {
		MockMultipartFile file = createWorkbookFile(sheet -> {
			writeHeader(sheet);
			writePaymentSummaryRow(sheet, 1, "2026-05-01", "100,000", "9,091", "10");
			writePaymentSummaryRow(sheet, 2, "2026/05/02", "120000", "10909", "12");
		});

		List<PosPaymentSummary> summaries = parser.parse(
			file,
			LocalDate.of(2026, 5, 1),
			LocalDate.of(2026, 5, 31)
		);

		assertThat(summaries).hasSize(2);
		PosPaymentSummary first = summaries.get(0);
		assertThat(first.salesDate()).isEqualTo(LocalDate.of(2026, 5, 1));
		assertThat(first.grossSales()).isEqualTo(100000);
		assertThat(first.vatAmount()).isEqualTo(9091);
		assertThat(first.paymentCount()).isEqualTo(10);
		assertThat(first.cashSales()).isEqualTo(10000);
		assertThat(first.cardSales()).isEqualTo(70000);
		assertThat(first.qrSales()).isEqualTo(10000);
		assertThat(first.bankTransferSales()).isEqualTo(5000);
		assertThat(first.prepaidSales()).isEqualTo(3000);
		assertThat(first.otherSales()).isEqualTo(2000);
	}

	@Test
	void parseFailsWhenRequiredColumnIsMissing() {
		MockMultipartFile file = createWorkbookFile(sheet -> {
			Row headerRow = sheet.createRow(0);
			headerRow.createCell(0).setCellValue("기간");
			headerRow.createCell(1).setCellValue("결제금액");
		});

		assertThatThrownBy(() -> parser.parse(
			file,
			LocalDate.of(2026, 5, 1),
			LocalDate.of(2026, 5, 31)
		))
			.isInstanceOfSatisfying(UploadException.class, exception -> {
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MISSING_REQUIRED_COLUMN);
				assertThat(exception.getMessage()).isEqualTo("결제 합계 필수 컬럼 '부가세'이 존재하지 않습니다.");
				assertThat(exception.getDetails()).containsEntry("columnName", "부가세");
			});
	}

	@Test
	void parseFailsWhenDateFormatIsInvalid() {
		MockMultipartFile file = createWorkbookFile(sheet -> {
			writeHeader(sheet);
			writePaymentSummaryRow(sheet, 1, "2026년 5월 1일", "100000", "9091", "10");
		});

		assertThatThrownBy(() -> parser.parse(
			file,
			LocalDate.of(2026, 5, 1),
			LocalDate.of(2026, 5, 31)
		))
			.isInstanceOfSatisfying(UploadException.class, exception -> {
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_DATE_FORMAT);
				assertThat(exception.getDetails()).containsEntry("columnName", "기간");
			});
	}

	@Test
	void parseFailsWhenNumericValueIsInvalid() {
		MockMultipartFile file = createWorkbookFile(sheet -> {
			writeHeader(sheet);
			writePaymentSummaryRow(sheet, 1, "2026-05-01", "-100000", "9091", "10");
		});

		assertThatThrownBy(() -> parser.parse(
			file,
			LocalDate.of(2026, 5, 1),
			LocalDate.of(2026, 5, 31)
		))
			.isInstanceOfSatisfying(UploadException.class, exception -> {
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_NUMERIC_VALUE);
				assertThat(exception.getDetails()).containsEntry("columnName", "결제금액");
			});
	}

	@Test
	void parseFailsWhenSalesDateIsOutOfReportPeriod() {
		MockMultipartFile file = createWorkbookFile(sheet -> {
			writeHeader(sheet);
			writePaymentSummaryRow(sheet, 1, "2026-06-01", "100000", "9091", "10");
		});

		assertThatThrownBy(() -> parser.parse(
			file,
			LocalDate.of(2026, 5, 1),
			LocalDate.of(2026, 5, 31)
		))
			.isInstanceOfSatisfying(UploadException.class, exception -> {
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OUT_OF_REPORT_PERIOD);
				assertThat(exception.getDetails()).containsEntry("salesDate", "2026-06-01");
			});
	}

	private MockMultipartFile createWorkbookFile(Consumer<Sheet> paymentSummaryWriter) {
		try (
			XSSFWorkbook workbook = new XSSFWorkbook();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
		) {
			workbook.createSheet("데이터 기준");
			Sheet sheet = workbook.createSheet("결제 합계");
			paymentSummaryWriter.accept(sheet);
			workbook.createSheet("상품 주문 상세내역");
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
			"기간",
			"결제금액",
			"부가세",
			"결제건수",
			"현금",
			"카드",
			"QR결제",
			"계좌이체",
			"선불지급수단",
			"기타"
		};
		Row headerRow = sheet.createRow(0);
		for (int index = 0; index < headers.length; index++) {
			headerRow.createCell(index).setCellValue(headers[index]);
		}
	}

	private void writePaymentSummaryRow(
		Sheet sheet,
		int rowIndex,
		String salesDate,
		String grossSales,
		String vatAmount,
		String paymentCount
	) {
		Row row = sheet.createRow(rowIndex);
		row.createCell(0).setCellValue(salesDate);
		row.createCell(1).setCellValue(grossSales);
		row.createCell(2).setCellValue(vatAmount);
		row.createCell(3).setCellValue(paymentCount);
		row.createCell(4).setCellValue("10000");
		row.createCell(5).setCellValue("70000");
		row.createCell(6).setCellValue("10000");
		row.createCell(7).setCellValue("5000");
		row.createCell(8).setCellValue("3000");
		row.createCell(9).setCellValue("2000");
	}
}
