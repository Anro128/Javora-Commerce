package com.javora.commerce.auth.service;

import com.javora.commerce.auth.AuthProperties;
import com.javora.commerce.user.entity.User;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

	static final String ISSUER = "javora-commerce";
	public static final String ROLES_CLAIM = "roles";

	private final JwtEncoder jwtEncoder;
	private final AuthProperties properties;

	public String issueAccessToken(User user) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(ISSUER)
				.subject(String.valueOf(user.getId()))
				.issuedAt(now)
				.expiresAt(now.plus(properties.accessTokenTtl()))
				.claim("email", user.getEmail())
				.claim(ROLES_CLAIM, List.of(user.getRole().name()))
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	public long accessTokenTtlSeconds() {
		return properties.accessTokenTtl().toSeconds();
	}

}
