package ru.urfu.backend.point;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Геокодер на OpenStreetMap Nominatim (бесплатный, без ключа — как и карта Leaflet + OSM).
 * Политика сервиса: обязательный User-Agent и не чаще 1 запроса в секунду; здесь запрос идёт
 * только при смене адреса пункта.
 */
@Component
public class NominatimGeocoder implements Geocoder {

	private static final Logger log = LoggerFactory.getLogger(NominatimGeocoder.class);
	private static final List<String> CITY_KEYS = List.of("city", "town", "village", "municipality");

	private final RestClient client;

	public NominatimGeocoder(@Value("${app.geocoder.url:https://nominatim.openstreetmap.org}") String baseUrl) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(5));
		factory.setReadTimeout(Duration.ofSeconds(5));
		this.client = RestClient.builder()
				.baseUrl(baseUrl)
				.requestFactory(factory)
				.defaultHeader("User-Agent", "kind-wardrobe/1.0 (student project)")
				.build();
	}

	@Override
	public Optional<GeocodingResult> geocode(String address) {
		try {
			List<Place> places = client.get()
					.uri(uri -> uri.path("/search")
							.queryParam("q", address)
							.queryParam("format", "jsonv2")
							.queryParam("addressdetails", 1)
							.queryParam("limit", 1)
							.build())
					.retrieve()
					.body(new ParameterizedTypeReference<List<Place>>() {
					});
			if (places == null || places.isEmpty()) {
				return Optional.empty();
			}
			Place place = places.get(0);
			return Optional.of(new GeocodingResult(round(place.lat()), round(place.lon()), cityOf(place.address())));
		}
		catch (RuntimeException e) {
			// сервис недоступен или ответ не разобран — для пользователя это «адрес не найден», детали в лог
			log.warn("Геокодирование адреса не удалось: {}", e.toString());
			return Optional.empty();
		}
	}

	private static BigDecimal round(String value) {
		return new BigDecimal(value).setScale(6, RoundingMode.HALF_UP);
	}

	private static String cityOf(Map<String, String> address) {
		if (address == null) {
			return null;
		}
		return CITY_KEYS.stream().map(address::get).filter(v -> v != null && !v.isBlank()).findFirst().orElse(null);
	}

	record Place(String lat, String lon, Map<String, String> address) {
	}
}
