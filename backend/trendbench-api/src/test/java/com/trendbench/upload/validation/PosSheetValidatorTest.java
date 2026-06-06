package com.trendbench.upload.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.trendbench.global.exception.ErrorCode;
import com.trendbench.global.exception.UploadException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class PosSheetValidatorTest {

	private final PosSheetValidator posSheetValidator = new PosSheetValidator();

	@Test
	void validateRequiredSheetsPassesWithAllRequiredSheets() {
		MockMultipartFile file = createWorkbookFile(
			"데이터 기준",
			"결제 합계",
			"상품 주문 상세내역"
		);

		assertThatCode(() -> posSheetValidator.validateRequiredSheets(file))
			.doesNotThrowAnyException();
	}

	@Test
	void validateRequiredSheetsFailsWhenDataBasisSheetIsMissing() {
		MockMultipartFile file = createWorkbookFile(
			"결제 합계",
			"상품 주문 상세내역"
		);

		assertMissingRequiredSheet(file, "데이터 기준");
	}

	@Test
	void validateRequiredSheetsFailsWhenPaymentSummarySheetIsMissing() {
		MockMultipartFile file = createWorkbookFile(
			"데이터 기준",
			"상품 주문 상세내역"
		);

		assertMissingRequiredSheet(file, "결제 합계");
	}

	@Test
	void validateRequiredSheetsFailsWhenProductOrderDetailSheetIsMissing() {
		MockMultipartFile file = createWorkbookFile(
			"데이터 기준",
			"결제 합계"
		);

		assertMissingRequiredSheet(file, "상품 주문 상세내역");
	}

	@Test
	void validateRequiredSheetsAllowsExtraSheets() {
		MockMultipartFile file = createWorkbookFile(
			"데이터 기준",
			"결제 합계",
			"상품 주문 상세내역",
			"상품 주문 합계",
			"결제 상세내역"
		);

		assertThatCode(() -> posSheetValidator.validateRequiredSheets(file))
			.doesNotThrowAnyException();
	}

	@Test
	void validateRequiredSheetsAllowsDifferentSheetOrder() {
		MockMultipartFile file = createWorkbookFile(
			"상품 주문 상세내역",
			"결제 합계",
			"데이터 기준"
		);

		assertThatCode(() -> posSheetValidator.validateRequiredSheets(file))
			.doesNotThrowAnyException();
	}

	@Test
	void validateRequiredSheetsFailsWhenWorkbookCannotBeOpened() {
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"매출리포트.xlsx",
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
			"not-an-xlsx".getBytes()
		);

		assertThatThrownBy(() -> posSheetValidator.validateRequiredSheets(file))
			.isInstanceOfSatisfying(UploadException.class, exception -> {
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_XLSX_FILE);
				assertThat(exception.getMessage()).isEqualTo("업로드된 XLSX 파일을 읽을 수 없습니다.");
				assertThat(exception.getDetails()).isNull();
			});
	}

	private void assertMissingRequiredSheet(MockMultipartFile file, String sheetName) {
		assertThatThrownBy(() -> posSheetValidator.validateRequiredSheets(file))
			.isInstanceOfSatisfying(UploadException.class, exception -> {
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MISSING_REQUIRED_SHEET);
				assertThat(exception.getMessage()).isEqualTo("필수 시트 '" + sheetName + "'이 존재하지 않습니다.");
				assertThat(exception.getDetails()).containsEntry("sheetName", sheetName);
				assertThat(exception.getDetails()).containsKey("requiredSheets");
			});
	}

	private MockMultipartFile createWorkbookFile(String... sheetNames) {
		try (
			XSSFWorkbook workbook = new XSSFWorkbook();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
		) {
			for (String sheetName : sheetNames) {
				workbook.createSheet(sheetName);
			}
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
}
