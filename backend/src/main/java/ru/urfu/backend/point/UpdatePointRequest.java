package ru.urfu.backend.point;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import ru.urfu.backend.entity.enums.ItemType;

/** Форма редактирования данных пункта (профиль организатора). */
public record UpdatePointRequest(
		@NotBlank @Size(max = 150) String name,
		@NotBlank @Size(max = 255) String address,
		@NotBlank @Size(max = 255) String workingHours,
		@NotBlank @Size(max = 20) @Pattern(regexp = "^\\+?[0-9][0-9\\s().-]{8,19}$", message = "Некорректный номер телефона") String phone,
		@Size(max = 2000) String description,
		@NotEmpty Set<ItemType> acceptedTypes) {
}
