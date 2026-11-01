package ru.urfu.backend.user;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.urfu.backend.profile.ProfileResponse;

@RestController
@RequestMapping("/api/users/me")
public class UserAccountController {

	private final UserAccountService service;

	public UserAccountController(UserAccountService service) {
		this.service = service;
	}

	@GetMapping("/stats")
	public UserStatsResponse stats(@AuthenticationPrincipal Jwt jwt) {
		return service.stats(Long.valueOf(jwt.getSubject()));
	}

	@PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ProfileResponse uploadAvatar(@AuthenticationPrincipal Jwt jwt, @RequestPart("avatar") MultipartFile avatar) {
		return service.uploadAvatar(Long.valueOf(jwt.getSubject()), avatar);
	}
}
