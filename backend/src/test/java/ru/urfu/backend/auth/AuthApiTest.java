package ru.urfu.backend.auth;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import ru.urfu.backend.PostgresIntegrationTest;
import ru.urfu.backend.entity.User;
import ru.urfu.backend.repository.UserRepository;
import ru.urfu.backend.entity.enums.UserRole;

@AutoConfigureMockMvc
class AuthApiTest extends PostgresIntegrationTest {

	@Autowired MockMvc mvc;
	@Autowired UserRepository users;
	@Autowired PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		users.deleteAll();
		User anna = new User();
		anna.setEmail("anna@example.com");
		anna.setPhone("+79990001122");
		anna.setPasswordHash(passwordEncoder.encode("secret-123"));
		anna.setRole(UserRole.ORGANIZER);
		anna.setTermsAcceptedAt(OffsetDateTime.now());
		users.save(anna);
	}

	@Test
	void passwordIsStoredAsBcryptHash() {
		String hash = users.findByEmail("anna@example.com").orElseThrow().getPasswordHash();
		org.assertj.core.api.Assertions.assertThat(hash).startsWith("$2").isNotEqualTo("secret-123");
	}

	@Test
	void loginByEmailReturnsTokenThatOpensProtectedEndpoint() throws Exception {
		String token = loginAndGetToken("anna@example.com", "secret-123");

		mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email", is("anna@example.com")))
				.andExpect(jsonPath("$.role", is("ORGANIZER")));
	}

	@Test
	void emailIsCaseInsensitiveAndPhoneWorksAsIdentifier() throws Exception {
		loginAndGetToken("  ANNA@Example.com ", "secret-123");
		loginAndGetToken("+79990001122", "secret-123");
	}

	@Test
	void wrongPasswordAndUnknownUserGiveSameUnauthorizedAnswer() throws Exception {
		mvc.perform(login("anna@example.com", "wrong"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail", is("Неверные учётные данные")));
		mvc.perform(login("nobody@example.com", "secret-123"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail", is("Неверные учётные данные")));
	}

	@Test
	void blankFieldsAreBadRequest() throws Exception {
		mvc.perform(login("", "")).andExpect(status().isBadRequest());
	}

	@Test
	void protectedEndpointRequiresValidToken() throws Exception {
		mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/auth/me").header("Authorization", "Bearer not.a.token"))
				.andExpect(status().isUnauthorized());
	}

	private String loginAndGetToken(String identifier, String password) throws Exception {
		MvcResult result = mvc.perform(login(identifier, password))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType", is("Bearer")))
				.andExpect(jsonPath("$.accessToken", not(emptyString())))
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder login(String identifier, String password) {
		return post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"identifier\":\"%s\",\"password\":\"%s\"}".formatted(identifier, password));
	}
}
