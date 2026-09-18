package com.trendbench.upload.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.trendbench.upload.clickhouse.PosAnalysisRepository;
import com.trendbench.upload.dto.DailySalesItemResponse;
import com.trendbench.upload.dto.HourlyAnalysisItemResponse;
import com.trendbench.upload.dto.MenuAnalysisItemResponse;
import com.trendbench.upload.dto.WeekdayAnalysisItemResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PosAnalysisServiceTest {

	private final PosAnalysisRepository repository = org.mockito.Mockito.mock(PosAnalysisRepository.class);
	private final PosAnalysisService service = new PosAnalysisService(repository);

	@Test
	void getDailySalesReturnsRepositoryItems() {
		when(repository.findDailySales(1L, 15L))
			.thenReturn(List.of(new DailySalesItemResponse(LocalDate.of(2026, 6, 13), 42000, 6, 4, 10500)));

		var response = service.getDailySales(1L, 15L);

		assertThat(response.storeId()).isEqualTo(1L);
		assertThat(response.uploadId()).isEqualTo(15L);
		assertThat(response.items()).hasSize(1);
		assertThat(response.items().get(0).totalRevenue()).isEqualTo(42000);
	}

	@Test
	void getMenuAnalysisReturnsTopMenu() {
		when(repository.findMenuAnalysis(1L, 15L))
			.thenReturn(List.of(
				new MenuAnalysisItemResponse("아메리카노", "메뉴", 4000, 1, 0.1),
				new MenuAnalysisItemResponse("카페라떼", "메뉴", 21000, 5, 0.5)
			));

		var response = service.getMenuAnalysis(1L, 15L);

		assertThat(response.topMenu()).isEqualTo("카페라떼");
		assertThat(response.items()).hasSize(2);
		assertThat(response.items().get(0).salesShare()).isEqualTo(0.16);
		assertThat(response.items().get(1).salesShare()).isEqualTo(0.84);
	}

	@Test
	void getHourlyAnalysisReturnsPeakHour() {
		when(repository.findHourlyAnalysis(1L, 15L))
			.thenReturn(List.of(
				new HourlyAnalysisItemResponse(10, 10000, 2, 2),
				new HourlyAnalysisItemResponse(11, 42000, 6, 4)
			));

		var response = service.getHourlyAnalysis(1L, 15L);

		assertThat(response.peakHour()).isEqualTo(11);
	}

	@Test
	void getWeekdayAnalysisReturnsStrongAndWeakWeekday() {
		when(repository.findWeekdayAnalysis(1L, 15L))
			.thenReturn(List.of(
				new WeekdayAnalysisItemResponse(5, "금요일", 10000, 2, 2, 10000),
				new WeekdayAnalysisItemResponse(6, "토요일", 42000, 6, 4, 42000)
			));

		var response = service.getWeekdayAnalysis(1L, 15L);

		assertThat(response.strongWeekday()).isEqualTo("토요일");
		assertThat(response.weakWeekday()).isEqualTo("금요일");
	}
}
