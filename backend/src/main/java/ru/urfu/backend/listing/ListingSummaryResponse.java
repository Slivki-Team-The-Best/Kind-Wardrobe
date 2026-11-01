package ru.urfu.backend.listing;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import ru.urfu.backend.entity.Listing;
import ru.urfu.backend.entity.ListingPhoto;
import ru.urfu.backend.entity.enums.ExchangeFormat;
import ru.urfu.backend.entity.enums.ItemType;
import ru.urfu.backend.entity.enums.ListingStatus;

public record ListingSummaryResponse(
		Long id,
		String title,
		ItemType itemType,
		ExchangeFormat exchangeFormat,
		BigDecimal weightKg,
		String city,
		ListingStatus status,
		String coverUrl,
		OffsetDateTime publishedAt,
		OffsetDateTime createdAt) {

	public static ListingSummaryResponse from(Listing listing) {
		String cover = listing.getPhotos().stream()
				.min(Comparator.comparing(ListingPhoto::getPosition))
				.map(ListingPhoto::getUrl)
				.orElse(null);
		return new ListingSummaryResponse(
				listing.getId(),
				listing.getTitle(),
				listing.getItemType(),
				listing.getExchangeFormat(),
				listing.getWeightKg(),
				listing.getCity(),
				listing.getStatus(),
				cover,
				listing.getPublishedAt(),
				listing.getCreatedAt());
	}
}
