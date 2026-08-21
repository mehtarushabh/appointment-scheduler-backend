package com.appointmentscheduler.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.appointmentscheduler.dto.AppointmentDtos.AppointmentResponse;
import com.appointmentscheduler.model.DoctorLeave;

import jakarta.validation.constraints.NotNull;

public final class DoctorLeaveDtos {

	private DoctorLeaveDtos() {
	}

	public record LeaveRequest(
			@NotNull LocalDate date,
			boolean fullDay,
			LocalTime startTime,
			LocalTime endTime,
			boolean confirmCancelConflicts) {
	}

	public record LeaveResponse(UUID id, LocalDate date, boolean fullDay, LocalTime startTime, LocalTime endTime) {

		public static LeaveResponse from(DoctorLeave leave) {
			return new LeaveResponse(leave.getId(), leave.getDate(), leave.isFullDay(), leave.getStartTime(), leave.getEndTime());
		}
	}

	/** Returned instead of ErrorResponse when a leave conflicts with SCHEDULED appointments (FR-028). */
	public record LeaveConflictResponse(String message, List<AppointmentResponse> conflictingAppointments) {
	}
}
