package com.trendbench.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.trendbench.global.exception.AuthException;
import com.trendbench.global.exception.ErrorCode;
import com.trendbench.store.entity.Store;
import com.trendbench.store.repository.StoreRepository;
import com.trendbench.user.dto.UserMeResponse;
import com.trendbench.user.entity.User;
import com.trendbench.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserServiceTest {

	private UserRepository userRepository;
	private StoreRepository storeRepository;
	private UserService userService;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		storeRepository = mock(StoreRepository.class);
		userService = new UserService(userRepository, storeRepository);
	}

	@Test
	void getMe_returnsProfileWithStoreName() {
		User user = mock(User.class);
		when(user.getUserId()).thenReturn(1L);
		when(user.getEmail()).thenReturn("test@test.com");
		when(user.getNickname()).thenReturn("민석");

		Store store = mock(Store.class);
		when(store.getStoreName()).thenReturn("민석식당");

		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(storeRepository.findAllByUser_UserId(1L)).thenReturn(List.of(store));

		UserMeResponse response = userService.getMe(1L);

		assertThat(response.userId()).isEqualTo(1L);
		assertThat(response.email()).isEqualTo("test@test.com");
		assertThat(response.nickname()).isEqualTo("민석");
		assertThat(response.storeName()).isEqualTo("민석식당");
	}

	@Test
	void getMe_returnsNullStoreName_whenNoStoreRegistered() {
		User user = mock(User.class);
		when(user.getUserId()).thenReturn(1L);
		when(user.getEmail()).thenReturn("a@b.com");
		when(user.getNickname()).thenReturn("nick");

		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(storeRepository.findAllByUser_UserId(1L)).thenReturn(List.of());

		UserMeResponse response = userService.getMe(1L);

		assertThat(response.storeName()).isNull();
	}

	@Test
	void getMe_throwsUserNotFound_whenUserMissing() {
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.getMe(99L))
				.isInstanceOf(AuthException.class)
				.extracting(e -> ((AuthException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_NOT_FOUND);
	}
}
