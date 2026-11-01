package ru.urfu.backend.profile;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import ru.urfu.backend.PostgresIntegrationTest;
import ru.urfu.backend.TestUsers;
import ru.urfu.backend.config.JwtService;
import ru.urfu.backend.entity.User;
import ru.urfu.backend.entity.enums.UserRole;
import ru.urfu.backend.repository.UserRepository;

@AutoConfigureMockMvc
@Transactional
class ProfileApiTest extends PostgresIntegrationTest {

	@Autowired MockMvc mvc;
	@Autowired UserRepository users;
	@Autowired JwtService jwtService;

	@Test
	void returnsOwnProfile() throws Exception {
		User user = TestUsers.save(users, "profile-get@example.com", "+79001110001", UserRole.USER);

		mvc.perform(get("/api/users/me").header("Authorization", TestUsers.bearer(jwtService, user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email", is("profile-get@example.com")))
				.andExpect(jsonPath("$.phone", is("+79001110001")))
				.andExpect(jsonPath("$.role", is("USER")))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void updatesNameCityAndPhoneButNotEmailOrRole() throws Exception {
		User user = TestUsers.save(users, "profile-upd@example.com", null, UserRole.USER);

		mvc.perform(update(user, """
				{"name":"  Анна Смирнова ","city":"Москва","phone":"+7 (916) 555-12-34",
				 "email":"hacker@example.com","role":"ORGANIZER"}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", is("Анна Смирнова")))
				.andExpect(jsonPath("$.city", is("Москва")))
				.andExpect(jsonPath("$.phone", is("+7 (916) 555-12-34")))
				.andExpect(jsonPath("$.email", is("profile-upd@example.com")))
				.andExpect(jsonPath("$.role", is("USER")));

		User saved = users.findById(user.getId()).orElseThrow();
		org.assertj.core.api.Assertions.assertThat(saved.getName()).isEqualTo("Анна Смирнова");
		org.assertj.core.api.Assertions.assertThat(saved.getEmail()).isEqualTo("profile-upd@example.com");
		org.assertj.core.api.Assertions.assertThat(saved.getRole()).isEqualTo(UserRole.USER);
	}

	@Test
	void blankValuesClearFields() throws Exception {
		User user = TestUsers.save(users, "profile-clear@example.com", "+79001110002", UserRole.USER);

		mvc.perform(update(user, "{\"name\":\"\",\"city\":\"  \",\"phone\":\"\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", nullValue()))
				.andExpect(jsonPath("$.city", nullValue()))
				.andExpect(jsonPath("$.phone", nullValue()));
	}

	@Test
	void rejectsInvalidPhone() throws Exception {
		User user = TestUsers.save(users, "profile-bad@example.com", null, UserRole.USER);

		mvc.perform(update(user, "{\"name\":\"Анна\",\"phone\":\"abc\"}")).andExpect(status().isBadRequest());
	}

	@Test
	void rejectsPhoneOfAnotherUserButAllowsOwnPhone() throws Exception {
		TestUsers.save(users, "profile-other@example.com", "+79001110003", UserRole.USER);
		User user = TestUsers.save(users, "profile-me@example.com", "+79001110004", UserRole.USER);

		mvc.perform(update(user, "{\"phone\":\"+79001110003\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail", is("Этот телефон уже используется")));
		mvc.perform(update(user, "{\"phone\":\"+79001110004\"}")).andExpect(status().isOk());
	}

	@Test
	void requiresAuthentication() throws Exception {
		mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
		mvc.perform(put("/api/users/me").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
	}

	private MockHttpServletRequestBuilder update(User user, String json) {
		return put("/api/users/me")
				.header("Authorization", TestUsers.bearer(jwtService, user))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json);
	}
}
