package com.trendbench.upload.parser;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.trendbench.upload.validation.PosSheetValidator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.util.List;

class LocalUploadFixtureSmokeTest {

	private static final Path LOCAL_FIXTURE = Path.of(
		"..",
		"..",
		"uploads",
		"매출리포트-260613113244.xlsx"
	);

	@Test
	void localUploadFileCanBeOpenedAndValidated() throws IOException {
		assumeTrue(Files.exists(LOCAL_FIXTURE), "로컬 uploads 파일이 있을 때만 실행합니다.");

		MockMultipartFile file = new MockMultipartFile(
			"file",
			LOCAL_FIXTURE.getFileName().toString(),
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
			Files.readAllBytes(LOCAL_FIXTURE)
		);

		assertThatCode(() -> new PosSheetValidator().validateRequiredSheets(file))
			.doesNotThrowAnyException();
	}

	@Test
	void localUploadFileCanBeOpenedWithPosPassword() throws IOException {
		assumeTrue(Files.exists(LOCAL_FIXTURE), "로컬 uploads 파일이 있을 때만 실행합니다.");

		MockMultipartFile file = new MockMultipartFile(
			"file",
			LOCAL_FIXTURE.getFileName().toString(),
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
			Files.readAllBytes(LOCAL_FIXTURE)
		);

		try (Workbook workbook = PosWorkbookReader.open(file)) {
			for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
				System.out.println(workbook.getSheetName(index));
			}
		}
	}

	@Test
	void localUploadFileCanBeParsed() throws IOException {
		assumeTrue(Files.exists(LOCAL_FIXTURE), "로컬 uploads 파일이 있을 때만 실행합니다.");

		MockMultipartFile file = new MockMultipartFile(
			"file",
			LOCAL_FIXTURE.getFileName().toString(),
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
			Files.readAllBytes(LOCAL_FIXTURE)
		);

		PosDataBasis dataBasis = new PosDataBasisParser().parse(file);
		assertThat(dataBasis.reportStartDate()).hasToString("2026-06-11");
		assertThat(dataBasis.reportEndDate()).hasToString("2026-06-13");
		assertThat(dataBasis.settlementBasis()).isEqualTo("주문한 날");
		assertThat(dataBasis.aggregationUnit()).isEqualTo("일간");

		List<PosPaymentSummary> paymentSummaries = new PosPaymentSummaryParser().parse(
			file,
			dataBasis.reportStartDate(),
			dataBasis.reportEndDate()
		);
		assertThat(paymentSummaries).hasSize(1);
		assertThat(paymentSummaries.get(0).salesDate()).hasToString("2026-06-13");
		assertThat(paymentSummaries.get(0).grossSales()).isEqualTo(42000);
		assertThat(paymentSummaries.get(0).paymentCount()).isEqualTo(6);

		List<PosOrderItem> orderItems = new PosOrderItemParser().parse(
			file,
			dataBasis.reportStartDate(),
			dataBasis.reportEndDate()
		);
		assertThat(orderItems).isNotEmpty();
		assertThat(orderItems.get(0).orderDate()).hasToString("2026-06-13");
		assertThat(orderItems.get(0).orderTime()).isEqualTo("11:30:59");
		assertThat(orderItems.get(0).paymentStatus()).isEqualTo("완료");
		assertThat(orderItems.get(0).productName()).isEqualTo("sample 카페라떼");
		assertThat(orderItems.get(0).quantity()).isEqualTo(2);
		assertThat(orderItems.get(0).netSales()).isEqualTo(8000);
	}
}
