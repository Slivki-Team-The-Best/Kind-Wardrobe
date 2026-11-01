package ru.urfu.backend;

import java.time.OffsetDateTime;
import ru.urfu.backend.config.JwtService;
import ru.urfu.backend.entity.User;
import ru.urfu.backend.entity.enums.UserRole;
import ru.urfu.backend.repository.UserRepository;

/** Создание пользователей и токенов для интеграционных тестов. */
public final class TestUsers {

	private TestUsers() {
	}

	public static User save(UserRepository users, String email, String phone, UserRole role) {
		User user = new User();
		user.setEmail(email);
		user.setPhone(phone);
		user.setPasswordHash("{test}hash");
		user.setRole(role);
		user.setTermsAcceptedAt(OffsetDateTime.now());
		return users.saveAndFlush(user);
	}

	public static String bearer(JwtService jwtService, User user) {
		return "Bearer " + jwtService.issueAccessToken(user).value();
	}
}
