package com.appointmentscheduler.common;

import java.time.Instant;
import java.util.List;

/** Structured error response shape shared by every endpoint (contracts/onboarding-api.yaml). */
public record ErrorResponse(Instant timestamp, int status, String error, String message, List<FieldError> fieldErrors) {

	public record FieldError(String field, String message) {
	}

	public static ErrorResponse of(int status, String error, String message) {
		return new ErrorResponse(Instant.now(), status, error, message, List.of());
	}

	public static ErrorResponse of(int status, String error, String message, List<FieldError> fieldErrors) {
		return new ErrorResponse(Instant.now(), status, error, message, fieldErrors);
	}
}
