package ru.urfu.backend.listing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import ru.urfu.backend.ApiTestSupport;
import ru.urfu.backend.TestUsers;
import ru.urfu.backend.config.JwtService;
import ru.urfu.backend.entity.User;
import ru.urfu.backend.entity.enums.UserRole;
import ru.urfu.backend.repository.UserRepository;

class ListingApiTest extends ApiTestSupport {

	@Autowired
	private UserRepository users;

	@Autowired
	private JwtService jwtService;

	private String anna;
	private String boris;

	@BeforeEach
	void users() throws Exception {
		anna = registerUser("anna@example.com", "+79990000001");
		boris = registerUser("boris@example.com", "+79990000002");
	}

	@Test
	void publishesListingWithPhotosThatAreServed() throws Exception {
		String response = mvc.perform(createListing(anna, listingJson("ACTIVE", "@anna_smirnova"), jpeg("a.jpg"),
						new MockMultipartFile("photos", "b.png", "image/png", PNG)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status", is("ACTIVE")))
				.andExpect(jsonPath("$.publishedAt", notNullValue()))
				.andExpect(jsonPath("$.telegramUrl", is("https://t.me/anna_smirnova")))
				.andExpect(jsonPath("$.weightKg", is(0.8)))
				.andExpect(jsonPath("$.author.name", is("Анна")))
				.andExpect(jsonPath("$.photos", hasSize(2)))
				.andExpect(jsonPath("$.photos[0].position", is(0)))
				.andExpect(jsonPath("$.photos[1].position", is(1)))
				.andReturn().getResponse().getContentAsString();

		String url = JsonPath.read(response, "$.photos[1].url");
		assertThat(url).startsWith("/uploads/listings/").endsWith(".png");
		mvc.perform(get(url)).andExpect(status().isOk());
	}

	@Test
	void organizerCannotCreateListings() throws Exception {
		User organizer = TestUsers.save(users, "org@example.com", "+79990000009", UserRole.ORGANIZER);

		mvc.perform(createListing(TestUsers.bearer(jwtService, organizer).substring("Bearer ".length()),
						listingJson("ACTIVE", "t.me/org"), jpeg("a.jpg")))
				.andExpect(status().isForbidden());
		mvc.perform(createListing(TestUsers.bearer(jwtService, organizer).substring("Bearer ".length()),
						"{\"title\":\"\"}"))
				.andExpect(status().isForbidden());
		assertThat(jdbc.queryForObject("SELECT count(*) FROM listings", Integer.class)).isZero();
	}

	@Test
	void publishingRequiresPhotoAndMessenger() throws Exception {
		mvc.perform(createListing(anna, listingJson("ACTIVE", null)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.photos", is("Загрузите хотя бы одно фото")))
				.andExpect(jsonPath("$.errors.contacts", is("Укажите ссылку на Telegram, WhatsApp или MAX")));
	}

	@Test
	void rejectsNonImageFiles() throws Exception {
		mvc.perform(createListing(anna, listingJson("ACTIVE", "t.me/anna"),
						new MockMultipartFile("photos", "evil.jpg", "image/jpeg", "<script>".getBytes())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.photos", is("Можно загружать только JPG или PNG")));
		assertThat(jdbc.queryForObject("SELECT count(*) FROM listings", Integer.class)).isZero();
	}

	@Test
	void validatesFields() throws Exception {
		mvc.perform(createListing(anna, """
						{"title":"","itemType":null,"weightKg":0,"city":"Москва","exchangeFormat":"FREE",
						 "category":"ADULT","condition":"GOOD","description":"x","status":"DRAFT"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.title", is("Введите название вещи")))
				.andExpect(jsonPath("$.errors.itemType", is("Выберите тип вещи")))
				.andExpect(jsonPath("$.errors.weightKg", is("Вес должен быть больше 0")));
	}

	@Test
	void draftIsVisibleOnlyToAuthorAndCanBePublishedLater() throws Exception {
		Long id = createdListingId(anna, listingJson("DRAFT", null));

		mvc.perform(get("/api/listings/" + id)).andExpect(status().isNotFound());
		mvc.perform(get("/api/listings/" + id).header("Authorization", bearer(boris))).andExpect(status().isNotFound());
		mvc.perform(get("/api/listings/" + id).header("Authorization", bearer(anna)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is("DRAFT")))
				.andExpect(jsonPath("$.publishedAt", nullValue()));

		mvc.perform(update(anna, id, listingJson("ACTIVE", "t.me/anna")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.photos", is("Загрузите хотя бы одно фото")));

		mvc.perform(multipart("/api/listings/" + id + "/photos").file(jpeg("a.jpg")).header("Authorization", bearer(anna)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.photos", hasSize(1)));
		mvc.perform(update(anna, id, listingJson("ACTIVE", "t.me/anna")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is("ACTIVE")))
				.andExpect(jsonPath("$.publishedAt", notNullValue()));
		mvc.perform(get("/api/listings/" + id)).andExpect(status().isOk());
	}

	@Test
	void onlyAuthorCanEditOrDelete() throws Exception {
		Long id = createdListingId(anna, listingJson("ACTIVE", "t.me/anna"), jpeg("a.jpg"));

		mvc.perform(update(boris, id, listingJson("ACTIVE", "t.me/boris"))).andExpect(status().isForbidden());
		mvc.perform(delete("/api/listings/" + id).header("Authorization", bearer(boris))).andExpect(status().isForbidden());
		mvc.perform(update(anna, id, listingJson("HIDDEN", "t.me/anna")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is("HIDDEN")));
		mvc.perform(update(anna, id, listingJson("TRANSFERRED", "t.me/anna"))).andExpect(status().isBadRequest());
	}

	@Test
	void editingListingWithoutTokenIsUnauthorized() throws Exception {
		Long id = createdListingId(anna, listingJson("ACTIVE", "t.me/anna"), jpeg("a.jpg"));

		mvc.perform(put("/api/listings/" + id).contentType(MediaType.APPLICATION_JSON).content(listingJson("ACTIVE", "t.me/x")))
				.andExpect(status().isUnauthorized());
		mvc.perform(get("/api/listings/my")).andExpect(status().isUnauthorized());
	}

	@Test
	void photosAreLimitedToFiveAndRepackedAfterDelete() throws Exception {
		Long id = createdListingId(anna, listingJson("ACTIVE", "t.me/anna"),
				jpeg("1.jpg"), jpeg("2.jpg"), jpeg("3.jpg"));

		mvc.perform(multipart("/api/listings/" + id + "/photos").file(jpeg("4.jpg")).file(jpeg("5.jpg")).file(jpeg("6.jpg"))
						.header("Authorization", bearer(anna)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.photos", is("Можно загрузить не больше 5 фото")));

		String response = mvc.perform(get("/api/listings/" + id)).andReturn().getResponse().getContentAsString();
		Number firstPhotoId = JsonPath.read(response, "$.photos[0].id");
		String firstUrl = JsonPath.read(response, "$.photos[0].url");

		mvc.perform(delete("/api/listings/" + id + "/photos/" + firstPhotoId).header("Authorization", bearer(anna)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.photos", hasSize(2)))
				.andExpect(jsonPath("$.photos[0].position", is(0)))
				.andExpect(jsonPath("$.photos[1].position", is(1)));
		assertThat(storedFile(firstUrl)).doesNotExist();
	}

	@Test
	void lastPhotoOfPublishedListingCannotBeDeleted() throws Exception {
		Long id = createdListingId(anna, listingJson("ACTIVE", "t.me/anna"), jpeg("1.jpg"));
		String response = mvc.perform(get("/api/listings/" + id)).andReturn().getResponse().getContentAsString();
		Number photoId = JsonPath.read(response, "$.photos[0].id");

		mvc.perform(delete("/api/listings/" + id + "/photos/" + photoId).header("Authorization", bearer(anna)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void deletingListingRemovesItsPhotos() throws Exception {
		String response = mvc.perform(createListing(anna, listingJson("ACTIVE", "t.me/anna"), jpeg("1.jpg")))
				.andReturn().getResponse().getContentAsString();
		Number id = JsonPath.read(response, "$.id");
		String url = JsonPath.read(response, "$.photos[0].url");
		assertThat(storedFile(url)).exists();

		mvc.perform(delete("/api/listings/" + id).header("Authorization", bearer(anna))).andExpect(status().isNoContent());

		mvc.perform(get("/api/listings/" + id)).andExpect(status().isNotFound());
		assertThat(storedFile(url)).doesNotExist();
	}

	@Test
	void myListingsReturnOwnListingsWithCover() throws Exception {
		createdListingId(anna, listingJson("ACTIVE", "t.me/anna"), jpeg("1.jpg"));
		createdListingId(anna, listingJson("DRAFT", null));
		createdListingId(boris, listingJson("ACTIVE", "t.me/boris"), jpeg("1.jpg"));

		mvc.perform(get("/api/listings/my").header("Authorization", bearer(anna)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));
		mvc.perform(get("/api/listings/my").param("status", "ACTIVE").header("Authorization", bearer(anna)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].coverUrl", notNullValue()));
		mvc.perform(get("/api/users/me/stats").header("Authorization", bearer(anna)))
				.andExpect(jsonPath("$.activeListingsCount", is(1)))
				.andExpect(jsonPath("$.listingsCount", is(2)));
	}

	private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder update(String token, Long id,
			String json) {
		return put("/api/listings/" + id)
				.header("Authorization", bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json);
	}

	private static Path storedFile(String url) {
		return Path.of("target/test-uploads").resolve(url.substring("/uploads/".length()));
	}
}
