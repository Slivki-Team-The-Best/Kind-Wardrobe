package ru.urfu.backend.point;

import java.math.BigDecimal;
import java.util.Set;
import ru.urfu.backend.entity.ExchangePoint;
import ru.urfu.backend.entity.enums.ItemType;
import ru.urfu.backend.entity.enums.PointStatus;

/** Карточка пункта обмена организатора. */
public record PointResponse(
		Long id,
		String name,
		String address,
		String city,
		BigDecimal latitude,
		BigDecimal longitude,
		String workingHours,
		String phone,
		String description,
		String photoUrl,
		PointStatus status,
		Set<ItemType> acceptedTypes) {

	public static PointResponse from(ExchangePoint point) {
		return new PointResponse(point.getId(), point.getName(), point.getAddress(), point.getCity(),
				point.getLatitude(), point.getLongitude(), point.getWorkingHours(), point.getPhone(),
				point.getDescription(), point.getPhotoUrl(), point.getStatus(), Set.copyOf(point.getAcceptedTypes()));
	}
}
