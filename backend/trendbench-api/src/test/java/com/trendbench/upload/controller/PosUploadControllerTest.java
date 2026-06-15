package com.trendbench.upload.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trendbench.global.exception.ErrorCode;
import com.trendbench.global.exception.GlobalExceptionHandler;
import com.trendbench.global.exception.UploadException;
import com.trendbench.upload.dto.SalesUploadResponse;
import com.trendbench.upload.dto.SalesUploadStatusResponse;
import com.trendbench.upload.service.PosUploadService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

class PosUploadControllerTest {

	private PosUploadService posUploadService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		posUploadService = org.mockito.Mockito.mock(PosUploadService.class);
		mockMvc = MockMvcBuilders
			.standaloneSetup(new PosUploadController(posUploadService))
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	void createUploadReturnsCreatedResponse() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"매출리포트-260517154717.xlsx",
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
			"test".getBytes(StandardCharsets.UTF_8)
		);
		when(posUploadService.createUpload(any(MultipartFile.class), eq(1L)))
			.thenReturn(new SalesUploadResponse(
				15L,
				1L,
				"매출리포트-260517154717.xlsx",
				"SUCCESS",
				"업로드 처리가 완료되었습니다."
			));

		mockMvc.perform(multipart("/api/pos/uploads")
				.file(file)
				.param("storeId", "1"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.uploadId").value(15))
			.andExpect(jsonPath("$.storeId").value(1))
			.andExpect(jsonPath("$.fileName").value("매출리포트-260517154717.xlsx"))
			.andExpect(jsonPath("$.status").value("SUCCESS"))
			.andExpect(jsonPath("$.message").value("업로드 처리가 완료되었습니다."));

		verify(posUploadService).createUpload(any(MultipartFile.class), eq(1L));
	}

	@Test
	void getUploadStatusReturnsUploadStatus() throws Exception {
		when(posUploadService.getUploadStatus(15L))
			.thenReturn(new SalesUploadStatusResponse(
				15L,
				1L,
				"SUCCESS",
				LocalDate.of(2026, 5, 1),
				LocalDate.of(2026, 5, 31),
				LocalDateTime.of(2026, 5, 17, 15, 52),
				null
			));

		mockMvc.perform(get("/api/pos/uploads/{uploadId}", 15L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.uploadId").value(15))
			.andExpect(jsonPath("$.storeId").value(1))
			.andExpect(jsonPath("$.status").value("SUCCESS"))
			.andExpect(jsonPath("$.reportStartDate").value("2026-05-01"))
			.andExpect(jsonPath("$.reportEndDate").value("2026-05-31"))
			.andExpect(jsonPath("$.processedAt").value("2026-05-17T15:52:00"))
			.andExpect(jsonPath("$.errorMessage").doesNotExist());

		verify(posUploadService).getUploadStatus(15L);
	}

	@Test
	void getUploadStatusReturnsNotFoundError() throws Exception {
		when(posUploadService.getUploadStatus(99L))
			.thenThrow(new UploadException(
				ErrorCode.UPLOAD_NOT_FOUND,
				"업로드 이력을 찾을 수 없습니다.",
				HttpStatus.NOT_FOUND,
				Map.of("uploadId", 99L)
			));

		mockMvc.perform(get("/api/pos/uploads/{uploadId}", 99L))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.errorCode").value("UPLOAD_NOT_FOUND"))
			.andExpect(jsonPath("$.message").value("업로드 이력을 찾을 수 없습니다."))
			.andExpect(jsonPath("$.details.uploadId").value(99));
	}

	@Test
	void createUploadReturnsMissingRequiredSheetError() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"매출리포트-260517154717.xlsx",
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
			"test".getBytes(StandardCharsets.UTF_8)
		);
		when(posUploadService.createUpload(any(MultipartFile.class), eq(1L)))
			.thenThrow(new UploadException(
				ErrorCode.MISSING_REQUIRED_SHEET,
				"필수 시트 '상품 주문 상세내역'이 존재하지 않습니다.",
				HttpStatus.BAD_REQUEST,
				Map.of(
					"sheetName", "상품 주문 상세내역",
					"requiredSheets", List.of("데이터 기준", "결제 합계", "상품 주문 상세내역")
				)
			));

		mockMvc.perform(multipart("/api/pos/uploads")
				.file(file)
				.param("storeId", "1"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("MISSING_REQUIRED_SHEET"))
			.andExpect(jsonPath("$.message").value("필수 시트 '상품 주문 상세내역'이 존재하지 않습니다."))
			.andExpect(jsonPath("$.details.sheetName").value("상품 주문 상세내역"))
			.andExpect(jsonPath("$.details.requiredSheets[0]").value("데이터 기준"))
			.andExpect(jsonPath("$.details.requiredSheets[1]").value("결제 합계"))
			.andExpect(jsonPath("$.details.requiredSheets[2]").value("상품 주문 상세내역"));
	}

	@Test
	void createUploadReturnsInvalidXlsxFileError() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"매출리포트-260517154717.xlsx",
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
			"test".getBytes(StandardCharsets.UTF_8)
		);
		when(posUploadService.createUpload(any(MultipartFile.class), eq(1L)))
			.thenThrow(new UploadException(
				ErrorCode.INVALID_XLSX_FILE,
				"업로드된 엑셀 파일을 읽을 수 없습니다.",
				HttpStatus.BAD_REQUEST,
				null
			));

		mockMvc.perform(multipart("/api/pos/uploads")
				.file(file)
				.param("storeId", "1"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("INVALID_XLSX_FILE"))
			.andExpect(jsonPath("$.message").value("업로드된 엑셀 파일을 읽을 수 없습니다."))
			.andExpect(jsonPath("$.details").doesNotExist());
	}
}
