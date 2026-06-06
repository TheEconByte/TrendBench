package com.trendbench.user.service;

import com.trendbench.global.exception.AuthException;
import com.trendbench.store.entity.Store;
import com.trendbench.store.repository.StoreRepository;
import com.trendbench.user.dto.UserMeResponse;
import com.trendbench.user.entity.User;
import com.trendbench.user.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final StoreRepository storeRepository;

	public UserService(UserRepository userRepository, StoreRepository storeRepository) {
		this.userRepository = userRepository;
		this.storeRepository = storeRepository;
	}

	@Transactional(readOnly = true)
	public UserMeResponse getMe(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(AuthException::userNotFound);

		List<Store> stores = storeRepository.findAllByUser_UserId(userId);
		String storeName = stores.isEmpty() ? null : stores.get(0).getStoreName();

		return new UserMeResponse(
				user.getUserId(),
				user.getEmail(),
				user.getNickname(),
				storeName);
	}
}
