package com.trendbench.upload.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trendbench.global.exception.GlobalExceptionHandler;
import com.trendbench.upload.dto.DailySalesItemResponse;
import com.trendbench.upload.dto.DailySalesResponse;
import com.trendbench.upload.dto.HourlyAnalysisItemResponse;
import com.trendbench.upload.dto.HourlyAnalysisResponse;
import com.trendbench.upload.dto.MenuAnalysisItemResponse;
import com.trendbench.upload.dto.MenuAnalysisResponse;
import com.trendbench.upload.dto.WeekdayAnalysisItemResponse;
import com.trendbench.upload.dto.WeekdayAnalysisResponse;
import com.trendbench.upload.service.PosAnalysisService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PosAnalysisControllerTest {

	private PosAnalysisService posAnalysisService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		posAnalysisService = org.mockito.Mockito.mock(PosAnalysisService.class);
		mockMvc = MockMvcBuilders
			.standaloneSetup(new PosAnalysisController(posAnalysisService))
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	void getDailySalesReturnsItems() throws Exception {
		when(posAnalysisService.getDailySales(1L, 15L))
			.thenReturn(new DailySalesResponse(
				1L,
				15L,
				List.of(new DailySalesItemResponse(LocalDate.of(2026, 6, 13), 42000, 6, 4, 10500))
			));

		mockMvc.perform(get("/api/stores/{storeId}/sales/daily", 1L).param("uploadId", "15"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.storeId").value(1))
			.andExpect(jsonPath("$.uploadId").value(15))
			.andExpect(jsonPath("$.items[0].saleDate").value("2026-06-13"))
			.andExpect(jsonPath("$.items[0].totalRevenue").value(42000));

		verify(posAnalysisService).getDailySales(1L, 15L);
	}

	@Test
	void getMenuAnalysisReturnsTopMenu() throws Exception {
		when(posAnalysisService.getMenuAnalysis(1L, 15L))
			.thenReturn(new MenuAnalysisResponse(
				1L,
				15L,
				List.of(new MenuAnalysisItemResponse("카페라떼", "메뉴", 21000, 5, 0.5)),
				"카페라떼"
			));

		mockMvc.perform(get("/api/stores/{storeId}/analysis/menu", 1L).param("uploadId", "15"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.topMenu").value("카페라떼"))
			.andExpect(jsonPath("$.items[0].productName").value("카페라떼"));
	}

	@Test
	void getHourlyAnalysisReturnsPeakHour() throws Exception {
		when(posAnalysisService.getHourlyAnalysis(1L, 15L))
			.thenReturn(new HourlyAnalysisResponse(
				1L,
				15L,
				List.of(new HourlyAnalysisItemResponse(11, 42000, 6, 4)),
				11
			));

		mockMvc.perform(get("/api/stores/{storeId}/analysis/hourly", 1L).param("uploadId", "15"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.peakHour").value(11))
			.andExpect(jsonPath("$.items[0].salesHour").value(11));
	}

	@Test
	void getWeekdayAnalysisReturnsStrongAndWeakWeekday() throws Exception {
		when(posAnalysisService.getWeekdayAnalysis(1L, 15L))
			.thenReturn(new WeekdayAnalysisResponse(
				1L,
				15L,
				List.of(new WeekdayAnalysisItemResponse(6, "토요일", 42000, 6, 4, 42000)),
				"토요일",
				"토요일"
			));

		mockMvc.perform(get("/api/stores/{storeId}/analysis/weekday", 1L).param("uploadId", "15"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.strongWeekday").value("토요일"))
			.andExpect(jsonPath("$.weakWeekday").value("토요일"))
			.andExpect(jsonPath("$.items[0].weekdayName").value("토요일"));
	}
}
