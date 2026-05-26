package com.trendbench.upload.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trendbench.global.exception.ErrorCode;
import com.trendbench.global.exception.UploadException;
import com.trendbench.upload.domain.SalesUploadStatus;
import com.trendbench.upload.dto.SalesUploadResponse;
import com.trendbench.upload.entity.SalesUpload;
import com.trendbench.upload.repository.SalesUploadRepository;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class PosUploadServiceTest {

	@Mock
	private SalesUploadRepository salesUploadRepository;

	@InjectMocks
	private PosUploadService posUploadService;

	@Test
	void createUploadWithXlsxFileCreatesPendingSalesUpload() {
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"매출리포트-260517154717.xlsx",
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
			"test".getBytes(StandardCharsets.UTF_8)
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
}
