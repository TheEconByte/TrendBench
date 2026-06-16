package com.trendbench.auth.jwt;

import com.trendbench.user.domain.UserRole;
import com.trendbench.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

	private final JwtProperties properties;
	private final SecretKey key;

	public JwtTokenProvider(JwtProperties properties) {
		this.properties = properties;
		this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
	}

	public String createToken(User user) {
		return createToken(user.getUserId(), user.getEmail(), user.getRole());
	}

	public String createToken(Long userId, String email, UserRole role) {
		long now = System.currentTimeMillis();
		Date issuedAt = new Date(now);
		Date expiration = new Date(now + properties.expirationMs());

		return Jwts.builder()
				.issuer(properties.issuer())
				.subject(String.valueOf(userId))
				.claim("userId", userId)
				.claim("email", email)
				.claim("role", role != null ? role.name() : UserRole.USER.name())
				.issuedAt(issuedAt)
				.expiration(expiration)
				.signWith(key)
				.compact();
	}

	public boolean validateToken(String token) {
		try {
			parseClaims(token);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}
	}

	public Long getUserId(String token) {
		Claims claims = parseClaims(token);
		Object userId = claims.get("userId");
		if (userId instanceof Number number) {
			return number.longValue();
		}
		return Long.parseLong(claims.getSubject());
	}

	public String getEmail(String token) {
		return parseClaims(token).get("email", String.class);
	}

	public UserRole getRole(String token) {
		String role = parseClaims(token).get("role", String.class);
		return role != null ? UserRole.valueOf(role) : UserRole.USER;
	}

	public long getExpirationMs() {
		return properties.expirationMs();
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
}
