package ru.urfu.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import ru.urfu.backend.ApiTestSupport;
import ru.urfu.backend.point.Geocoder;

class RegistrationApiTest extends ApiTestSupport {

	@Test
	void userRegistersAndIsLoggedInRightAway() throws Exception {
		String token = registerUser("Anna@Example.com", "8 (916) 555-12-34");

		mvc.perform(get("/api/users/me").header("Authorization", bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email", is("anna@example.com")))
				.andExpect(jsonPath("$.phone", is("8 (916) 555-12-34")))
				.andExpect(jsonPath("$.role", is("USER")))
				.andExpect(jsonPath("$.name", is("Анна")))
				.andExpect(jsonPath("$.city", is("Москва")));

		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"identifier\":\"anna@example.com\",\"password\":\"secret-123\"}"))
				.andExpect(status().isOk());
	}

	@Test
	void organizerRegistrationCreatesPointOnMap() throws Exception {
		when(geocoder.geocode(anyString())).thenReturn(Optional.of(
				new Geocoder.GeocodingResult(new BigDecimal("55.758500"), new BigDecimal("37.648300"), "Москва")));

		String response = register("""
				{"role":"ORGANIZER","email":"org@example.com","phone":"+7 (495) 123-45-67","password":"secret-123",
				 "passwordConfirm":"secret-123","termsAccepted":true,"pointName":"Тёплый круг",
				 "pointAddress":"Москва, ул. Покровка, 27","workingHours":"Пн–Сб 10:00–20:00",
				 "pointPhone":"+7 (495) 123-45-67","pointDescription":"Соседский пункт обмена"}
				""")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.role", is("ORGANIZER")))
				.andReturn().getResponse().getContentAsString();
		String token = JsonPath.read(response, "$.accessToken");

		mvc.perform(get("/api/users/me").header("Authorization", bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.city", is("Москва")));
		mvc.perform(get("/api/organizer/point").header("Authorization", bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", is("Тёплый круг")))
				.andExpect(jsonPath("$.city", is("Москва")))
				.andExpect(jsonPath("$.status", is("ACTIVE")))
				.andExpect(jsonPath("$.latitude", is(55.7585)))
				.andExpect(jsonPath("$.id", notNullValue()));
	}

	@Test
	void organizerCityFallsBackToAddressWhenGeocoderHasNone() throws Exception {
		when(geocoder.geocode(anyString())).thenReturn(Optional.of(
				new Geocoder.GeocodingResult(new BigDecimal("56.838900"), new BigDecimal("60.604500"), null)));

		String response = register("""
				{"role":"ORGANIZER","email":"org3@example.com","phone":"+79990000003","password":"secret-123",
				 "passwordConfirm":"secret-123","termsAccepted":true,"pointName":"Детский шкаф",
				 "pointAddress":"Екатеринбург, ул. Вайнера, 10","workingHours":"10-18","pointPhone":"+79990000003",
				 "pointDescription":"Детские вещи"}
				""")
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();

		mvc.perform(get("/api/organizer/point").header("Authorization", bearer(JsonPath.read(response, "$.accessToken"))))
				.andExpect(jsonPath("$.city", is("Екатеринбург")));
	}

	@Test
	void organizerWithUnknownAddressIsRejectedAndNotSaved() throws Exception {
		when(geocoder.geocode(anyString())).thenReturn(Optional.empty());

		register("""
				{"role":"ORGANIZER","email":"org2@example.com","phone":"+79990000001","password":"secret-123",
				 "passwordConfirm":"secret-123","termsAccepted":true,"pointName":"Пункт",
				 "pointAddress":"нет такого адреса","workingHours":"10-18","pointPhone":"+79990000001",
				 "pointDescription":"Описание"}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.pointAddress", is("Не удалось найти адрес на карте")));
		assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE email = 'org2@example.com'", Integer.class))
				.isZero();
	}

	@Test
	void duplicateEmailIsConflict() throws Exception {
		registerUser("anna@example.com", "+79990000001");

		register("""
				{"role":"USER","email":"ANNA@example.com","phone":"+79990000002","password":"secret-123",
				 "passwordConfirm":"secret-123","termsAccepted":true,"name":"Анна","city":"Москва"}
				""")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errors.email", is("Email уже зарегистрирован")));
	}

	@Test
	void duplicatePhoneIsConflict() throws Exception {
		registerUser("anna@example.com", "+79990000001");

		register("""
				{"role":"USER","email":"other@example.com","phone":"+79990000001","password":"secret-123",
				 "passwordConfirm":"secret-123","termsAccepted":true,"name":"Анна","city":"Москва"}
				""")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errors.phone", is("Телефон уже зарегистрирован")));
	}

	@Test
	void invalidFormReturnsFieldErrors() throws Exception {
		register("""
				{"role":"USER","email":"not-an-email","phone":"123","password":"short",
				 "passwordConfirm":"other","termsAccepted":false}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.email", is("Некорректный email")))
				.andExpect(jsonPath("$.errors.phone", is("Некорректный номер телефона")))
				.andExpect(jsonPath("$.errors.password", is("Пароль должен быть не короче 8 символов")))
				.andExpect(jsonPath("$.errors.termsAccepted", is("Нужно согласие с условиями использования")));

		register("""
				{"role":"USER","email":"anna@example.com","phone":"+79990000005","password":"secret-123",
				 "passwordConfirm":"secret-124","termsAccepted":true}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.passwordConfirm", is("Пароли не совпадают")))
				.andExpect(jsonPath("$.errors.name", is("Введите имя")))
				.andExpect(jsonPath("$.errors.city", is("Выберите город")));
	}

	private ResultActions register(String json) throws Exception {
		return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json));
	}
}
