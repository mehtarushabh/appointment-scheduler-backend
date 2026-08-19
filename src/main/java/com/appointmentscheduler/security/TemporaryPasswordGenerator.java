package com.appointmentscheduler.security;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

/** Generates the system-generated initial/resend password for a new or resent account (FR-020). */
@Component
public class TemporaryPasswordGenerator {

	private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
	private static final int LENGTH = 16;
	private final SecureRandom random = new SecureRandom();

	public String generate() {
		StringBuilder sb = new StringBuilder(LENGTH);
		for (int i = 0; i < LENGTH; i++) {
			sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
		}
		return sb.toString();
	}
}
