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

	public static AuthException invalidToken() {
		return new AuthException(
				ErrorCode.INVALID_TOKEN,
				"유효하지 않은 토큰입니다.",
				HttpStatus.UNAUTHORIZED,
				Map.of());
	}

	public static AuthException expiredToken() {
		return new AuthException(
				ErrorCode.EXPIRED_TOKEN,
				"만료된 토큰입니다.",
				HttpStatus.UNAUTHORIZED,
				Map.of());
	}

	public static AuthException accessDenied() {
		return new AuthException(
				ErrorCode.ACCESS_DENIED,
				"접근 권한이 없습니다.",
				HttpStatus.FORBIDDEN,
				Map.of());
	}

	public static AuthException userNotFound() {
		return new AuthException(
				ErrorCode.USER_NOT_FOUND,
				"사용자를 찾을 수 없습니다.",
				HttpStatus.NOT_FOUND,
				Map.of());
	}

	public static AuthException emailAlreadyExists(String email) {
		return new AuthException(
				ErrorCode.EMAIL_ALREADY_EXISTS,
				"이미 사용 중인 이메일입니다.",
				HttpStatus.CONFLICT,
				Map.of("email", email));
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
