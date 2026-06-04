package com.trendbench.global.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class UploadException extends RuntimeException {

	private final ErrorCode errorCode;
	private final HttpStatus status;
	private final Map<String, Object> details;

	public UploadException(ErrorCode errorCode, String message, HttpStatus status, Map<String, Object> details) {
		super(message);
		this.errorCode = errorCode;
		this.status = status;
		this.details = details;
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
