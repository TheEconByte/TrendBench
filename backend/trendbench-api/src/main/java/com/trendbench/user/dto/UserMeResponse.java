package com.trendbench.user.dto;

public record UserMeResponse(
		Long userId,
		String email,
		String nickname,
		String storeName
) {
}
