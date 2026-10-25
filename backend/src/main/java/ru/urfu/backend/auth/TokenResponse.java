package ru.urfu.backend.auth;

import ru.urfu.backend.user.UserRole;

public record TokenResponse(String accessToken, String tokenType, long expiresIn, Long userId, UserRole role) {

	public static final String BEARER = "Bearer";
}
