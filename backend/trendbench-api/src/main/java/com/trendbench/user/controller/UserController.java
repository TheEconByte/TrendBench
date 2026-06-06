package com.trendbench.user.controller;

import com.trendbench.global.exception.AuthException;
import com.trendbench.user.dto.UserMeResponse;
import com.trendbench.user.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/me")
	public UserMeResponse getMe(@AuthenticationPrincipal Long userId) {
		if (userId == null) {
			throw AuthException.invalidToken();
		}
		return userService.getMe(userId);
	}
}
