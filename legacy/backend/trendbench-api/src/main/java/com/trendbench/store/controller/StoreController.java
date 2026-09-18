package com.trendbench.store.controller;

import com.trendbench.global.exception.AuthException;
import com.trendbench.store.dto.StoreCreateRequest;
import com.trendbench.store.dto.StoreCreateResponse;
import com.trendbench.store.service.StoreService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stores")
public class StoreController {

	private final StoreService storeService;

	public StoreController(StoreService storeService) {
		this.storeService = storeService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public StoreCreateResponse createStore(
			@AuthenticationPrincipal Long userId,
			@Valid @RequestBody StoreCreateRequest request) {
		if (userId == null) {
			throw AuthException.invalidToken();
		}
		return storeService.createStore(userId, request);
	}
}
