package com.trendbench.global.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Spring Security 의 인가 실패 시점에서 발생하는 AccessDeniedException 을
 * {@link org.springframework.web.servlet.HandlerExceptionResolver} 로 위임한다.
 */
@Component
public class DelegatedAccessDeniedHandler implements AccessDeniedHandler {

	private final HandlerExceptionResolver resolver;

	public DelegatedAccessDeniedHandler(
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
		this.resolver = resolver;
	}

	@Override
	public void handle(
			HttpServletRequest request,
			HttpServletResponse response,
			AccessDeniedException accessDeniedException) {
		resolver.resolveException(request, response, null, accessDeniedException);
	}
}
