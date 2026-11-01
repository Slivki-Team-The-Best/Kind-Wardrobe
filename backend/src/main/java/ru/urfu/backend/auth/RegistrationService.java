package ru.urfu.backend.auth;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urfu.backend.common.ApiException;
import ru.urfu.backend.config.JwtService;
import ru.urfu.backend.entity.ExchangePoint;
import ru.urfu.backend.entity.User;
import ru.urfu.backend.entity.enums.UserRole;
import ru.urfu.backend.point.Geocoder;
import ru.urfu.backend.repository.ExchangePointRepository;
import ru.urfu.backend.repository.UserRepository;

@Service
public class RegistrationService {

	private final UserRepository users;
	private final ExchangePointRepository points;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final Geocoder geocoder;

	public RegistrationService(UserRepository users, ExchangePointRepository points, PasswordEncoder passwordEncoder,
			JwtService jwtService, Geocoder geocoder) {
		this.users = users;
		this.points = points;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.geocoder = geocoder;
	}

	@Transactional
	public TokenResponse register(RegisterRequest request) {
		Map<String, String> errors = new LinkedHashMap<>();
		if (!request.password().equals(request.passwordConfirm())) {
			errors.put("passwordConfirm", "Пароли не совпадают");
		}
		if (request.role() == UserRole.USER) {
			requireText(errors, "name", request.name(), "Введите имя");
			requireText(errors, "city", request.city(), "Выберите город");
		} else {
			requireText(errors, "pointName", request.pointName(), "Введите название пункта");
			requireText(errors, "pointAddress", request.pointAddress(), "Введите адрес пункта");
			requireText(errors, "workingHours", request.workingHours(), "Укажите часы работы");
			requireText(errors, "pointPhone", request.pointPhone(), "Укажите контактный телефон");
			requireText(errors, "pointDescription", request.pointDescription(), "Расскажите о пункте");
		}
		if (!errors.isEmpty()) {
			throw ApiException.badRequest(errors);
		}

		String email = request.email().strip().toLowerCase(Locale.ROOT);
		String phone = request.phone().strip();
		if (users.existsByEmail(email)) {
			throw ApiException.conflict("email", "Email уже зарегистрирован");
		}
		if (users.existsByPhone(phone)) {
			throw ApiException.conflict("phone", "Телефон уже зарегистрирован");
		}

		Geocoder.GeocodingResult location = null;
		String pointCity = null;
		if (request.role() == UserRole.ORGANIZER) {
			String address = request.pointAddress().strip();
			location = geocoder.geocode(address)
					.orElseThrow(() -> ApiException.badRequest("pointAddress", "Не удалось найти адрес на карте"));
			pointCity = location.city() != null ? location.city() : cityFromAddress(address);
		}

		User user = new User();
		user.setEmail(email);
		user.setPhone(phone);
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setRole(request.role());
		user.setName(blankToNull(request.name()));
		user.setCity(request.role() == UserRole.USER ? request.city().strip() : pointCity);
		user.setTermsAcceptedAt(OffsetDateTime.now());
		users.save(user);

		if (location != null) {
			ExchangePoint point = new ExchangePoint();
			point.setOrganizer(user);
			point.setName(request.pointName().strip());
			point.setAddress(request.pointAddress().strip());
			point.setCity(pointCity);
			point.setLatitude(location.latitude());
			point.setLongitude(location.longitude());
			point.setWorkingHours(request.workingHours().strip());
			point.setPhone(request.pointPhone().strip());
			point.setDescription(request.pointDescription().strip());
			points.save(point);
		}

		JwtService.IssuedToken token = jwtService.issueAccessToken(user);
		return new TokenResponse(token.value(), TokenResponse.BEARER, token.expiresInSeconds(), user.getId(), user.getRole());
	}

	private static void requireText(Map<String, String> errors, String field, String value, String message) {
		if (value == null || value.isBlank()) {
			errors.put(field, message);
		}
	}

	private static String cityFromAddress(String address) {
		int comma = address.indexOf(',');
		if (comma <= 0) {
			throw ApiException.badRequest("pointAddress", "Укажите город в адресе: «Город, улица, дом»");
		}
		return address.substring(0, comma).strip();
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}
}
