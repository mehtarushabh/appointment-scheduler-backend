package com.appointmentscheduler.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.appointmentscheduler.model.Appointment;
import com.appointmentscheduler.model.User;

/**
 * Notifies a Patient by email when their appointment is cancelled (FR-024), structured identically
 * to {@link WelcomeEmailService} (research.md #4): best-effort send, logs a warning on failure
 * rather than propagating — the cancellation itself still takes effect either way.
 */
@Service
public class AppointmentCancellationEmailService {

	private static final Logger log = LoggerFactory.getLogger(AppointmentCancellationEmailService.class);

	private final JavaMailSender mailSender;

	public AppointmentCancellationEmailService(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

	public void sendCancellationEmail(User patient, Appointment appointment) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(patient.getEmail());
		message.setSubject("Your appointment has been cancelled");
		message.setText("Hello " + patient.getFirstName() + ",\n\n"
			+ "Your appointment on " + appointment.getDate() + " at " + appointment.getStartTime() + " has been cancelled.\n");
		try {
			mailSender.send(message);
		} catch (MailException e) {
			log.warn("Failed to send cancellation email to {}; cancellation still succeeded.", patient.getEmail(), e);
		}
	}
}
