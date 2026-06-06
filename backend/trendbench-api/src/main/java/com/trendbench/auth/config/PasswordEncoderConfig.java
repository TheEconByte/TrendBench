package com.trendbench.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 비밀번호 단방향 해시를 담당하는 {@link PasswordEncoder} 빈 등록.
 *
 * <p>BCrypt 를 채택한 이유:
 * <ul>
 *   <li>per-password salt 가 hash 안에 내장된다 (별도 컬럼 불필요).</li>
 *   <li>strength 파라미터로 future-proof 한 비용 조정이 가능하다.</li>
 *   <li>Spring Security 가 기본적으로 권장하는 인코더이다.</li>
 * </ul>
 */
@Configuration
public class PasswordEncoderConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
