package com.trendbench.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.trendbench.auth.dto.LoginRequest;
import com.trendbench.auth.dto.LoginResponse;
import com.trendbench.auth.jwt.JwtTokenProvider;
import com.trendbench.global.exception.AuthException;
import com.trendbench.global.exception.ErrorCode;
import com.trendbench.user.domain.UserRole;
import com.trendbench.user.entity.User;
import com.trendbench.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

	private UserRepository userRepository;
	private PasswordEncoder passwordEncoder;
	private JwtTokenProvider jwtTokenProvider;
	private AuthService authService;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		passwordEncoder = mock(PasswordEncoder.class);
		jwtTokenProvider = mock(JwtTokenProvider.class);
		authService = new AuthService(userRepository, passwordEncoder, jwtTokenProvider);
	}

	@Test
	void login_returnsAccessTokenAndUserId() {
		User user = mock(User.class);
		when(user.getUserId()).thenReturn(42L);
		when(user.getEmail()).thenReturn("test@test.com");
		when(user.getPasswordHash()).thenReturn("hashed");
		when(user.getRole()).thenReturn(UserRole.USER);

		when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("raw-pw", "hashed")).thenReturn(true);
		when(jwtTokenProvider.createToken(any(User.class))).thenReturn("jwt-token");

		LoginResponse response = authService.login(new LoginRequest("test@test.com", "raw-pw"));

		assertThat(response.accessToken()).isEqualTo("jwt-token");
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.userId()).isEqualTo(42L);
	}

	@Test
	void login_throwsInvalidCredential_whenEmailNotFound() {
		when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() ->
				authService.login(new LoginRequest("missing@test.com", "any")))
				.isInstanceOf(AuthException.class)
				.extracting(e -> ((AuthException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_CREDENTIAL);
	}

	@Test
	void login_throwsInvalidCredential_whenPasswordMismatch() {
		User user = mock(User.class);
		when(user.getPasswordHash()).thenReturn("hashed");

		when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("wrong-pw", "hashed")).thenReturn(false);

		assertThatThrownBy(() ->
				authService.login(new LoginRequest("test@test.com", "wrong-pw")))
				.isInstanceOf(AuthException.class)
				.extracting(e -> ((AuthException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_CREDENTIAL);
	}
}
