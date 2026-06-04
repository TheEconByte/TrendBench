package com.trendbench.upload.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trendbench.global.exception.GlobalExceptionHandler;
import com.trendbench.upload.dto.SalesUploadResponse;
import com.trendbench.upload.service.PosUploadService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
				"PENDING",
				"업로드가 접수되었습니다."
			));

		mockMvc.perform(multipart("/api/pos/uploads")
				.file(file)
				.param("storeId", "1"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.uploadId").value(15))
			.andExpect(jsonPath("$.storeId").value(1))
			.andExpect(jsonPath("$.fileName").value("매출리포트-260517154717.xlsx"))
			.andExpect(jsonPath("$.status").value("PENDING"))
			.andExpect(jsonPath("$.message").value("업로드가 접수되었습니다."));

		verify(posUploadService).createUpload(any(MultipartFile.class), eq(1L));
	}
}
