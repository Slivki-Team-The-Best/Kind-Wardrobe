package ru.urfu.backend.listing;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.urfu.backend.entity.enums.ListingStatus;

@RestController
@RequestMapping("/api/listings")
public class ListingController {

	private final ListingService listingService;

	public ListingController(ListingService listingService) {
		this.listingService = listingService;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('USER')")
	public ListingResponse create(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestPart("listing") ListingRequest request,
			@RequestPart(value = "photos", required = false) List<MultipartFile> photos) {
		return listingService.create(userId(jwt), request, photos);
	}

	@GetMapping("/my")
	public List<ListingSummaryResponse> mine(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) ListingStatus status) {
		return listingService.mine(userId(jwt), status);
	}

	@GetMapping("/{id}")
	public ListingResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		return listingService.get(id, jwt == null ? null : userId(jwt));
	}

	@PutMapping("/{id}")
	public ListingResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
			@Valid @RequestBody ListingRequest request) {
		return listingService.update(userId(jwt), id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		listingService.delete(userId(jwt), id);
	}

	@PostMapping(value = "/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ListingResponse uploadPhotos(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
			@RequestPart("photos") List<MultipartFile> photos) {
		return listingService.uploadPhotos(userId(jwt), id, photos);
	}

	@DeleteMapping("/{id}/photos/{photoId}")
	public ListingResponse deletePhoto(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
			@PathVariable Long photoId) {
		return listingService.deletePhoto(userId(jwt), id, photoId);
	}

	private static Long userId(Jwt jwt) {
		return Long.valueOf(jwt.getSubject());
	}
}
