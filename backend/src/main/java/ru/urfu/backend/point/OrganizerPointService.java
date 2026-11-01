package ru.urfu.backend.point;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.urfu.backend.entity.ExchangePoint;
import ru.urfu.backend.entity.User;
import ru.urfu.backend.repository.ExchangePointRepository;
import ru.urfu.backend.repository.UserRepository;

/** Единственный пункт обмена организатора: просмотр и создание/редактирование. */
@Service
public class OrganizerPointService {

	private final ExchangePointRepository points;
	private final UserRepository users;
	private final Geocoder geocoder;

	public OrganizerPointService(ExchangePointRepository points, UserRepository users, Geocoder geocoder) {
		this.points = points;
		this.users = users;
		this.geocoder = geocoder;
	}

	@Transactional(readOnly = true)
	public PointResponse get(Long organizerId) {
		return points.findByOrganizerId(organizerId)
				.map(PointResponse::from)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пункт ещё не создан"));
	}

	/** Создаёт пункт, если его ещё нет, иначе обновляет. Координаты пересчитываются только при смене адреса. */
	@Transactional
	public PointResponse save(Long organizerId, UpdatePointRequest request) {
		User organizer = users.findById(organizerId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Пользователь не найден"));
		ExchangePoint point = points.findByOrganizerId(organizerId).orElseGet(() -> {
			ExchangePoint created = new ExchangePoint();
			created.setOrganizer(organizer);
			return created;
		});

		String address = request.address().strip();
		if (point.getId() == null || !address.equals(point.getAddress())) {
			locate(point, address, organizer);
		}
		point.setName(request.name().strip());
		point.setAddress(address);
		point.setWorkingHours(request.workingHours().strip());
		point.setPhone(request.phone().strip());
		point.setDescription(request.description() == null || request.description().isBlank()
				? null : request.description().strip());
		point.getAcceptedTypes().clear();
		point.getAcceptedTypes().addAll(request.acceptedTypes());
		return PointResponse.from(points.save(point));
	}

	private void locate(ExchangePoint point, String address, User organizer) {
		Geocoder.GeocodingResult found = geocoder.geocode(address)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Не удалось определить адрес. Укажите город, улицу и дом"));
		point.setLatitude(found.latitude());
		point.setLongitude(found.longitude());
		point.setCity(cityFor(found, address, organizer));
	}

	/** Город: от геокодера, иначе первая часть адреса («Москва, ул. …»), иначе город из профиля. */
	private static String cityFor(Geocoder.GeocodingResult found, String address, User organizer) {
		if (found.city() != null) {
			return found.city();
		}
		int comma = address.indexOf(',');
		if (comma > 0) {
			return address.substring(0, comma).strip();
		}
		if (organizer.getCity() != null) {
			return organizer.getCity();
		}
		throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите город в адресе");
	}
}
