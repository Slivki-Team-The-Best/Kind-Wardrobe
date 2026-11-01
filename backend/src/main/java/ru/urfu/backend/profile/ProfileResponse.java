package ru.urfu.backend.profile;

import java.time.OffsetDateTime;
import ru.urfu.backend.entity.User;
import ru.urfu.backend.entity.enums.UserRole;

/** Данные профиля текущего пользователя. */
public record ProfileResponse(
		Long id,
		String email,
		String phone,
		String name,
		String city,
		String avatarUrl,
		UserRole role,
		OffsetDateTime createdAt) {

	public static ProfileResponse from(User user) {
		return new ProfileResponse(user.getId(), user.getEmail(), user.getPhone(), user.getName(), user.getCity(),
				user.getAvatarUrl(), user.getRole(), user.getCreatedAt());
	}
}
