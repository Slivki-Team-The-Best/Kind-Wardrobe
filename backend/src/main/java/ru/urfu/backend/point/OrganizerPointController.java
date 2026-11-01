package ru.urfu.backend.point;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Пункт обмена текущего организатора. Доступно только роли ORGANIZER (для USER — 403). */
@RestController
@RequestMapping("/api/organizer/point")
@PreAuthorize("hasRole('ORGANIZER')")
public class OrganizerPointController {

	private final OrganizerPointService service;

	public OrganizerPointController(OrganizerPointService service) {
		this.service = service;
	}

	@GetMapping
	public PointResponse get(@AuthenticationPrincipal Jwt jwt) {
		return service.get(Long.valueOf(jwt.getSubject()));
	}

	@PutMapping
	public PointResponse save(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdatePointRequest request) {
		return service.save(Long.valueOf(jwt.getSubject()), request);
	}
}
