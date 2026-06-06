package com.trendbench.auth.service;

import com.trendbench.auth.dto.LoginRequest;
import com.trendbench.auth.dto.LoginResponse;
import com.trendbench.auth.jwt.JwtTokenProvider;
import com.trendbench.global.exception.AuthException;
import com.trendbench.user.entity.User;
import com.trendbench.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;

	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			JwtTokenProvider jwtTokenProvider) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenProvider = jwtTokenProvider;
	}

	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest request) {
		User user = userRepository.findByEmail(request.email())
				.orElseThrow(AuthException::invalidCredential);

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw AuthException.invalidCredential();
		}

		String accessToken = jwtTokenProvider.createToken(user);
		return LoginResponse.of(accessToken, user.getUserId());
	}
}
