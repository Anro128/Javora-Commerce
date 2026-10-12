package com.javora.commerce.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
public class MailService {

	private final JavaMailSender mailSender;
	private final TemplateEngine templateEngine;
	private final String from;
	private final String frontendUrl;

	public MailService(
			JavaMailSender mailSender,
			TemplateEngine templateEngine,
			@Value("${app.mail.from}") String from,
			@Value("${app.frontend-url}") String frontendUrl) {
		this.mailSender = mailSender;
		this.templateEngine = templateEngine;
		this.from = from;
		this.frontendUrl = frontendUrl;
	}

	@Async
	public void sendVerificationEmail(String to, String fullName, String token) {
		String link = link("/verify-email", token);
		send(to, "Verify your Javora account", "email/verify-email", Map.of("name", fullName, "link", link));
	}

	@Async
	public void sendPasswordResetEmail(String to, String fullName, String token) {
		String link = link("/reset-password", token);
		send(to, "Reset your Javora password", "email/reset-password", Map.of("name", fullName, "link", link));
	}

	private String link(String path, String token) {
		return UriComponentsBuilder.fromUriString(frontendUrl).path(path).queryParam("token", token).toUriString();
	}

	private void send(String to, String subject, String template, Map<String, Object> variables) {
		try {
			Context context = new Context();
			context.setVariables(variables);
			String html = templateEngine.process(template, context);

			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
			helper.setFrom(from);
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(html, true);
			mailSender.send(message);
		} catch (MessagingException | MailException ex) {
			// A mail outage must never break the user-facing request that triggered it.
			log.warn("Could not send '{}' email: {}", subject, ex.getMessage());
		}
	}

}
