package ru.urfu.backend.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ru.urfu.backend.entity.enums.UserRole;

public record RegisterRequest(
		@NotNull(message = "Выберите роль") UserRole role,
		@NotBlank(message = "Введите email") @Email(message = "Некорректный email") @Size(max = 255) String email,
		@NotBlank(message = "Введите телефон") @Size(max = 20, message = "Не длиннее 20 символов")
		@Pattern(regexp = "^\\+?[0-9][0-9\\s().-]{8,19}$", message = "Некорректный номер телефона") String phone,
		@NotBlank(message = "Введите пароль") @Size(min = 8, max = 128, message = "Пароль должен быть не короче 8 символов") String password,
		@NotBlank(message = "Повторите пароль") String passwordConfirm,
		@AssertTrue(message = "Нужно согласие с условиями использования") boolean termsAccepted,
		@Size(max = 100, message = "Не длиннее 100 символов") String name,
		@Size(max = 100, message = "Не длиннее 100 символов") String city,
		@Size(max = 150, message = "Не длиннее 150 символов") String pointName,
		@Size(max = 255, message = "Не длиннее 255 символов") String pointAddress,
		@Size(max = 255, message = "Не длиннее 255 символов") String workingHours,
		@Size(max = 20, message = "Не длиннее 20 символов") String pointPhone,
		@Size(max = 2000, message = "Не длиннее 2000 символов") String pointDescription) {
}
