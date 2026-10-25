package ru.urfu.backend.config;

import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import ru.urfu.backend.entity.User;

@Service
public class JwtService {

	public static final String ROLE_CLAIM = "role";

	private final JwtEncoder encoder;
	private final JwtProperties properties;

	public JwtService(JwtEncoder encoder, JwtProperties properties) {
		this.encoder = encoder;
		this.properties = properties;
	}

	public IssuedToken issueAccessToken(User user) {
		Instant now = Instant.now();
		Instant expiresAt = now.plus(properties.accessTokenTtl());
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(properties.issuer())
				.subject(String.valueOf(user.getId()))
				.issuedAt(now)
				.expiresAt(expiresAt)
				.claim("email", user.getEmail())
				.claim(ROLE_CLAIM, user.getRole().name())
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new IssuedToken(token, properties.accessTokenTtl().toSeconds());
	}

	public record IssuedToken(String value, long expiresInSeconds) {
	}
}
