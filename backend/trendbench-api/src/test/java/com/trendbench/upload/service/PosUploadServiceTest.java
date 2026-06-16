package com.trendbench.upload.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trendbench.global.exception.ErrorCode;
import com.trendbench.global.exception.UploadException;
import com.trendbench.upload.clickhouse.RawOrderItemRepository;
import com.trendbench.upload.clickhouse.SalesAggregationRepository;
import com.trendbench.upload.domain.SalesUploadStatus;
import com.trendbench.upload.dto.SalesUploadResponse;
import com.trendbench.upload.dto.SalesUploadStatusResponse;
import com.trendbench.upload.entity.SalesUpload;
import com.trendbench.upload.parser.PosDataBasisParser;
import com.trendbench.upload.parser.PosOrderItem;
import com.trendbench.upload.parser.PosOrderItemParser;
import com.trendbench.upload.parser.PosPaymentSummaryParser;
import com.trendbench.upload.repository.SalesUploadRepository;
import com.trendbench.upload.validation.PosSheetValidator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class PosUploadServiceTest {

	@Mock
	private SalesUploadRepository salesUploadRepository;

	@Mock
	private RawOrderItemRepository rawOrderItemRepository;

	@Mock
	private SalesAggregationRepository salesAggregationRepository;

	@Spy
	private PosSheetValidator posSheetValidator;

	@Spy
	private PosDataBasisParser posDataBasisParser;

	@Spy
	private PosPaymentSummaryParser posPaymentSummaryParser;

	@Spy
	private PosOrderItemParser posOrderItemParser;

	@InjectMocks
	private PosUploadService posUploadService;

	@Test
	void createUploadWithXlsxFileCreatesPendingSalesUpload() {
		MockMultipartFile file = createWorkbookFile(
			"매출리포트-260517154717.xlsx",
			"데이터 기준",
			"결제 합계",
			"상품 주문 상세내역"
		);
		when(salesUploadRepository.save(any(SalesUpload.class))).thenAnswer(invocation -> {
			SalesUpload salesUpload = invocation.getArgument(0);
			ReflectionTestUtils.setField(salesUpload, "uploadId", 15L);
			return salesUpload;
		});

		SalesUploadResponse response = posUploadService.createUpload(file, 1L);

		assertThat(response.uploadId()).isEqualTo(15L);
		assertThat(response.storeId()).isEqualTo(1L);
		assertThat(response.fileName()).isEqualTo("매출리포트-260517154717.xlsx");
		assertThat(response.status()).isEqualTo(SalesUploadStatus.SUCCESS.name());
		assertThat(response.message()).isEqualTo("업로드 처리가 완료되었습니다.");

		ArgumentCaptor<SalesUpload> captor = ArgumentCaptor.forClass(SalesUpload.class);
		verify(salesUploadRepository, times(3)).save(captor.capture());
		List<SalesUpload> savedUploads = captor.getAllValues();
		SalesUpload savedUpload = savedUploads.get(2);
		assertThat(savedUpload.getStoreId()).isEqualTo(1L);
		assertThat(savedUpload.getOriginalFileName()).isEqualTo("매출리포트-260517154717.xlsx");
		assertThat(savedUpload.getFileType()).isEqualTo("xlsx");
		assertThat(savedUpload.getStatus()).isEqualTo(SalesUploadStatus.SUCCESS);
		assertThat(savedUpload.getReportStartDate()).hasToString("2026-05-01");
		assertThat(savedUpload.getReportEndDate()).hasToString("2026-05-31");
		assertThat(savedUpload.getSettlementBasis()).isEqualTo("주문한 날");
		assertThat(savedUpload.getAggregationUnit()).isEqualTo("일간");
		assertThat(savedUpload.getProcessedAt()).isNotNull();

		ArgumentCaptor<List<PosOrderItem>> orderItemsCaptor = ArgumentCaptor.forClass(List.class);
		verify(rawOrderItemRepository).batchInsert(eq(1L), eq(15L), orderItemsCaptor.capture());
		assertThat(orderItemsCaptor.getValue()).hasSize(1);
		assertThat(orderItemsCaptor.getValue().get(0).productName()).isEqualTo("김치찌개");
		verify(salesAggregationRepository).aggregateUpload(1L, 15L);
	}

	@Test
	void createUploadWithInvalidDataBasisMarksUploadFailed() {
		MockMultipartFile file = createWorkbookFile(
			"매출리포트.xlsx",
			true,
			"데이터 기준",
			"결제 합계",
			"상품 주문 상세내역"
		);
		when(salesUploadRepository.save(any(SalesUpload.class))).thenAnswer(invocation -> {
			SalesUpload salesUpload = invocation.getArgument(0);
			ReflectionTestUtils.setField(salesUpload, "uploadId", 15L);
			return salesUpload;
		});

		assertThatThrownBy(() -> posUploadService.createUpload(file, 1L))
			.isInstanceOfSatisfying(UploadException.class, exception -> {
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MISSING_DATA_BASIS_VALUE);
				assertThat(exception.getMessage()).isEqualTo("데이터 기준 값 '시작일자'이 존재하지 않습니다.");
		});

		ArgumentCaptor<SalesUpload> captor = ArgumentCaptor.forClass(SalesUpload.class);
		verify(salesUploadRepository, times(3)).save(captor.capture());
		List<SalesUpload> savedUploads = captor.getAllValues();
		SalesUpload failedUpload = savedUploads.get(2);
		assertThat(failedUpload.getStatus()).isEqualTo(SalesUploadStatus.FAILED);
		assertThat(failedUpload.getErrorMessage()).isEqualTo("데이터 기준 값 '시작일자'이 존재하지 않습니다.");
		assertThat(failedUpload.getProcessedAt()).isNotNull();
	}

	@Test
	void createUploadWithMissingRequiredSheetMarksUploadFailed() {
		MockMultipartFile file = createWorkbookFile(
			"매출리포트.xlsx",
			"데이터 기준",
			"결제 합계"
		);
		when(salesUploadRepository.save(any(SalesUpload.class))).thenAnswer(invocation -> {
			SalesUpload salesUpload = invocation.getArgument(0);
			ReflectionTestUtils.setField(salesUpload, "uploadId", 15L);
			return salesUpload;
		});

		assertThatThrownBy(() -> posUploadService.createUpload(file, 1L))
			.isInstanceOfSatisfying(UploadException.class, exception -> {
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MISSING_REQUIRED_SHEET);
				assertThat(exception.getMessage()).isEqualTo("필수 시트 '상품 주문 상세내역'이 존재하지 않습니다.");
			});

		ArgumentCaptor<SalesUpload> captor = ArgumentCaptor.forClass(SalesUpload.class);
		verify(salesUploadRepository, times(3)).save(captor.capture());
		List<SalesUpload> savedUploads = captor.getAllValues();
		SalesUpload failedUpload = savedUploads.get(2);
		assertThat(failedUpload.getUploadId()).isEqualTo(15L);
		assertThat(failedUpload.getStoreId()).isEqualTo(1L);
		assertThat(failedUpload.getStatus()).isEqualTo(SalesUploadStatus.FAILED);
		assertThat(failedUpload.getErrorMessage()).isEqualTo("필수 시트 '상품 주문 상세내역'이 존재하지 않습니다.");
		assertThat(failedUpload.getProcessedAt()).isNotNull();
	}

	@Test
	void createUploadWithUnreadableWorkbookMarksUploadFailed() {
		MultipartFile file = new MockMultipartFile(
			"file",
			"매출리포트.xlsx",
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
			"not-an-xlsx".getBytes(StandardCharsets.UTF_8)
		);
		when(salesUploadRepository.save(any(SalesUpload.class))).thenAnswer(invocation -> {
			SalesUpload salesUpload = invocation.getArgument(0);
			ReflectionTestUtils.setField(salesUpload, "uploadId", 15L);
			return salesUpload;
		});

		assertThatThrownBy(() -> posUploadService.createUpload(file, 1L))
			.isInstanceOfSatisfying(UploadException.class, exception -> {
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_XLSX_FILE);
				assertThat(exception.getMessage()).isEqualTo("업로드된 엑셀 파일을 읽을 수 없습니다.");
		});

		ArgumentCaptor<SalesUpload> captor = ArgumentCaptor.forClass(SalesUpload.class);
		verify(salesUploadRepository, times(3)).save(captor.capture());
		List<SalesUpload> savedUploads = captor.getAllValues();
		SalesUpload failedUpload = savedUploads.get(2);
		assertThat(failedUpload.getUploadId()).isEqualTo(15L);
		assertThat(failedUpload.getStatus()).isEqualTo(SalesUploadStatus.FAILED);
		assertThat(failedUpload.getErrorMessage()).isEqualTo("업로드된 엑셀 파일을 읽을 수 없습니다.");
		assertThat(failedUpload.getProcessedAt()).isNotNull();
	}

	@Test
	void createUploadMarksFailedWhenRawOrderItemInsertFails() {
		MockMultipartFile file = createWorkbookFile(
			"매출리포트.xlsx",
			"데이터 기준",
			"결제 합계",
			"상품 주문 상세내역"
		);
		when(salesUploadRepository.save(any(SalesUpload.class))).thenAnswer(invocation -> {
			SalesUpload salesUpload = invocation.getArgument(0);
			ReflectionTestUtils.setField(salesUpload, "uploadId", 15L);
			return salesUpload;
		});
		doThrow(new RuntimeException("clickhouse unavailable"))
			.when(rawOrderItemRepository)
			.batchInsert(eq(1L), eq(15L), any());

		assertThatThrownBy(() -> posUploadService.createUpload(file, 1L))
			.isInstanceOfSatisfying(UploadException.class, exception -> {
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
				assertThat(exception.getMessage()).isEqualTo("POS 주문 상세내역 저장에 실패했습니다.");
			});

		ArgumentCaptor<SalesUpload> captor = ArgumentCaptor.forClass(SalesUpload.class);
		verify(salesUploadRepository, times(3)).save(captor.capture());
		SalesUpload failedUpload = captor.getAllValues().get(2);
		assertThat(failedUpload.getStatus()).isEqualTo(SalesUploadStatus.FAILED);
		assertThat(failedUpload.getErrorMessage()).isEqualTo("POS 주문 상세내역 저장에 실패했습니다.");
	}

	@Test
	void createUploadMarksFailedWhenSalesAggregationFails() {
		MockMultipartFile file = createWorkbookFile(
			"매출리포트.xlsx",
			"데이터 기준",
			"결제 합계",
			"상품 주문 상세내역"
		);
		when(salesUploadRepository.save(any(SalesUpload.class))).thenAnswer(invocation -> {
			SalesUpload salesUpload = invocation.getArgument(0);
			ReflectionTestUtils.setField(salesUpload, "uploadId", 15L);
			return salesUpload;
		});
		doThrow(new RuntimeException("aggregation failed"))
			.when(salesAggregationRepository)
			.aggregateUpload(1L, 15L);

		assertThatThrownBy(() -> posUploadService.createUpload(file, 1L))
			.isInstanceOfSatisfying(UploadException.class, exception -> {
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
				assertThat(exception.getMessage()).isEqualTo("POS 주문 상세내역 저장에 실패했습니다.");
			});

		ArgumentCaptor<SalesUpload> captor = ArgumentCaptor.forClass(SalesUpload.class);
		verify(salesUploadRepository, times(3)).save(captor.capture());
		SalesUpload failedUpload = captor.getAllValues().get(2);
		assertThat(failedUpload.getStatus()).isEqualTo(SalesUploadStatus.FAILED);
		assertThat(failedUpload.getErrorMessage()).isEqualTo("POS 주문 상세내역 저장에 실패했습니다.");
	}

	@Test
	void getUploadStatusReturnsStoredUploadStatus() {
		SalesUpload salesUpload = new SalesUpload(1L, "매출리포트.xlsx", "xlsx");
		ReflectionTestUtils.setField(salesUpload, "uploadId", 15L);
		ReflectionTestUtils.setField(salesUpload, "reportStartDate", LocalDate.of(2026, 5, 1));
		ReflectionTestUtils.setField(salesUpload, "reportEndDate", LocalDate.of(2026, 5, 31));
		ReflectionTestUtils.setField(salesUpload, "status", SalesUploadStatus.SUCCESS);
		ReflectionTestUtils.setField(salesUpload, "processedAt", LocalDateTime.of(2026, 5, 17, 15, 52));
		when(salesUploadRepository.findById(15L)).thenReturn(Optional.of(salesUpload));

		SalesUploadStatusResponse response = posUploadService.getUploadStatus(15L);

		assertThat(response.uploadId()).isEqualTo(15L);
		assertThat(response.storeId()).isEqualTo(1L);
		assertThat(response.status()).isEqualTo("SUCCESS");
		assertThat(response.reportStartDate()).isEqualTo(LocalDate.of(2026, 5, 1));
		assertThat(response.reportEndDate()).isEqualTo(LocalDate.of(2026, 5, 31));
		assertThat(response.processedAt()).isEqualTo(LocalDateTime.of(2026, 5, 17, 15, 52));
		assertThat(response.errorMessage()).isNull();
	}

	@Test
	void getUploadStatusThrowsWhenUploadDoesNotExist() {
		when(salesUploadRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> posUploadService.getUploadStatus(99L))
			.isInstanceOfSatisfying(UploadException.class, exception -> {
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UPLOAD_NOT_FOUND);
				assertThat(exception.getMessage()).isEqualTo("업로드 이력을 찾을 수 없습니다.");
				assertThat(exception.getDetails()).containsEntry("uploadId", 99L);
			});
	}

	@Test
	void createUploadWithNonXlsxFileRejectsRequest() {
		MultipartFile file = new MockMultipartFile(
			"file",
			"sales.csv",
			"text/csv",
			"test".getBytes(StandardCharsets.UTF_8)
		);

		assertThatThrownBy(() -> posUploadService.createUpload(file, 1L))
			.isInstanceOfSatisfying(UploadException.class, exception -> {
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_FILE_TYPE);
				assertThat(exception.getMessage()).isEqualTo("POS 매출리포트 엑셀 파일만 업로드할 수 있습니다.");
				assertThat(exception.getDetails()).containsEntry("allowedExtensions", List.of(".xlsx", ".xls"));
		});
		verify(salesUploadRepository, never()).save(any(SalesUpload.class));
	}

	@Test
	void createUploadWithEmptyFileRejectsRequest() {
		MultipartFile file = new MockMultipartFile(
			"file",
			"매출리포트.xlsx",
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
			new byte[0]
		);

		assertThatThrownBy(() -> posUploadService.createUpload(file, 1L))
			.isInstanceOfSatisfying(UploadException.class, exception -> {
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMPTY_FILE);
				assertThat(exception.getDetails()).containsEntry("field", "file");
			});
		verify(salesUploadRepository, never()).save(any(SalesUpload.class));
	}

	@Test
	void createUploadWithInvalidStoreIdRejectsRequest() {
		MultipartFile file = new MockMultipartFile(
			"file",
			"매출리포트.xlsx",
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
			"test".getBytes(StandardCharsets.UTF_8)
		);

		assertThatThrownBy(() -> posUploadService.createUpload(file, 0L))
			.isInstanceOfSatisfying(UploadException.class, exception -> {
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_UPLOAD_REQUEST);
				assertThat(exception.getDetails()).containsEntry("field", "storeId");
		});
		verify(salesUploadRepository, never()).save(any(SalesUpload.class));
	}

	private MockMultipartFile createWorkbookFile(String fileName, String... sheetNames) {
		return createWorkbookFile(fileName, false, sheetNames);
	}

	private MockMultipartFile createWorkbookFile(String fileName, boolean skipDataBasisValues, String... sheetNames) {
		try (
			XSSFWorkbook workbook = new XSSFWorkbook();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
		) {
			for (String sheetName : sheetNames) {
				Sheet sheet = workbook.createSheet(sheetName);
				if ("데이터 기준".equals(sheetName) && !skipDataBasisValues) {
					writeDataBasisRows(sheet);
				}
				if ("결제 합계".equals(sheetName)) {
					writePaymentSummaryRows(sheet);
				}
				if ("상품 주문 상세내역".equals(sheetName)) {
					writeOrderItemRows(sheet);
				}
			}
			workbook.write(outputStream);
			return new MockMultipartFile(
				"file",
				fileName,
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				outputStream.toByteArray()
			);
		} catch (IOException exception) {
			throw new IllegalStateException("테스트 XLSX 파일을 만들 수 없습니다.", exception);
		}
	}

	private void writeDataBasisRows(Sheet sheet) {
		sheet.createRow(0).createCell(0).setCellValue("시작일자");
		sheet.getRow(0).createCell(1).setCellValue("2026-05-01");
		sheet.createRow(1).createCell(0).setCellValue("종료일자");
		sheet.getRow(1).createCell(1).setCellValue("2026-05-31");
		sheet.createRow(2).createCell(0).setCellValue("매출 정산 기준");
		sheet.getRow(2).createCell(1).setCellValue("주문한 날");
		sheet.createRow(3).createCell(0).setCellValue("집계 단위");
		sheet.getRow(3).createCell(1).setCellValue("일간");
	}

	private void writePaymentSummaryRows(Sheet sheet) {
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
		org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
		for (int index = 0; index < headers.length; index++) {
			headerRow.createCell(index).setCellValue(headers[index]);
		}
		sheet.createRow(1).createCell(0).setCellValue("2026-05-01");
		sheet.getRow(1).createCell(1).setCellValue(100000);
		sheet.getRow(1).createCell(2).setCellValue(9091);
		sheet.getRow(1).createCell(3).setCellValue(10);
		sheet.getRow(1).createCell(4).setCellValue(10000);
		sheet.getRow(1).createCell(5).setCellValue(70000);
		sheet.getRow(1).createCell(6).setCellValue(10000);
		sheet.getRow(1).createCell(7).setCellValue(5000);
		sheet.getRow(1).createCell(8).setCellValue(3000);
		sheet.getRow(1).createCell(9).setCellValue(2000);
	}

	private void writeOrderItemRows(Sheet sheet) {
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
		org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
		for (int index = 0; index < headers.length; index++) {
			headerRow.createCell(index).setCellValue(headers[index]);
		}

		org.apache.poi.ss.usermodel.Row row = sheet.createRow(1);
		row.createCell(0).setCellValue("2026-05-01");
		row.createCell(1).setCellValue("완료");
		row.createCell(2).setCellValue("12:30");
		row.createCell(3).setCellValue("포스");
		row.createCell(4).setCellValue("ORD-1");
		row.createCell(5).setCellValue("김치찌개");
		row.createCell(6).setCellValue("P001");
		row.createCell(7).setCellValue("식사");
		row.createCell(8).setCellValue("");
		row.createCell(9).setCellValue(2);
		row.createCell(10).setCellValue(9000);
		row.createCell(11).setCellValue(0);
		row.createCell(12).setCellValue(0);
		row.createCell(13).setCellValue(0);
		row.createCell(14).setCellValue(18000);
		row.createCell(15).setCellValue(1636);
	}
}
