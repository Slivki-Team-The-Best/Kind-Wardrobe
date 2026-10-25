package ru.urfu.backend.auth;

import java.util.Locale;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urfu.backend.config.JwtService;
import ru.urfu.backend.entity.User;
import ru.urfu.backend.repository.UserRepository;

@Service
public class AuthService {

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final String dummyHash;

	public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.dummyHash = passwordEncoder.encode("not-a-real-password");
	}

	@Transactional(readOnly = true)
	public TokenResponse login(String identifier, String password) {
		Optional<User> found = findByIdentifier(identifier.strip());
		boolean passwordMatches = passwordEncoder.matches(password, found.map(User::getPasswordHash).orElse(dummyHash));
		if (found.isEmpty() || !passwordMatches) {
			throw new InvalidCredentialsException();
		}
		User user = found.get();
		JwtService.IssuedToken token = jwtService.issueAccessToken(user);
		return new TokenResponse(token.value(), TokenResponse.BEARER, token.expiresInSeconds(), user.getId(), user.getRole());
	}

	private Optional<User> findByIdentifier(String identifier) {
		return identifier.contains("@")
				? users.findByEmail(identifier.toLowerCase(Locale.ROOT))
				: users.findByPhone(identifier);
	}
}
