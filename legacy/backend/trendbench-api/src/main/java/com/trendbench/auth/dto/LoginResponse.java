package com.trendbench.auth.dto;

public record LoginResponse(
		String accessToken,
		String tokenType,
		Long userId
) {
	public static LoginResponse of(String accessToken, Long userId) {
		return new LoginResponse(accessToken, "Bearer", userId);
	}
}
