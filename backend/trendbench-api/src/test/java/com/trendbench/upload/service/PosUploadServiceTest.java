package com.trendbench.upload.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trendbench.global.exception.ErrorCode;
import com.trendbench.global.exception.UploadException;
import com.trendbench.upload.domain.SalesUploadStatus;
import com.trendbench.upload.dto.SalesUploadResponse;
import com.trendbench.upload.entity.SalesUpload;
import com.trendbench.upload.repository.SalesUploadRepository;
import com.trendbench.upload.validation.PosSheetValidator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

	@Spy
	private PosSheetValidator posSheetValidator;

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
		assertThat(response.status()).isEqualTo(SalesUploadStatus.PENDING.name());
		assertThat(response.message()).isEqualTo("업로드가 접수되었습니다.");

		ArgumentCaptor<SalesUpload> captor = ArgumentCaptor.forClass(SalesUpload.class);
		verify(salesUploadRepository).save(captor.capture());
		SalesUpload savedUpload = captor.getValue();
		assertThat(savedUpload.getStoreId()).isEqualTo(1L);
		assertThat(savedUpload.getOriginalFileName()).isEqualTo("매출리포트-260517154717.xlsx");
		assertThat(savedUpload.getFileType()).isEqualTo("xlsx");
		assertThat(savedUpload.getStatus()).isEqualTo(SalesUploadStatus.PENDING);
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
		verify(salesUploadRepository, times(2)).save(captor.capture());
		List<SalesUpload> savedUploads = captor.getAllValues();
		SalesUpload failedUpload = savedUploads.get(1);
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
				assertThat(exception.getMessage()).isEqualTo("업로드된 XLSX 파일을 읽을 수 없습니다.");
			});

		ArgumentCaptor<SalesUpload> captor = ArgumentCaptor.forClass(SalesUpload.class);
		verify(salesUploadRepository, times(2)).save(captor.capture());
		SalesUpload failedUpload = captor.getAllValues().get(1);
		assertThat(failedUpload.getUploadId()).isEqualTo(15L);
		assertThat(failedUpload.getStatus()).isEqualTo(SalesUploadStatus.FAILED);
		assertThat(failedUpload.getErrorMessage()).isEqualTo("업로드된 XLSX 파일을 읽을 수 없습니다.");
		assertThat(failedUpload.getProcessedAt()).isNotNull();
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
				assertThat(exception.getMessage()).isEqualTo("POS 매출리포트 XLSX 파일만 업로드할 수 있습니다.");
				assertThat(exception.getDetails()).containsEntry("allowedExtension", ".xlsx");
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
				fileName,
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				outputStream.toByteArray()
			);
		} catch (IOException exception) {
			throw new IllegalStateException("테스트 XLSX 파일을 만들 수 없습니다.", exception);
		}
	}
}
