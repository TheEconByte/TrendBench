package com.trendbench.global.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class AuthException extends RuntimeException {

	private final ErrorCode errorCode;
	private final HttpStatus status;
	private final Map<String, Object> details;

	public AuthException(ErrorCode errorCode, String message, HttpStatus status, Map<String, Object> details) {
		super(message);
		this.errorCode = errorCode;
		this.status = status;
		this.details = details;
	}

	public static AuthException invalidCredential() {
		return new AuthException(
				ErrorCode.INVALID_CREDENTIAL,
				"이메일 또는 비밀번호가 올바르지 않습니다.",
				HttpStatus.UNAUTHORIZED,
				Map.of());
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public Map<String, Object> getDetails() {
		return details;
	}
}
