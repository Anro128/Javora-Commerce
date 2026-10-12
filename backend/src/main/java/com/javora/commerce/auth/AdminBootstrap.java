package com.javora.commerce.auth;

import com.javora.commerce.user.entity.Role;
import com.javora.commerce.user.entity.User;
import com.javora.commerce.user.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Creates the first admin account when credentials are configured and the email does not exist yet.
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.bootstrap-admin", name = {"email", "password"})
public class AdminBootstrap implements ApplicationRunner {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Value("${app.bootstrap-admin.email}")
	private String email;

	@Value("${app.bootstrap-admin.password}")
	private String password;

	@Value("${app.bootstrap-admin.full-name:Administrator}")
	private String fullName;

	@Override
	public void run(ApplicationArguments args) {
		String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
		if (userRepository.existsByEmail(normalizedEmail)) {
			return;
		}

		User admin = new User();
		admin.setEmail(normalizedEmail);
		admin.setPasswordHash(passwordEncoder.encode(password));
		admin.setFullName(fullName);
		admin.setRole(Role.ADMIN);
		admin.setEmailVerified(true);
		userRepository.save(admin);
		log.info("Bootstrap admin account created for {}", normalizedEmail);
	}

}
