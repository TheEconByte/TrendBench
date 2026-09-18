package com.trendbench.store.service;

import com.trendbench.global.exception.AuthException;
import com.trendbench.store.dto.StoreCreateRequest;
import com.trendbench.store.dto.StoreCreateResponse;
import com.trendbench.store.entity.Store;
import com.trendbench.store.repository.StoreRepository;
import com.trendbench.user.entity.User;
import com.trendbench.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoreService {

	private final StoreRepository storeRepository;
	private final UserRepository userRepository;

	public StoreService(StoreRepository storeRepository, UserRepository userRepository) {
		this.storeRepository = storeRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public StoreCreateResponse createStore(Long userId, StoreCreateRequest request) {
		User user = userRepository.findById(userId)
				.orElseThrow(AuthException::userNotFound);

		Store store = new Store(
				user,
				request.storeName(),
				request.businessNumber(),
				request.regionName(),
				request.businessAreaName(),
				request.industryName());

		Store saved = storeRepository.save(store);
		return StoreCreateResponse.from(saved);
	}
}
