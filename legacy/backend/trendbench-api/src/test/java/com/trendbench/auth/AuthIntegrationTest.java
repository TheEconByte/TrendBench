package com.trendbench.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.trendbench.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 회원가입 → 로그인 → JWT 인증 전체 흐름을 검증하는 통합 테스트.
 *
 * <p>H2 in-memory DB + Spring Security + 실제 JwtTokenProvider 까지 컨텍스트를
 * 전부 로드한 상태로 MockMvc 를 통해 HTTP 레이어를 호출한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void cleanDatabase() {
		userRepository.deleteAll();
	}

	private ObjectNode signupBody(String email, String password, String nickname) {
		ObjectNode body = objectMapper.createObjectNode();
		body.put("email", email);
		body.put("password", password);
		body.put("nickname", nickname);
		return body;
	}

	private ObjectNode loginBody(String email, String password) {
		ObjectNode body = objectMapper.createObjectNode();
		body.put("email", email);
		body.put("password", password);
		return body;
	}

	@Test
	@DisplayName("회원가입 → 로그인 → /api/users/me 전체 인증 흐름")
	void fullSignupLoginAndAuthenticatedRequestFlow() throws Exception {
		// 1) 회원가입
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(
						signupBody("test@test.com", "1234abcd!", "민석"))))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.userId").exists())
			.andExpect(jsonPath("$.email").value("test@test.com"))
			.andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."));

		// 2) 로그인 → accessToken 획득
		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(
						loginBody("test@test.com", "1234abcd!"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.userId").exists())
			.andReturn();

		String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
				.get("accessToken").asText();
		assertThat(accessToken).isNotBlank();

		// 3) /api/users/me 호출 — JWT 인증 통과 시 본인 정보 반환
		mockMvc.perform(get("/api/users/me")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("test@test.com"))
			.andExpect(jsonPath("$.nickname").value("민석"))
			.andExpect(jsonPath("$.storeName").isEmpty());
	}

	@Test
	@DisplayName("중복 이메일 회원가입은 EMAIL_ALREADY_EXISTS 로 거절된다")
	void signup_returns409_whenEmailAlreadyExists() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(
						signupBody("dup@test.com", "1234abcd!", "민석"))))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(
						signupBody("dup@test.com", "another-pw", "다른닉네임"))))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_EXISTS"))
			.andExpect(jsonPath("$.details.email").value("dup@test.com"));
	}

	@Test
	@DisplayName("틀린 비밀번호 로그인은 INVALID_CREDENTIAL 로 거절된다")
	void login_returns401_whenPasswordWrong() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(
						signupBody("user@test.com", "1234abcd!", "민석"))))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(
						loginBody("user@test.com", "WRONG-PASSWORD"))))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIAL"));
	}

	@Test
	@DisplayName("존재하지 않는 이메일 로그인도 INVALID_CREDENTIAL 로 거절된다 (계정 존재 여부 노출 방지)")
	void login_returns401_whenEmailUnknown() throws Exception {
		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(
						loginBody("nobody@test.com", "any-password"))))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIAL"));
	}

	@Test
	@DisplayName("토큰 없이 보호된 엔드포인트 접근은 INVALID_TOKEN 로 거절된다")
	void usersMe_returns401_whenNoToken() throws Exception {
		mockMvc.perform(get("/api/users/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"));
	}

	@Test
	@DisplayName("위조된 토큰으로 보호된 엔드포인트 접근은 INVALID_TOKEN 로 거절된다")
	void usersMe_returns401_whenTokenTampered() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(
						signupBody("user@test.com", "1234abcd!", "민석"))))
			.andExpect(status().isCreated());

		MvcResult login = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsBytes(
						loginBody("user@test.com", "1234abcd!"))))
			.andExpect(status().isOk())
			.andReturn();

		String token = objectMapper.readTree(login.getResponse().getContentAsString())
				.get("accessToken").asText();
		// 서명 부분 마지막 글자 변조
		String tampered = token.substring(0, token.length() - 2) + "xx";

		mockMvc.perform(get("/api/users/me")
				.header("Authorization", "Bearer " + tampered))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"));
	}
}
