package ru.urfu.backend.profile;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Профиль текущего пользователя (любая роль). */
@RestController
@RequestMapping("/api/users/me")
public class ProfileController {

	private final ProfileService profileService;

	public ProfileController(ProfileService profileService) {
		this.profileService = profileService;
	}

	@GetMapping
	public ProfileResponse get(@AuthenticationPrincipal Jwt jwt) {
		return profileService.get(Long.valueOf(jwt.getSubject()));
	}

	@PutMapping
	public ProfileResponse update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
		return profileService.update(Long.valueOf(jwt.getSubject()), request);
	}
}
