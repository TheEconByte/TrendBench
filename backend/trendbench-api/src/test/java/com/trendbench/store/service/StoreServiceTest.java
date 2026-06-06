package com.trendbench.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trendbench.global.exception.AuthException;
import com.trendbench.global.exception.ErrorCode;
import com.trendbench.store.dto.StoreCreateRequest;
import com.trendbench.store.dto.StoreCreateResponse;
import com.trendbench.store.entity.Store;
import com.trendbench.store.repository.StoreRepository;
import com.trendbench.user.entity.User;
import com.trendbench.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class StoreServiceTest {

	private StoreRepository storeRepository;
	private UserRepository userRepository;
	private StoreService storeService;

	@BeforeEach
	void setUp() {
		storeRepository = mock(StoreRepository.class);
		userRepository = mock(UserRepository.class);
		storeService = new StoreService(storeRepository, userRepository);
	}

	@Test
	void createStore_persistsStoreLinkedToUser_andReturnsResponse() {
		User user = mock(User.class);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		Store saved = mock(Store.class);
		when(saved.getStoreId()).thenReturn(7L);
		when(saved.getStoreName()).thenReturn("민석식당");
		when(storeRepository.save(any(Store.class))).thenReturn(saved);

		StoreCreateRequest request = new StoreCreateRequest(
				"민석식당", "123-45-67890", "강남구", "강남역", "한식");

		StoreCreateResponse response = storeService.createStore(1L, request);

		assertThat(response.storeId()).isEqualTo(7L);
		assertThat(response.storeName()).isEqualTo("민석식당");

		ArgumentCaptor<Store> captor = ArgumentCaptor.forClass(Store.class);
		verify(storeRepository).save(captor.capture());
		Store passed = captor.getValue();
		assertThat(passed.getUser()).isSameAs(user);
		assertThat(passed.getStoreName()).isEqualTo("민석식당");
		assertThat(passed.getBusinessNumber()).isEqualTo("123-45-67890");
		assertThat(passed.getRegionName()).isEqualTo("강남구");
		assertThat(passed.getBusinessAreaName()).isEqualTo("강남역");
		assertThat(passed.getIndustryName()).isEqualTo("한식");
	}

	@Test
	void createStore_throwsUserNotFound_whenUserMissing() {
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		StoreCreateRequest request = new StoreCreateRequest(
				"x", null, null, null, null);

		assertThatThrownBy(() -> storeService.createStore(99L, request))
				.isInstanceOf(AuthException.class)
				.extracting(e -> ((AuthException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_NOT_FOUND);
	}
}
