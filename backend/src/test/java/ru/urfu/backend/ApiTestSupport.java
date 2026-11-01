package ru.urfu.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import ru.urfu.backend.point.Geocoder;

@AutoConfigureMockMvc
@TestPropertySource(properties = "app.storage.dir=target/test-uploads")
public abstract class ApiTestSupport extends PostgresIntegrationTest {

	protected static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F'};
	protected static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};

	@Autowired
	protected MockMvc mvc;

	@Autowired
	protected JdbcTemplate jdbc;

	@MockitoBean
	protected Geocoder geocoder;

	@BeforeEach
	@AfterEach
	void cleanUpUsers() {
		jdbc.update("DELETE FROM exchange_points WHERE organizer_id IS NOT NULL");
		jdbc.update("DELETE FROM users");
	}

	protected String registerUser(String email, String phone) throws Exception {
		String body = """
				{"role":"USER","email":"%s","phone":"%s","password":"secret-123","passwordConfirm":"secret-123",
				 "termsAccepted":true,"name":"Анна","city":"Москва"}
				""".formatted(email, phone);
		String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.accessToken");
	}

	protected static String bearer(String token) {
		return "Bearer " + token;
	}

	protected static MockMultipartFile jpeg(String name) {
		return new MockMultipartFile("photos", name, "image/jpeg", JPEG);
	}

	protected static MockMultipartFile listingPart(String json) {
		return new MockMultipartFile("listing", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes(StandardCharsets.UTF_8));
	}

	protected static String listingJson(String status, String telegram) {
		return """
				{"title":"Куртка-рубашка","itemType":"CLOTHES","category":"ADULT","condition":"GOOD",
				 "description":"Носила один сезон","exchangeFormat":"FREE","weightKg":0.8,"city":"Москва",
				 "telegramUrl":%s,"status":"%s"}
				""".formatted(telegram == null ? "null" : "\"" + telegram + "\"", status);
	}

	protected MockMultipartHttpServletRequestBuilder createListing(String token, String json, MockMultipartFile... photos) {
		MockMultipartHttpServletRequestBuilder request = multipart("/api/listings").file(listingPart(json));
		for (MockMultipartFile photo : photos) {
			request.file(photo);
		}
		request.header("Authorization", bearer(token));
		return request;
	}

	protected Long createdListingId(String token, String json, MockMultipartFile... photos) throws Exception {
		String response = mvc.perform(createListing(token, json, photos))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(response, "$.id")).longValue();
	}
}
