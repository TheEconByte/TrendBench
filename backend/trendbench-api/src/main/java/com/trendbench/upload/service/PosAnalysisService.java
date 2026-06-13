package com.trendbench.upload.service;

import com.trendbench.upload.clickhouse.PosAnalysisRepository;
import com.trendbench.upload.dto.DailySalesItemResponse;
import com.trendbench.upload.dto.DailySalesResponse;
import com.trendbench.upload.dto.HourlyAnalysisItemResponse;
import com.trendbench.upload.dto.HourlyAnalysisResponse;
import com.trendbench.upload.dto.MenuAnalysisItemResponse;
import com.trendbench.upload.dto.MenuAnalysisResponse;
import com.trendbench.upload.dto.WeekdayAnalysisItemResponse;
import com.trendbench.upload.dto.WeekdayAnalysisResponse;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PosAnalysisService {

	private final PosAnalysisRepository posAnalysisRepository;

	public PosAnalysisService(PosAnalysisRepository posAnalysisRepository) {
		this.posAnalysisRepository = posAnalysisRepository;
	}

	public DailySalesResponse getDailySales(Long storeId, Long uploadId) {
		List<DailySalesItemResponse> items = posAnalysisRepository.findDailySales(storeId, uploadId);
		return new DailySalesResponse(storeId, uploadId, items);
	}

	public MenuAnalysisResponse getMenuAnalysis(Long storeId, Long uploadId) {
		List<MenuAnalysisItemResponse> items = posAnalysisRepository.findMenuAnalysis(storeId, uploadId);
		String topMenu = items.stream()
			.max(Comparator.comparingLong(MenuAnalysisItemResponse::totalSales))
			.map(MenuAnalysisItemResponse::productName)
			.orElse(null);
		return new MenuAnalysisResponse(storeId, uploadId, items, topMenu);
	}

	public HourlyAnalysisResponse getHourlyAnalysis(Long storeId, Long uploadId) {
		List<HourlyAnalysisItemResponse> items = posAnalysisRepository.findHourlyAnalysis(storeId, uploadId);
		Integer peakHour = items.stream()
			.max(Comparator.comparingLong(HourlyAnalysisItemResponse::totalSales))
			.map(HourlyAnalysisItemResponse::salesHour)
			.orElse(null);
		return new HourlyAnalysisResponse(storeId, uploadId, items, peakHour);
	}

	public WeekdayAnalysisResponse getWeekdayAnalysis(Long storeId, Long uploadId) {
		List<WeekdayAnalysisItemResponse> items = posAnalysisRepository.findWeekdayAnalysis(storeId, uploadId);
		String strongWeekday = items.stream()
			.max(Comparator.comparingDouble(WeekdayAnalysisItemResponse::avgSales))
			.map(WeekdayAnalysisItemResponse::weekdayName)
			.orElse(null);
		String weakWeekday = items.stream()
			.min(Comparator.comparingDouble(WeekdayAnalysisItemResponse::avgSales))
			.map(WeekdayAnalysisItemResponse::weekdayName)
			.orElse(null);
		return new WeekdayAnalysisResponse(storeId, uploadId, items, strongWeekday, weakWeekday);
	}
}
