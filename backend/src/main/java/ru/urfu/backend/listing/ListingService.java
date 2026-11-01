package ru.urfu.backend.listing;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.urfu.backend.common.ApiException;
import ru.urfu.backend.entity.Listing;
import ru.urfu.backend.entity.ListingPhoto;
import ru.urfu.backend.entity.User;
import ru.urfu.backend.entity.enums.ListingStatus;
import ru.urfu.backend.repository.ListingPhotoRepository;
import ru.urfu.backend.repository.ListingRepository;
import ru.urfu.backend.repository.UserRepository;
import ru.urfu.backend.storage.FileStorageService;

@Service
public class ListingService {

	public static final int MAX_PHOTOS = 5;
	public static final long MAX_PHOTO_BYTES = 10L * 1024 * 1024;

	private static final String PHOTOS_FOLDER = "listings";

	private final ListingRepository listings;
	private final ListingPhotoRepository photos;
	private final UserRepository users;
	private final FileStorageService storage;

	public ListingService(ListingRepository listings, ListingPhotoRepository photos, UserRepository users,
			FileStorageService storage) {
		this.listings = listings;
		this.photos = photos;
		this.users = users;
		this.storage = storage;
	}

	@Transactional
	public ListingResponse create(Long userId, ListingRequest request, List<MultipartFile> files) {
		User author = users.findById(userId).orElseThrow(() -> ApiException.notFound("Пользователь не найден"));
		List<MultipartFile> newPhotos = nonEmpty(files);
		ListingStatus status = request.status() == null ? ListingStatus.ACTIVE : request.status();
		Map<String, String> errors = new LinkedHashMap<>();
		if (status != ListingStatus.ACTIVE && status != ListingStatus.DRAFT) {
			errors.put("status", "Новое объявление можно только опубликовать или сохранить как черновик");
		}
		if (newPhotos.size() > MAX_PHOTOS) {
			errors.put("photos", "Можно загрузить не больше " + MAX_PHOTOS + " фото");
		}
		Listing listing = new Listing();
		listing.setAuthor(author);
		apply(listing, request, errors);
		if (status == ListingStatus.ACTIVE) {
			checkPublishable(listing, newPhotos.size(), errors);
		}
		if (!errors.isEmpty()) {
			throw ApiException.badRequest(errors);
		}
		listing.setStatus(status);
		if (status == ListingStatus.ACTIVE) {
			listing.setPublishedAt(OffsetDateTime.now());
		}
		listings.save(listing);
		addPhotos(listing, newPhotos);
		listings.flush();
		return ListingResponse.from(listing);
	}

	@Transactional
	public ListingResponse update(Long userId, Long listingId, ListingRequest request) {
		Listing listing = loadOwned(userId, listingId);
		ensureEditable(listing);
		ListingStatus status = request.status() == null ? listing.getStatus() : request.status();
		Map<String, String> errors = new LinkedHashMap<>();
		if (status == ListingStatus.TRANSFERRED) {
			errors.put("status", "Отметить обмен можно отдельной кнопкой");
		}
		apply(listing, request, errors);
		if (status == ListingStatus.ACTIVE) {
			checkPublishable(listing, listing.getPhotos().size(), errors);
		}
		if (!errors.isEmpty()) {
			throw ApiException.badRequest(errors);
		}
		listing.setStatus(status);
		if (status == ListingStatus.ACTIVE && listing.getPublishedAt() == null) {
			listing.setPublishedAt(OffsetDateTime.now());
		}
		listings.flush();
		return ListingResponse.from(listing);
	}

	@Transactional
	public ListingResponse uploadPhotos(Long userId, Long listingId, List<MultipartFile> files) {
		Listing listing = loadOwned(userId, listingId);
		ensureEditable(listing);
		List<MultipartFile> newPhotos = nonEmpty(files);
		if (newPhotos.isEmpty()) {
			throw ApiException.badRequest("photos", "Выберите хотя бы одно фото");
		}
		if (listing.getPhotos().size() + newPhotos.size() > MAX_PHOTOS) {
			throw ApiException.badRequest("photos", "Можно загрузить не больше " + MAX_PHOTOS + " фото");
		}
		addPhotos(listing, newPhotos);
		listings.flush();
		return ListingResponse.from(listing);
	}

	@Transactional
	public ListingResponse deletePhoto(Long userId, Long listingId, Long photoId) {
		Listing listing = loadOwned(userId, listingId);
		ensureEditable(listing);
		ListingPhoto photo = listing.getPhotos().stream()
				.filter(p -> p.getId().equals(photoId))
				.findFirst()
				.orElseThrow(() -> ApiException.notFound("Фото не найдено"));
		if (listing.getStatus() == ListingStatus.ACTIVE && listing.getPhotos().size() == 1) {
			throw ApiException.badRequest("photos", "У опубликованного объявления должно остаться хотя бы одно фото");
		}
		listing.removePhoto(photo);
		listings.flush();
		for (int i = 0; i < listing.getPhotos().size(); i++) {
			ListingPhoto remaining = listing.getPhotos().get(i);
			if (remaining.getPosition() != i) {
				remaining.setPosition((short) i);
				photos.saveAndFlush(remaining);
			}
		}
		storage.deleteAfterCommit(photo.getUrl());
		return ListingResponse.from(listing);
	}

	@Transactional
	public void delete(Long userId, Long listingId) {
		Listing listing = loadOwned(userId, listingId);
		List<String> urls = listing.getPhotos().stream().map(ListingPhoto::getUrl).toList();
		listings.delete(listing);
		urls.forEach(storage::deleteAfterCommit);
	}

	@Transactional(readOnly = true)
	public ListingResponse get(Long listingId, Long viewerId) {
		Listing listing = listings.findWithPhotosAndAuthorById(listingId)
				.orElseThrow(() -> ApiException.notFound("Объявление не найдено"));
		boolean isAuthor = Objects.equals(listing.getAuthor().getId(), viewerId);
		boolean isPublic = listing.getStatus() == ListingStatus.ACTIVE || listing.getStatus() == ListingStatus.TRANSFERRED;
		if (!isPublic && !isAuthor) {
			throw ApiException.notFound("Объявление не найдено");
		}
		return ListingResponse.from(listing);
	}

	@Transactional(readOnly = true)
	public List<ListingSummaryResponse> mine(Long userId, ListingStatus status) {
		List<Listing> found = status == null
				? listings.findByAuthorIdOrderByCreatedAtDesc(userId)
				: listings.findByAuthorIdAndStatusOrderByCreatedAtDesc(userId, status);
		return found.stream().map(ListingSummaryResponse::from).toList();
	}

	private Listing loadOwned(Long userId, Long listingId) {
		Listing listing = listings.findWithPhotosAndAuthorById(listingId)
				.orElseThrow(() -> ApiException.notFound("Объявление не найдено"));
		if (!listing.getAuthor().getId().equals(userId)) {
			throw ApiException.forbidden("Это объявление принадлежит другому пользователю");
		}
		return listing;
	}

	private static void ensureEditable(Listing listing) {
		if (listing.getStatus() == ListingStatus.TRANSFERRED) {
			throw ApiException.conflict("Переданное объявление нельзя изменить");
		}
	}

	private static void apply(Listing listing, ListingRequest request, Map<String, String> errors) {
		listing.setTitle(request.title().strip());
		listing.setItemType(request.itemType());
		listing.setCategory(request.category());
		listing.setCondition(request.condition());
		listing.setDescription(request.description().strip());
		listing.setExchangeFormat(request.exchangeFormat());
		listing.setWeightKg(request.weightKg());
		listing.setCity(request.city().strip());
		listing.setTelegramUrl(ContactLinks.telegram(request.telegramUrl(), "telegramUrl", errors));
		listing.setWhatsappUrl(ContactLinks.whatsapp(request.whatsappUrl(), "whatsappUrl", errors));
		listing.setMaxUrl(ContactLinks.max(request.maxUrl(), "maxUrl", errors));
	}

	private static void checkPublishable(Listing listing, int photoCount, Map<String, String> errors) {
		if (photoCount == 0) {
			errors.putIfAbsent("photos", "Загрузите хотя бы одно фото");
		}
		if (listing.getTelegramUrl() == null && listing.getWhatsappUrl() == null && listing.getMaxUrl() == null
				&& !errors.containsKey("telegramUrl") && !errors.containsKey("whatsappUrl") && !errors.containsKey("maxUrl")) {
			errors.put("contacts", "Укажите ссылку на Telegram, WhatsApp или MAX");
		}
	}

	private void addPhotos(Listing listing, List<MultipartFile> files) {
		int position = listing.getPhotos().size();
		for (MultipartFile file : files) {
			ListingPhoto photo = new ListingPhoto();
			photo.setUrl(storage.storeImage(file, PHOTOS_FOLDER, MAX_PHOTO_BYTES, "photos"));
			photo.setPosition((short) position++);
			listing.addPhoto(photo);
		}
	}

	private static List<MultipartFile> nonEmpty(List<MultipartFile> files) {
		return files == null ? List.of() : files.stream().filter(f -> f != null && !f.isEmpty()).toList();
	}
}
