package com.trendbench.auth.controller;

import com.trendbench.auth.dto.LoginRequest;
import com.trendbench.auth.dto.LoginResponse;
import com.trendbench.auth.dto.LogoutResponse;
import com.trendbench.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	/**
	 * MVP 단계의 stateless JWT 환경에서는 서버측에서 토큰을 폐기하지 않는다.
	 * 클라이언트가 보유한 accessToken 을 폐기하도록 안내만 하고,
	 * 현재 요청 SecurityContext 만 비워 응답을 반환한다.
	 */
	@PostMapping("/logout")
	public LogoutResponse logout() {
		SecurityContextHolder.clearContext();
		return LogoutResponse.success();
	}
}
