package com.trendbench.upload.controller;

import com.trendbench.upload.dto.DailySalesResponse;
import com.trendbench.upload.dto.HourlyAnalysisResponse;
import com.trendbench.upload.dto.MenuAnalysisResponse;
import com.trendbench.upload.dto.WeekdayAnalysisResponse;
import com.trendbench.upload.service.PosAnalysisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stores/{storeId}")
public class PosAnalysisController {

	private final PosAnalysisService posAnalysisService;

	public PosAnalysisController(PosAnalysisService posAnalysisService) {
		this.posAnalysisService = posAnalysisService;
	}

	@GetMapping("/sales/daily")
	public DailySalesResponse getDailySales(
		@PathVariable Long storeId,
		@RequestParam Long uploadId
	) {
		return posAnalysisService.getDailySales(storeId, uploadId);
	}

	@GetMapping("/analysis/menu")
	public MenuAnalysisResponse getMenuAnalysis(
		@PathVariable Long storeId,
		@RequestParam Long uploadId
	) {
		return posAnalysisService.getMenuAnalysis(storeId, uploadId);
	}

	@GetMapping("/analysis/hourly")
	public HourlyAnalysisResponse getHourlyAnalysis(
		@PathVariable Long storeId,
		@RequestParam Long uploadId
	) {
		return posAnalysisService.getHourlyAnalysis(storeId, uploadId);
	}

	@GetMapping("/analysis/weekday")
	public WeekdayAnalysisResponse getWeekdayAnalysis(
		@PathVariable Long storeId,
		@RequestParam Long uploadId
	) {
		return posAnalysisService.getWeekdayAnalysis(storeId, uploadId);
	}
}
