package ru.urfu.backend.point;

import java.math.BigDecimal;
import java.util.Optional;

/** Превращает адрес в координаты для карты. */
public interface Geocoder {

	Optional<GeocodingResult> geocode(String address);

	/** city может быть null, если сервис геокодирования не определил населённый пункт. */
	record GeocodingResult(BigDecimal latitude, BigDecimal longitude, String city) {
	}
}
