package com.trendbench.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordEncoderConfigTest {

	private final PasswordEncoder passwordEncoder = new PasswordEncoderConfig().passwordEncoder();

	@Test
	void bean_isBCryptPasswordEncoder() {
		assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
	}

	@Test
	void encode_producesBCryptHash() {
		String raw = "1234abcd!";

		String encoded = passwordEncoder.encode(raw);

		assertThat(encoded).isNotBlank();
		assertThat(encoded).isNotEqualTo(raw);
		// BCrypt 해시는 $2a$, $2b$, $2y$ prefix 중 하나로 시작한다.
		assertThat(encoded).matches("^\\$2[aby]\\$.+");
	}

	@Test
	void matches_returnsTrue_forCorrectPassword() {
		String raw = "1234abcd!";
		String encoded = passwordEncoder.encode(raw);

		assertThat(passwordEncoder.matches(raw, encoded)).isTrue();
	}

	@Test
	void matches_returnsFalse_forWrongPassword() {
		String encoded = passwordEncoder.encode("right-password");

		assertThat(passwordEncoder.matches("wrong-password", encoded)).isFalse();
	}

	@Test
	void encode_producesDifferentHash_forSamePassword_dueToSalt() {
		String raw = "same-password";

		String hash1 = passwordEncoder.encode(raw);
		String hash2 = passwordEncoder.encode(raw);

		assertThat(hash1).isNotEqualTo(hash2);
		// 두 해시 모두 같은 평문에 대해 matches 가 통과해야 한다.
		assertThat(passwordEncoder.matches(raw, hash1)).isTrue();
		assertThat(passwordEncoder.matches(raw, hash2)).isTrue();
	}
}
