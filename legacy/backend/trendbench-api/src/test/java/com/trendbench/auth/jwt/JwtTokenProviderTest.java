package com.trendbench.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.trendbench.user.domain.UserRole;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

	private final JwtProperties properties = new JwtProperties(
			"test-secret-key-must-be-at-least-32-bytes-long-aaaaaaa",
			3600000L,
			"trendbench-test"
	);
	private final JwtTokenProvider provider = new JwtTokenProvider(properties);

	@Test
	void createToken_validateToken_getUserId_roundTrip() {
		String token = provider.createToken(42L, "test@test.com", UserRole.USER);

		assertThat(token).isNotBlank();
		assertThat(provider.validateToken(token)).isTrue();
		assertThat(provider.getUserId(token)).isEqualTo(42L);
		assertThat(provider.getEmail(token)).isEqualTo("test@test.com");
		assertThat(provider.getRole(token)).isEqualTo(UserRole.USER);
	}

	@Test
	void validateToken_rejectsTamperedToken() {
		String token = provider.createToken(1L, "a@b.com", UserRole.USER);
		String tampered = token.substring(0, token.length() - 2) + "xx";

		assertThat(provider.validateToken(tampered)).isFalse();
	}

	@Test
	void validateToken_rejectsGarbage() {
		assertThat(provider.validateToken("not-a-jwt")).isFalse();
		assertThat(provider.validateToken("")).isFalse();
	}

	@Test
	void createToken_acceptsNullRole_defaultsToUser() {
		String token = provider.createToken(7L, "x@y.com", null);

		assertThat(provider.getRole(token)).isEqualTo(UserRole.USER);
	}
}
