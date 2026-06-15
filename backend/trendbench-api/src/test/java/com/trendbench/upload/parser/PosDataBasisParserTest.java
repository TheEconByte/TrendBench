package com.trendbench.upload.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.trendbench.global.exception.ErrorCode;
import com.trendbench.global.exception.UploadException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.function.Consumer;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class PosDataBasisParserTest {

	private final PosDataBasisParser posDataBasisParser = new PosDataBasisParser();

	@Test
	void parseReadsReportMetadataFromDataBasisSheet() {
		MockMultipartFile file = createWorkbookFile(sheet -> writeDefaultDataBasisRows(sheet));

		PosDataBasis dataBasis = posDataBasisParser.parse(file);

		assertThat(dataBasis.reportStartDate()).isEqualTo(LocalDate.of(2026, 5, 1));
		assertThat(dataBasis.reportEndDate()).isEqualTo(LocalDate.of(2026, 5, 31));
		assertThat(dataBasis.settlementBasis()).isEqualTo("주문한 날");
		assertThat(dataBasis.aggregationUnit()).isEqualTo("일간");
	}

	@Test
	void parseSupportsDotAndSlashDateText() {
		MockMultipartFile file = createWorkbookFile(sheet -> {
			writeRow(sheet, 0, "시작일자", "2026.05.01");
			writeRow(sheet, 1, "종료일자", "2026/05/31");
			writeRow(sheet, 2, "매출 정산 기준", "결제한 날");
			writeRow(sheet, 3, "집계 단위", "일간");
		});

		PosDataBasis dataBasis = posDataBasisParser.parse(file);

		assertThat(dataBasis.reportStartDate()).isEqualTo(LocalDate.of(2026, 5, 1));
		assertThat(dataBasis.reportEndDate()).isEqualTo(LocalDate.of(2026, 5, 31));
		assertThat(dataBasis.settlementBasis()).isEqualTo("결제한 날");
	}

	@Test
	void parseSupportsHeaderRowLayout() {
		MockMultipartFile file = createWorkbookFile(sheet -> {
			Row headerRow = sheet.createRow(0);
			headerRow.createCell(0).setCellValue("시작일자");
			headerRow.createCell(1).setCellValue("종료일자");
			headerRow.createCell(2).setCellValue("매출 정산 기준");
			headerRow.createCell(3).setCellValue("집계 단위");

			Row valueRow = sheet.createRow(1);
			valueRow.createCell(0).setCellValue("2026-05-01");
			valueRow.createCell(1).setCellValue("2026-05-31");
			valueRow.createCell(2).setCellValue("주문한 날");
			valueRow.createCell(3).setCellValue("일간");
		});

		PosDataBasis dataBasis = posDataBasisParser.parse(file);

		assertThat(dataBasis.reportStartDate()).isEqualTo(LocalDate.of(2026, 5, 1));
		assertThat(dataBasis.reportEndDate()).isEqualTo(LocalDate.of(2026, 5, 31));
		assertThat(dataBasis.settlementBasis()).isEqualTo("주문한 날");
		assertThat(dataBasis.aggregationUnit()).isEqualTo("일간");
	}

	@Test
	void parseSupportsExcelSerialDateCells() {
		MockMultipartFile file = createWorkbookFile(sheet -> {
			writeRow(sheet, 0, "시작일자", LocalDate.of(2026, 5, 1));
			writeRow(sheet, 1, "종료일자", LocalDate.of(2026, 5, 31));
			writeRow(sheet, 2, "매출 정산 기준", "주문한 날");
			writeRow(sheet, 3, "집계 단위", "일간");
		});

		PosDataBasis dataBasis = posDataBasisParser.parse(file);

		assertThat(dataBasis.reportStartDate()).isEqualTo(LocalDate.of(2026, 5, 1));
		assertThat(dataBasis.reportEndDate()).isEqualTo(LocalDate.of(2026, 5, 31));
	}

	@Test
	void parseFailsWhenRequiredValueIsMissing() {
		MockMultipartFile file = createWorkbookFile(sheet -> {
			writeRow(sheet, 0, "종료일자", "2026-05-31");
			writeRow(sheet, 1, "매출 정산 기준", "주문한 날");
			writeRow(sheet, 2, "집계 단위", "일간");
		});

		assertThatThrownBy(() -> posDataBasisParser.parse(file))
			.isInstanceOfSatisfying(UploadException.class, exception -> {
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MISSING_DATA_BASIS_VALUE);
				assertThat(exception.getMessage()).isEqualTo("데이터 기준 값 '시작일자'이 존재하지 않습니다.");
				assertThat(exception.getDetails()).containsEntry("field", "reportStartDate");
			});
	}

	@Test
	void parseFailsWhenReportPeriodIsInvalid() {
		MockMultipartFile file = createWorkbookFile(sheet -> {
			writeRow(sheet, 0, "시작일자", "2026-06-01");
			writeRow(sheet, 1, "종료일자", "2026-05-31");
			writeRow(sheet, 2, "매출 정산 기준", "주문한 날");
			writeRow(sheet, 3, "집계 단위", "일간");
		});

		assertThatThrownBy(() -> posDataBasisParser.parse(file))
			.isInstanceOfSatisfying(UploadException.class, exception -> {
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REPORT_PERIOD);
				assertThat(exception.getMessage()).isEqualTo("리포트 시작일은 종료일보다 늦을 수 없습니다.");
			});
	}

	@Test
	void parseFailsWhenAggregationUnitIsNotDaily() {
		MockMultipartFile file = createWorkbookFile(sheet -> {
			writeRow(sheet, 0, "시작일자", "2026-05-01");
			writeRow(sheet, 1, "종료일자", "2026-05-31");
			writeRow(sheet, 2, "매출 정산 기준", "주문한 날");
			writeRow(sheet, 3, "집계 단위", "월간");
		});

		assertThatThrownBy(() -> posDataBasisParser.parse(file))
			.isInstanceOfSatisfying(UploadException.class, exception -> {
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_AGGREGATION_UNIT);
				assertThat(exception.getMessage()).isEqualTo("집계 단위는 MVP 기준 '일간'이어야 합니다.");
				assertThat(exception.getDetails()).containsEntry("aggregationUnit", "월간");
			});
	}

	@Test
	void parseFailsWhenDateFormatIsInvalid() {
		MockMultipartFile file = createWorkbookFile(sheet -> {
			writeRow(sheet, 0, "시작일자", "2026년 5월 1일");
			writeRow(sheet, 1, "종료일자", "2026-05-31");
			writeRow(sheet, 2, "매출 정산 기준", "주문한 날");
			writeRow(sheet, 3, "집계 단위", "일간");
		});

		assertThatThrownBy(() -> posDataBasisParser.parse(file))
			.isInstanceOfSatisfying(UploadException.class, exception -> {
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_DATA_BASIS_SHEET);
				assertThat(exception.getMessage()).isEqualTo("데이터 기준 값 '시작일자'의 날짜 형식이 올바르지 않습니다.");
			});
	}

	private MockMultipartFile createWorkbookFile(Consumer<Sheet> dataBasisWriter) {
		try (
			XSSFWorkbook workbook = new XSSFWorkbook();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
		) {
			Sheet sheet = workbook.createSheet("데이터 기준");
			dataBasisWriter.accept(sheet);
			workbook.createSheet("결제 합계");
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

	private void writeDefaultDataBasisRows(Sheet sheet) {
		writeRow(sheet, 0, "시작일자", "2026-05-01");
		writeRow(sheet, 1, "종료일자", "2026-05-31");
		writeRow(sheet, 2, "매출 정산 기준", "주문한 날");
		writeRow(sheet, 3, "집계 단위", "일간");
	}

	private void writeRow(Sheet sheet, int rowIndex, String label, String value) {
		Row row = sheet.createRow(rowIndex);
		row.createCell(0).setCellValue(label);
		row.createCell(1).setCellValue(value);
	}

	private void writeRow(Sheet sheet, int rowIndex, String label, LocalDate value) {
		Row row = sheet.createRow(rowIndex);
		row.createCell(0).setCellValue(label);
		Cell valueCell = row.createCell(1);
		valueCell.setCellValue(value);
	}
}
