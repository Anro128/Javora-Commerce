package com.javora.commerce.common.config;

import com.javora.commerce.auth.service.JwtService;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	private static final String AUTH_PREFIX = "/api/v1/auth/";
	private static final String PUBLIC_PREFIX = "/api/v1/public/";

	// Auth endpoints that require a logged in user; every other auth endpoint is public.
	private static final Set<String> AUTHENTICATED_AUTH_PATHS = Set.of(
			AUTH_PREFIX + "resend-verification",
			AUTH_PREFIX + "change-password");

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, BearerTokenResolver bearerTokenResolver)
			throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.oauth2ResourceServer(oauth2 -> oauth2
						.bearerTokenResolver(bearerTokenResolver)
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
						.requestMatchers("/actuator/**").denyAll()
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.requestMatchers(AUTHENTICATED_AUTH_PATHS.toArray(String[]::new)).authenticated()
						.requestMatchers(AUTH_PREFIX + "**", PUBLIC_PREFIX + "**").permitAll()
						.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
						.requestMatchers("/api/**").authenticated()
						// Everything else is the React SPA and its static assets.
						.anyRequest().permitAll());
		return http.build();
	}

	// A stale access token must not block public calls such as browsing the shop, login, or refresh.
	@Bean
	BearerTokenResolver bearerTokenResolver() {
		DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
		return request -> {
			String path = request.getRequestURI();
			boolean publicAuthCall = path.startsWith(AUTH_PREFIX) && !AUTHENTICATED_AUTH_PATHS.contains(path);
			boolean publicCall = publicAuthCall || path.startsWith(PUBLIC_PREFIX);
			return publicCall ? null : delegate.resolve(request);
		};
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	private static JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
		authorities.setAuthoritiesClaimName(JwtService.ROLES_CLAIM);
		authorities.setAuthorityPrefix("ROLE_");

		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authorities);
		return converter;
	}

}
