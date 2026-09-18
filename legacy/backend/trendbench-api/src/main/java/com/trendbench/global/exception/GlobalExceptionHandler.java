package com.trendbench.global.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(UploadException.class)
	public ResponseEntity<ErrorResponse> handleUploadException(UploadException exception) {
		ErrorResponse response = ErrorResponse.of(
			exception.getErrorCode(),
			exception.getMessage(),
			exception.getDetails()
		);
		return ResponseEntity.status(exception.getStatus()).body(response);
	}

	@ExceptionHandler(AuthException.class)
	public ResponseEntity<ErrorResponse> handleAuthException(AuthException exception) {
		ErrorResponse response = ErrorResponse.of(
			exception.getErrorCode(),
			exception.getMessage(),
			exception.getDetails()
		);
		return ResponseEntity.status(exception.getStatus()).body(response);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException exception) {
		AuthException authException = AuthException.invalidToken();
		ErrorResponse response = ErrorResponse.of(
			authException.getErrorCode(),
			authException.getMessage(),
			authException.getDetails()
		);
		return ResponseEntity.status(authException.getStatus()).body(response);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException exception) {
		AuthException authException = AuthException.accessDenied();
		ErrorResponse response = ErrorResponse.of(
			authException.getErrorCode(),
			authException.getMessage(),
			authException.getDetails()
		);
		return ResponseEntity.status(authException.getStatus()).body(response);
	}

	@ExceptionHandler({
		MissingServletRequestParameterException.class,
		MissingServletRequestPartException.class,
		MethodArgumentTypeMismatchException.class
	})
	public ResponseEntity<ErrorResponse> handleInvalidRequest(Exception exception) {
		ErrorResponse response = ErrorResponse.of(
			ErrorCode.INVALID_UPLOAD_REQUEST,
			"업로드 요청 값이 올바르지 않습니다.",
			Map.of("reason", exception.getMessage())
		);
		return ResponseEntity.badRequest().body(response);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception exception) {
		ErrorResponse response = ErrorResponse.of(
			ErrorCode.INTERNAL_SERVER_ERROR,
			"서버 오류가 발생했습니다.",
			Map.of()
		);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	}
}
