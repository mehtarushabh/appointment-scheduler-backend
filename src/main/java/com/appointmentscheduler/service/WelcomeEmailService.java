package com.appointmentscheduler.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.appointmentscheduler.model.User;

/**
 * Sends the welcome email containing a new user's system-generated password (FR-020), and
 * resends it on demand (FR-020a). Best-effort: a send failure MUST NOT roll back account
 * creation (research.md #7) — callers invoke this after the account is already persisted and
 * simply log a failure rather than propagate it.
 */
@Service
public class WelcomeEmailService {

	private static final Logger log = LoggerFactory.getLogger(WelcomeEmailService.class);

	private final JavaMailSender mailSender;

	public WelcomeEmailService(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

	public void sendWelcomeEmail(User user, String temporaryPassword) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(user.getEmail());
		message.setSubject("Welcome to Appointment Scheduler");
		message.setText("Hello " + user.getFirstName() + ",\n\n"
			+ "Your account has been created. Your temporary password is: " + temporaryPassword + "\n"
			+ "You can change it any time after logging in — this is never required.\n");
		try {
			mailSender.send(message);
		} catch (MailException e) {
			log.warn("Failed to send welcome email to {}; account creation still succeeded.", user.getEmail(), e);
		}
	}
}
