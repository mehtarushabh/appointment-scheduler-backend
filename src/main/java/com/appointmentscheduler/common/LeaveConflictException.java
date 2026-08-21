package com.appointmentscheduler.common;

import com.appointmentscheduler.dto.DoctorLeaveDtos.LeaveConflictResponse;

/**
 * Thrown when a leave attempt conflicts with one or more SCHEDULED appointments and the caller has
 * not yet confirmed cancellation (FR-028, research.md #7) — carries the 409 body directly, since it
 * is a LeaveConflictResponse rather than the usual ErrorResponse shape.
 */
public class LeaveConflictException extends RuntimeException {

	private final LeaveConflictResponse response;

	public LeaveConflictException(LeaveConflictResponse response) {
		super(response.message());
		this.response = response;
	}

	public LeaveConflictResponse getResponse() {
		return response;
	}
}
