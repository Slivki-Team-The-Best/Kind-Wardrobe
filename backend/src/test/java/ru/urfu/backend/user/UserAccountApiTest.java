package ru.urfu.backend.user;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import ru.urfu.backend.ApiTestSupport;

class UserAccountApiTest extends ApiTestSupport {

	@Test
	void uploadsAvatarAndReturnsProfile() throws Exception {
		String token = registerUser("anna@example.com", "+79990000001");

		mvc.perform(multipart("/api/users/me/avatar")
						.file(new MockMultipartFile("avatar", "me.png", "image/png", PNG))
						.header("Authorization", bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email", is("anna@example.com")))
				.andExpect(jsonPath("$.avatarUrl", startsWith("/uploads/avatars/")));
	}

	@Test
	void rejectsNonImageAvatar() throws Exception {
		String token = registerUser("anna@example.com", "+79990000001");

		mvc.perform(multipart("/api/users/me/avatar")
						.file(new MockMultipartFile("avatar", "me.png", "image/png", "not an image".getBytes()))
						.header("Authorization", bearer(token)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.avatar", is("Можно загружать только JPG или PNG")));
	}

	@Test
	void statsStartAtZero() throws Exception {
		String token = registerUser("anna@example.com", "+79990000001");

		mvc.perform(get("/api/users/me/stats").header("Authorization", bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.savedKg", is(0)))
				.andExpect(jsonPath("$.exchangesCount", is(0)))
				.andExpect(jsonPath("$.activeListingsCount", is(0)))
				.andExpect(jsonPath("$.listingsCount", is(0)));
	}

	@Test
	void statsRequireToken() throws Exception {
		mvc.perform(get("/api/users/me/stats")).andExpect(status().isUnauthorized());
	}
}
