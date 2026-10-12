package com.javora.commerce.auth.service;

import com.javora.commerce.auth.AuthProperties;
import com.javora.commerce.auth.dto.AuthResponse;
import com.javora.commerce.auth.dto.ChangePasswordRequest;
import com.javora.commerce.auth.dto.LoginRequest;
import com.javora.commerce.auth.dto.RegisterRequest;
import com.javora.commerce.auth.entity.RefreshToken;
import com.javora.commerce.auth.entity.TokenType;
import com.javora.commerce.auth.entity.VerificationToken;
import com.javora.commerce.auth.event.PasswordResetRequestedEvent;
import com.javora.commerce.auth.event.VerificationRequestedEvent;
import com.javora.commerce.auth.repository.RefreshTokenRepository;
import com.javora.commerce.auth.repository.VerificationTokenRepository;
import com.javora.commerce.common.exception.BusinessException;
import com.javora.commerce.common.exception.ResourceNotFoundException;
import com.javora.commerce.common.util.TokenGenerator;
import com.javora.commerce.user.dto.UserResponse;
import com.javora.commerce.user.entity.Role;
import com.javora.commerce.user.entity.User;
import com.javora.commerce.user.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

	// Compared against when the email is unknown so response time does not reveal registered emails.
	private static final String DUMMY_HASH = new BCryptPasswordEncoder().encode("javora-dummy-password-1");

	// Tolerates two browser tabs rotating the same refresh token at nearly the same time.
	private static final Duration REUSE_GRACE_PERIOD = Duration.ofSeconds(10);

	private static final String INVALID_CREDENTIALS = "Invalid email or password";
	private static final String INVALID_SESSION = "Session is invalid or has expired";
	private static final String INVALID_TOKEN = "This link is invalid or has expired";

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final VerificationTokenRepository verificationTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthProperties properties;
	private final ApplicationEventPublisher events;

	@Transactional
	public Session register(RegisterRequest request) {
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmail(email)) {
			throw new BusinessException(HttpStatus.CONFLICT, "Email is already registered");
		}

		User user = new User();
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setFullName(request.fullName().trim());
		user.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
		user.setRole(Role.CUSTOMER);
		userRepository.save(user);

		String token = createVerificationToken(user.getId(), TokenType.EMAIL_VERIFICATION,
				properties.verificationTokenTtl());
		events.publishEvent(new VerificationRequestedEvent(user.getEmail(), user.getFullName(), token));
		return openSession(user);
	}

	@Transactional
	public Session login(LoginRequest request) {
		Optional<User> found = userRepository.findByEmail(normalizeEmail(request.email()));
		String hash = found.map(User::getPasswordHash).orElse(DUMMY_HASH);
		boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
		if (found.isEmpty() || !passwordMatches) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
		}

		User user = found.get();
		if (!user.isEnabled()) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "Your account has been disabled");
		}
		return openSession(user);
	}

	// Keeps the revocation on failure so a replayed token also kills the whole token family.
	@Transactional(noRollbackFor = BusinessException.class)
	public Session refresh(String rawRefreshToken) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, INVALID_SESSION);
		}

		Instant now = Instant.now();
		RefreshToken stored = refreshTokenRepository.findByTokenHash(TokenGenerator.sha256Hex(rawRefreshToken))
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, INVALID_SESSION));

		if (stored.isRevoked()) {
			boolean withinGrace = stored.getRevokedAt().plus(REUSE_GRACE_PERIOD).isAfter(now);
			if (!withinGrace) {
				refreshTokenRepository.revokeAllForUser(stored.getUserId(), now);
			}
			throw new BusinessException(HttpStatus.UNAUTHORIZED, INVALID_SESSION);
		}
		if (stored.isExpired(now)) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, INVALID_SESSION);
		}

		User user = userRepository.findById(stored.getUserId())
				.filter(User::isEnabled)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, INVALID_SESSION));

		stored.setRevokedAt(now);
		return openSession(user);
	}

	@Transactional
	public void logout(String rawRefreshToken) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			return;
		}
		refreshTokenRepository.findByTokenHash(TokenGenerator.sha256Hex(rawRefreshToken))
				.filter(token -> !token.isRevoked())
				.ifPresent(token -> token.setRevokedAt(Instant.now()));
	}

	@Transactional
	public void verifyEmail(String rawToken) {
		VerificationToken token = findUsableToken(rawToken, TokenType.EMAIL_VERIFICATION);
		token.setUsedAt(Instant.now());
		userRepository.findById(token.getUserId()).ifPresent(user -> user.setEmailVerified(true));
	}

	@Transactional
	public void resendVerification(Long userId) {
		User user = findUser(userId);
		if (user.isEmailVerified()) {
			throw new BusinessException("Your email is already verified");
		}
		verificationTokenRepository.invalidateUnused(userId, TokenType.EMAIL_VERIFICATION, Instant.now());
		String token = createVerificationToken(userId, TokenType.EMAIL_VERIFICATION, properties.verificationTokenTtl());
		events.publishEvent(new VerificationRequestedEvent(user.getEmail(), user.getFullName(), token));
	}

	// Always succeeds from the caller's view so the endpoint cannot be used to discover accounts.
	@Transactional
	public void forgotPassword(String email) {
		userRepository.findByEmail(normalizeEmail(email)).filter(User::isEnabled).ifPresent(user -> {
			verificationTokenRepository.invalidateUnused(user.getId(), TokenType.PASSWORD_RESET, Instant.now());
			String token = createVerificationToken(user.getId(), TokenType.PASSWORD_RESET,
					properties.passwordResetTokenTtl());
			events.publishEvent(new PasswordResetRequestedEvent(user.getEmail(), user.getFullName(), token));
		});
	}

	@Transactional
	public void resetPassword(String rawToken, String newPassword) {
		VerificationToken token = findUsableToken(rawToken, TokenType.PASSWORD_RESET);
		Instant now = Instant.now();
		token.setUsedAt(now);

		User user = findUser(token.getUserId());
		user.setPasswordHash(passwordEncoder.encode(newPassword));
		refreshTokenRepository.revokeAllForUser(user.getId(), now);
	}

	@Transactional
	public void changePassword(Long userId, ChangePasswordRequest request) {
		User user = findUser(userId);
		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw new BusinessException("Current password is incorrect");
		}
		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		refreshTokenRepository.revokeAllForUser(userId, Instant.now());
	}

	private Session openSession(User user) {
		String rawRefreshToken = TokenGenerator.generate();
		Instant expiresAt = Instant.now().plus(properties.refreshTokenTtl());
		refreshTokenRepository.save(new RefreshToken(user.getId(), TokenGenerator.sha256Hex(rawRefreshToken), expiresAt));

		AuthResponse response = AuthResponse.bearer(
				jwtService.issueAccessToken(user), jwtService.accessTokenTtlSeconds(), UserResponse.from(user));
		return new Session(response, rawRefreshToken);
	}

	private String createVerificationToken(Long userId, TokenType type, Duration ttl) {
		String raw = TokenGenerator.generate();
		verificationTokenRepository.save(
				new VerificationToken(userId, TokenGenerator.sha256Hex(raw), type, Instant.now().plus(ttl)));
		return raw;
	}

	private VerificationToken findUsableToken(String rawToken, TokenType type) {
		return verificationTokenRepository.findByTokenHashAndType(TokenGenerator.sha256Hex(rawToken), type)
				.filter(token -> token.isUsable(Instant.now()))
				.orElseThrow(() -> new BusinessException(INVALID_TOKEN));
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
