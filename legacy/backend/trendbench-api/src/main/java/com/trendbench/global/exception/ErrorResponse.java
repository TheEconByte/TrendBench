package com.trendbench.global.exception;

import java.util.Map;

public record ErrorResponse(
	String errorCode,
	String message,
	Map<String, Object> details
) {
	public static ErrorResponse of(ErrorCode errorCode, String message, Map<String, Object> details) {
		return new ErrorResponse(errorCode.name(), message, details);
	}
}
