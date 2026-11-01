package ru.urfu.backend.listing;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import ru.urfu.backend.entity.enums.ExchangeFormat;
import ru.urfu.backend.entity.enums.ItemCategory;
import ru.urfu.backend.entity.enums.ItemCondition;
import ru.urfu.backend.entity.enums.ItemType;
import ru.urfu.backend.entity.enums.ListingStatus;

public record ListingRequest(
		@NotBlank(message = "Введите название вещи") @Size(max = 100, message = "Не длиннее 100 символов") String title,
		@NotNull(message = "Выберите тип вещи") ItemType itemType,
		@NotNull(message = "Выберите категорию") ItemCategory category,
		@NotNull(message = "Выберите состояние") ItemCondition condition,
		@NotBlank(message = "Добавьте описание") @Size(max = 500, message = "Не длиннее 500 символов") String description,
		@NotNull(message = "Выберите формат обмена") ExchangeFormat exchangeFormat,
		@NotNull(message = "Укажите вес")
		@DecimalMin(value = "0.01", message = "Вес должен быть больше 0")
		@DecimalMax(value = "9999.99", message = "Слишком большой вес")
		@Digits(integer = 4, fraction = 2, message = "Не больше двух знаков после запятой") BigDecimal weightKg,
		@NotBlank(message = "Выберите город") @Size(max = 100, message = "Не длиннее 100 символов") String city,
		@Size(max = 255, message = "Слишком длинная ссылка") String telegramUrl,
		@Size(max = 255, message = "Слишком длинная ссылка") String whatsappUrl,
		@Size(max = 255, message = "Слишком длинная ссылка") String maxUrl,
		ListingStatus status) {
}
