package com.javora.commerce.notification;

import com.javora.commerce.auth.event.PasswordResetRequestedEvent;
import com.javora.commerce.auth.event.VerificationRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AuthNotificationListener {

	private final MailService mailService;

	// Emails are sent only after the transaction commits so a rollback never leaks a valid link.
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onVerificationRequested(VerificationRequestedEvent event) {
		mailService.sendVerificationEmail(event.email(), event.fullName(), event.verificationToken());
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
		mailService.sendPasswordResetEmail(event.email(), event.fullName(), event.resetToken());
	}

}
