package com.trendbench.upload.clickhouse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

class SalesAggregationRepositoryTest {

	@Test
	void aggregateUploadInsertsAllAggregationTables() {
		JdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(JdbcTemplate.class);
		when(jdbcTemplate.update(anyString(), org.mockito.ArgumentMatchers.any(Object[].class)))
			.thenReturn(1);
		SalesAggregationRepository repository = new SalesAggregationRepository(jdbcTemplate);

		repository.aggregateUpload(1L, 15L);

		ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate, times(4)).update(sqlCaptor.capture(), org.mockito.ArgumentMatchers.any(Object[].class));
		assertThat(sqlCaptor.getAllValues().get(0)).contains("INSERT INTO fact_daily_sales");
		assertThat(sqlCaptor.getAllValues().get(1)).contains("INSERT INTO menu_sales_daily");
		assertThat(sqlCaptor.getAllValues().get(2)).contains("INSERT INTO hourly_sales");
		assertThat(sqlCaptor.getAllValues().get(3)).contains("INSERT INTO weekday_sales");
	}
}
