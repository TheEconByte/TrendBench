package com.trendbench.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trendbench.auth.dto.LoginRequest;
import com.trendbench.auth.dto.LoginResponse;
import com.trendbench.auth.dto.SignupRequest;
import com.trendbench.auth.dto.SignupResponse;
import com.trendbench.auth.jwt.JwtTokenProvider;
import com.trendbench.global.exception.AuthException;
import com.trendbench.global.exception.ErrorCode;
import com.trendbench.user.domain.UserRole;
import com.trendbench.user.entity.User;
import com.trendbench.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
	void signup_persistsHashedUser_returnsResponse() {
		when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
		when(passwordEncoder.encode("raw-pw")).thenReturn("hashed-pw");

		User saved = mock(User.class);
		when(saved.getUserId()).thenReturn(7L);
		when(saved.getEmail()).thenReturn("new@test.com");
		when(userRepository.save(any(User.class))).thenReturn(saved);

		SignupResponse response = authService.signup(
				new SignupRequest("new@test.com", "raw-pw", "민석"));

		assertThat(response.userId()).isEqualTo(7L);
		assertThat(response.email()).isEqualTo("new@test.com");
		assertThat(response.message()).isEqualTo("회원가입이 완료되었습니다.");

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		User passed = captor.getValue();
		assertThat(passed.getEmail()).isEqualTo("new@test.com");
		assertThat(passed.getNickname()).isEqualTo("민석");
		assertThat(passed.getPasswordHash()).isEqualTo("hashed-pw");
		assertThat(passed.getPasswordHash()).isNotEqualTo("raw-pw");
	}

	@Test
	void signup_throwsEmailAlreadyExists_whenDuplicate() {
		when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);

		assertThatThrownBy(() ->
				authService.signup(new SignupRequest("dup@test.com", "raw-pw", "민석")))
				.isInstanceOf(AuthException.class)
				.extracting(e -> ((AuthException) e).getErrorCode())
				.isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);

		verify(userRepository, org.mockito.Mockito.never()).save(any(User.class));
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
