package ru.urfu.backend.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.urfu.backend.PostgresIntegrationTest;

class MigrationsTest extends PostgresIntegrationTest {

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void createsAllFiveTables() {
		List<String> tables = jdbc.queryForList("""
				SELECT table_name FROM information_schema.tables
				WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history'
				""", String.class);
		assertThat(tables).containsExactlyInAnyOrder(
				"users", "listings", "listing_photos", "exchange_points", "point_item_types");
	}

	@Test
	void seedHasAtLeastTenPointsEachWithItemTypes() {
		Integer points = jdbc.queryForObject("SELECT count(*) FROM exchange_points", Integer.class);
		Integer pointsWithoutTypes = jdbc.queryForObject("""
				SELECT count(*) FROM exchange_points p
				WHERE NOT EXISTS (SELECT 1 FROM point_item_types t WHERE t.point_id = p.id)
				""", Integer.class);
		Integer withOrganizer = jdbc.queryForObject(
				"SELECT count(*) FROM exchange_points WHERE organizer_id IS NOT NULL", Integer.class);

		assertThat(points).isGreaterThanOrEqualTo(10);
		assertThat(pointsWithoutTypes).isZero();
		assertThat(withOrganizer).isZero();
	}

	@Test
	void rejectsUnknownUserRole() {
		assertThatThrownBy(() -> jdbc.update("""
				INSERT INTO users (email, password_hash, role, terms_accepted_at)
				VALUES ('bad-role@example.com', 'x', 'ADMIN', now())
				"""))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsActiveListingWithoutMessengerLinkButAllowsDraft() {
		Long authorId = jdbc.queryForObject("""
				INSERT INTO users (email, password_hash, role, terms_accepted_at)
				VALUES ('author@example.com', 'x', 'USER', now()) RETURNING id
				""", Long.class);
		String insert = """
				INSERT INTO listings (author_id, title, item_type, category, condition, exchange_format,
				                      weight_kg, city, status)
				VALUES (?, 'Куртка', 'CLOTHES', 'ADULT', 'GOOD', 'FREE', 1.5, 'Екатеринбург', ?)
				""";

		assertThatThrownBy(() -> jdbc.update(insert, authorId, "ACTIVE"))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThat(jdbc.update(insert, authorId, "DRAFT")).isEqualTo(1);
	}

	@Test
	void transferredAtMustMatchTransferredStatus() {
		Long authorId = jdbc.queryForObject("""
				INSERT INTO users (email, password_hash, role, terms_accepted_at)
				VALUES ('author2@example.com', 'x', 'USER', now()) RETURNING id
				""", Long.class);
		String insert = """
				INSERT INTO listings (author_id, title, item_type, category, condition, exchange_format,
				                      weight_kg, city, telegram_url, status, transferred_at)
				VALUES (?, 'Книги', 'BOOKS', 'ADULT', 'GOOD', 'SWAP', 2, 'Тверь', 'https://t.me/x', ?, ?::timestamptz)
				""";

		assertThatThrownBy(() -> jdbc.update(insert, authorId, "TRANSFERRED", null))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.update(insert, authorId, "ACTIVE", "2026-11-01T10:00:00Z"))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThat(jdbc.update(insert, authorId, "TRANSFERRED", "2026-11-01T10:00:00Z")).isEqualTo(1);
	}

	@Test
	void photoPositionIsLimitedToFiveUniqueSlots() {
		Long authorId = jdbc.queryForObject("""
				INSERT INTO users (email, password_hash, role, terms_accepted_at)
				VALUES ('author3@example.com', 'x', 'USER', now()) RETURNING id
				""", Long.class);
		Long listingId = jdbc.queryForObject("""
				INSERT INTO listings (author_id, title, item_type, category, condition, exchange_format,
				                      weight_kg, city, telegram_url)
				VALUES (?, 'Стул', 'FURNITURE', 'ADULT', 'NEEDS_REPAIR', 'FREE', 4, 'Пенза', 'https://t.me/x')
				RETURNING id
				""", Long.class, authorId);

		assertThat(jdbc.update("INSERT INTO listing_photos (listing_id, url, position) VALUES (?, 'a.jpg', 4)", listingId))
				.isEqualTo(1);
		assertThatThrownBy(() -> jdbc.update(
				"INSERT INTO listing_photos (listing_id, url, position) VALUES (?, 'b.jpg', 5)", listingId))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.update(
				"INSERT INTO listing_photos (listing_id, url, position) VALUES (?, 'c.jpg', 4)", listingId))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void deletingUserCascadesListingsAndPhotosButKeepsPoint() {
		Long userId = jdbc.queryForObject("""
				INSERT INTO users (email, password_hash, role, terms_accepted_at)
				VALUES ('org@example.com', 'x', 'ORGANIZER', now()) RETURNING id
				""", Long.class);
		Long listingId = jdbc.queryForObject("""
				INSERT INTO listings (author_id, title, item_type, category, condition, exchange_format,
				                      weight_kg, city, telegram_url)
				VALUES (?, 'Мишка', 'TOYS', 'CHILDREN', 'NEW', 'FREE', 0.3, 'Москва', 'https://t.me/x')
				RETURNING id
				""", Long.class, userId);
		jdbc.update("INSERT INTO listing_photos (listing_id, url, position) VALUES (?, 'a.jpg', 0)", listingId);
		Long pointId = jdbc.queryForObject("""
				INSERT INTO exchange_points (organizer_id, name, address, city, latitude, longitude,
				                             working_hours, phone)
				VALUES (?, 'Мой пункт', 'ул. Тестовая, 1', 'Москва', 55.75, 37.61, '10-18', '+70000000000')
				RETURNING id
				""", Long.class, userId);

		jdbc.update("DELETE FROM users WHERE id = ?", userId);

		assertThat(jdbc.queryForObject("SELECT count(*) FROM listings WHERE id = ?", Integer.class, listingId)).isZero();
		assertThat(jdbc.queryForObject("SELECT count(*) FROM listing_photos WHERE listing_id = ?", Integer.class, listingId))
				.isZero();
		assertThat(jdbc.queryForObject("SELECT organizer_id FROM exchange_points WHERE id = ?", Long.class, pointId))
				.isNull();
	}
}
