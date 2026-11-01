package ru.urfu.backend.listing;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import ru.urfu.backend.entity.Listing;
import ru.urfu.backend.entity.ListingPhoto;
import ru.urfu.backend.entity.User;
import ru.urfu.backend.entity.enums.ExchangeFormat;
import ru.urfu.backend.entity.enums.ItemCategory;
import ru.urfu.backend.entity.enums.ItemCondition;
import ru.urfu.backend.entity.enums.ItemType;
import ru.urfu.backend.entity.enums.ListingStatus;

public record ListingResponse(
		Long id,
		String title,
		ItemType itemType,
		ItemCategory category,
		ItemCondition condition,
		String description,
		ExchangeFormat exchangeFormat,
		BigDecimal weightKg,
		String city,
		String telegramUrl,
		String whatsappUrl,
		String maxUrl,
		ListingStatus status,
		OffsetDateTime publishedAt,
		OffsetDateTime transferredAt,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt,
		List<Photo> photos,
		Author author) {

	public static ListingResponse from(Listing listing) {
		User author = listing.getAuthor();
		return new ListingResponse(
				listing.getId(),
				listing.getTitle(),
				listing.getItemType(),
				listing.getCategory(),
				listing.getCondition(),
				listing.getDescription(),
				listing.getExchangeFormat(),
				listing.getWeightKg(),
				listing.getCity(),
				listing.getTelegramUrl(),
				listing.getWhatsappUrl(),
				listing.getMaxUrl(),
				listing.getStatus(),
				listing.getPublishedAt(),
				listing.getTransferredAt(),
				listing.getCreatedAt(),
				listing.getUpdatedAt(),
				listing.getPhotos().stream().map(Photo::from).toList(),
				new Author(author.getId(), author.getName(), author.getAvatarUrl(), author.getCreatedAt()));
	}

	public record Photo(Long id, String url, Short position) {

		static Photo from(ListingPhoto photo) {
			return new Photo(photo.getId(), photo.getUrl(), photo.getPosition());
		}
	}

	public record Author(Long id, String name, String avatarUrl, OffsetDateTime memberSince) {
	}
}
