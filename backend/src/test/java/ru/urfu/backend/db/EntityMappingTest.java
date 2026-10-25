package ru.urfu.backend.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.urfu.backend.PostgresIntegrationTest;
import ru.urfu.backend.entity.ExchangePoint;
import ru.urfu.backend.entity.Listing;
import ru.urfu.backend.entity.ListingPhoto;
import ru.urfu.backend.entity.User;
import ru.urfu.backend.entity.enums.ExchangeFormat;
import ru.urfu.backend.entity.enums.ItemCategory;
import ru.urfu.backend.entity.enums.ItemCondition;
import ru.urfu.backend.entity.enums.ItemType;
import ru.urfu.backend.entity.enums.ListingStatus;
import ru.urfu.backend.entity.enums.PointStatus;
import ru.urfu.backend.entity.enums.UserRole;
import ru.urfu.backend.repository.ExchangePointRepository;
import ru.urfu.backend.repository.ListingRepository;
import ru.urfu.backend.repository.UserRepository;

/** JPA-сущности и репозитории работают с таблицами из миграций (реальный SQL, а не только validate). */
@Transactional
class EntityMappingTest extends PostgresIntegrationTest {

	@Autowired UserRepository users;
	@Autowired ListingRepository listings;
	@Autowired ExchangePointRepository points;

	@Test
	void seededPointsAreReadableWithAcceptedTypes() {
		List<ExchangePoint> active = points.findByStatus(PointStatus.ACTIVE);

		assertThat(active).hasSizeGreaterThanOrEqualTo(10);
		assertThat(active).allSatisfy(p -> assertThat(p.getAcceptedTypes()).isNotEmpty());
		assertThat(points.findByStatusAndAcceptedTypesIn(PointStatus.ACTIVE, List.of(ItemType.FURNITURE)))
				.isNotEmpty()
				.allSatisfy(p -> assertThat(p.getAcceptedTypes()).contains(ItemType.FURNITURE));
	}

	@Test
	void listingWithPhotosIsSavedAndLoaded() {
		User author = new User();
		author.setEmail("author@example.com");
		author.setPasswordHash("x");
		author.setRole(UserRole.USER);
		author.setTermsAcceptedAt(OffsetDateTime.now());
		users.saveAndFlush(author);

		Listing listing = new Listing();
		listing.setAuthor(author);
		listing.setTitle("Куртка");
		listing.setItemType(ItemType.CLOTHES);
		listing.setCategory(ItemCategory.ADULT);
		listing.setCondition(ItemCondition.GOOD);
		listing.setExchangeFormat(ExchangeFormat.FREE);
		listing.setWeightKg(new BigDecimal("1.50"));
		listing.setCity("Екатеринбург");
		listing.setTelegramUrl("https://t.me/example");
		ListingPhoto photo = new ListingPhoto();
		photo.setListing(listing);
		photo.setUrl("a.jpg");
		photo.setPosition((short) 0);
		listing.getPhotos().add(photo);

		Listing saved = listings.saveAndFlush(listing);

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getStatus()).isEqualTo(ListingStatus.ACTIVE);
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getPhotos()).extracting(ListingPhoto::getId).doesNotContainNull();
	}
}
