package ru.urfu.backend.point;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import ru.urfu.backend.PostgresIntegrationTest;
import ru.urfu.backend.TestUsers;
import ru.urfu.backend.config.JwtService;
import ru.urfu.backend.entity.User;
import ru.urfu.backend.entity.enums.UserRole;
import ru.urfu.backend.repository.UserRepository;

@AutoConfigureMockMvc
@Transactional
class OrganizerPointApiTest extends PostgresIntegrationTest {

	private static final String VALID_POINT = """
			{"name":"Тёплый круг","address":"Москва, ул. Покровка, 27","workingHours":"Пн–Сб 10:00–20:00",
			 "phone":"+7 (495) 123-45-67","description":"Соседский пункт обмена вещами",
			 "acceptedTypes":["CLOTHES","SHOES","BOOKS"]}""";

	@Autowired MockMvc mvc;
	@Autowired UserRepository users;
	@Autowired JwtService jwtService;
	@MockitoBean Geocoder geocoder;

	User organizer;

	@BeforeEach
	void setUp() {
		organizer = TestUsers.save(users, "org@example.com", "+79002220001", UserRole.ORGANIZER);
		when(geocoder.geocode(anyString())).thenReturn(Optional.of(
				new Geocoder.GeocodingResult(new BigDecimal("55.757000"), new BigDecimal("37.649000"), "Москва")));
	}

	@Test
	void userRoleGetsForbiddenOnOrganizerEndpoints() throws Exception {
		User user = TestUsers.save(users, "plain@example.com", "+79002220002", UserRole.USER);

		mvc.perform(get("/api/organizer/point").header("Authorization", TestUsers.bearer(jwtService, user)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.detail", is("Недостаточно прав")));
		mvc.perform(save(user, VALID_POINT)).andExpect(status().isForbidden());
		verify(geocoder, never()).geocode(anyString());
	}

	@Test
	void requiresAuthentication() throws Exception {
		mvc.perform(get("/api/organizer/point")).andExpect(status().isUnauthorized());
	}

	@Test
	void organizerWithoutPointGets404() throws Exception {
		mvc.perform(get("/api/organizer/point").header("Authorization", TestUsers.bearer(jwtService, organizer)))
				.andExpect(status().isNotFound());
	}

	@Test
	void createsPointWithCoordinatesFromGeocoder() throws Exception {
		mvc.perform(save(organizer, VALID_POINT))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", is("Тёплый круг")))
				.andExpect(jsonPath("$.city", is("Москва")))
				.andExpect(jsonPath("$.latitude", is(55.757)))
				.andExpect(jsonPath("$.longitude", is(37.649)))
				.andExpect(jsonPath("$.status", is("ACTIVE")))
				.andExpect(jsonPath("$.acceptedTypes", containsInAnyOrder("CLOTHES", "SHOES", "BOOKS")));

		mvc.perform(get("/api/organizer/point").header("Authorization", TestUsers.bearer(jwtService, organizer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.workingHours", is("Пн–Сб 10:00–20:00")));
	}

	@Test
	void secondSaveUpdatesSamePointAndGeocodesOnlyWhenAddressChanges() throws Exception {
		mvc.perform(save(organizer, VALID_POINT)).andExpect(status().isOk());

		mvc.perform(save(organizer, VALID_POINT.replace("Пн–Сб 10:00–20:00", "Ежедневно 9:00–21:00")
						.replace("[\"CLOTHES\",\"SHOES\",\"BOOKS\"]", "[\"TOYS\"]")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.workingHours", is("Ежедневно 9:00–21:00")))
				.andExpect(jsonPath("$.acceptedTypes", containsInAnyOrder("TOYS")));
		verify(geocoder, times(1)).geocode(anyString());

		mvc.perform(save(organizer, VALID_POINT.replace("ул. Покровка, 27", "ул. Маросейка, 8")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.address", is("Москва, ул. Маросейка, 8")));
		verify(geocoder, times(2)).geocode(anyString());
	}

	@Test
	void unknownAddressIsRejected() throws Exception {
		when(geocoder.geocode(anyString())).thenReturn(Optional.empty());

		mvc.perform(save(organizer, VALID_POINT))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", is("Не удалось определить адрес. Укажите город, улицу и дом")));
	}

	@Test
	void requiredFieldsAreValidated() throws Exception {
		mvc.perform(save(organizer, VALID_POINT.replace("\"name\":\"Тёплый круг\"", "\"name\":\" \"")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", is("Проверьте заполнение полей")))
				.andExpect(jsonPath("$.errors.name", is("Обязательное поле")));
		mvc.perform(save(organizer, VALID_POINT.replace("[\"CLOTHES\",\"SHOES\",\"BOOKS\"]", "[]")))
				.andExpect(status().isBadRequest());
		mvc.perform(save(organizer, VALID_POINT.replace("+7 (495) 123-45-67", "123")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.phone", is("Некорректный номер телефона")));
		mvc.perform(save(organizer, VALID_POINT.replace("\"CLOTHES\"", "\"CARS\"")))
				.andExpect(status().isBadRequest());
	}

	private MockHttpServletRequestBuilder save(User user, String json) {
		return put("/api/organizer/point")
				.header("Authorization", TestUsers.bearer(jwtService, user))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json);
	}
}
