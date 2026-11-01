package ru.urfu.backend.profile;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.urfu.backend.entity.User;
import ru.urfu.backend.repository.UserRepository;

@Service
public class ProfileService {

	private final UserRepository users;

	public ProfileService(UserRepository users) {
		this.users = users;
	}

	@Transactional(readOnly = true)
	public ProfileResponse get(Long userId) {
		return ProfileResponse.from(find(userId));
	}

	@Transactional
	public ProfileResponse update(Long userId, UpdateProfileRequest request) {
		User user = find(userId);
		String phone = blankToNull(request.phone());
		if (phone != null) {
			users.findByPhone(phone)
					.filter(other -> !other.getId().equals(userId))
					.ifPresent(other -> {
						throw new ResponseStatusException(HttpStatus.CONFLICT, "Этот телефон уже используется");
					});
		}
		user.setName(blankToNull(request.name()));
		user.setCity(blankToNull(request.city()));
		user.setPhone(phone);
		return ProfileResponse.from(users.save(user));
	}

	private User find(Long userId) {
		return users.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Пользователь не найден"));
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}
}
