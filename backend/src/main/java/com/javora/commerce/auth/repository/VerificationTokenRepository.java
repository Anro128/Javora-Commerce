package com.javora.commerce.auth.repository;

import com.javora.commerce.auth.entity.TokenType;
import com.javora.commerce.auth.entity.VerificationToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

	Optional<VerificationToken> findByTokenHashAndType(String tokenHash, TokenType type);

	@Modifying
	@Query("update VerificationToken t set t.usedAt = :now "
			+ "where t.userId = :userId and t.type = :type and t.usedAt is null")
	int invalidateUnused(@Param("userId") Long userId, @Param("type") TokenType type, @Param("now") Instant now);

}
