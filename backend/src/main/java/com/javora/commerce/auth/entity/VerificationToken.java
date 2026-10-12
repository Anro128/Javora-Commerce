package com.javora.commerce.auth.entity;

import com.javora.commerce.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "verification_tokens")
public class VerificationToken extends BaseEntity {

	@Column(name = "user_id", nullable = false, updatable = false)
	private Long userId;

	@Column(name = "token_hash", nullable = false, updatable = false)
	private String tokenHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private TokenType type;

	@Column(name = "expires_at", nullable = false, updatable = false)
	private Instant expiresAt;

	@Column(name = "used_at")
	private Instant usedAt;

	public VerificationToken(Long userId, String tokenHash, TokenType type, Instant expiresAt) {
		this.userId = userId;
		this.tokenHash = tokenHash;
		this.type = type;
		this.expiresAt = expiresAt;
	}

	public boolean isUsable(Instant now) {
		return usedAt == null && expiresAt.isAfter(now);
	}

}
