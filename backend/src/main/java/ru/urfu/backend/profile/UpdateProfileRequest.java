package ru.urfu.backend.profile;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Редактируемые поля профиля. Пустое значение очищает поле. Email и роль через профиль не меняются. */
public record UpdateProfileRequest(
		@Size(max = 100) String name,
		@Size(max = 100) String city,
		@Size(max = 20) @Pattern(regexp = "^$|^\\+?[0-9][0-9\\s().-]{8,19}$", message = "Некорректный номер телефона") String phone) {
}
