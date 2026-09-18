package com.trendbench.auth.dto;

import com.trendbench.user.entity.User;

public record SignupResponse(
		Long userId,
		String email,
		String message
) {
	public static SignupResponse from(User user) {
		return new SignupResponse(
				user.getUserId(),
				user.getEmail(),
				"회원가입이 완료되었습니다."
		);
	}
}
