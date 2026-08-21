package com.appointmentscheduler.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.appointmentscheduler.model.Appointment;
import com.appointmentscheduler.model.AppointmentState;
import com.appointmentscheduler.model.Clinic;
import com.appointmentscheduler.model.User;

import jakarta.validation.constraints.NotNull;

public final class AppointmentDtos {

	private AppointmentDtos() {
	}

	/** Deliberately minimal (research.md #1) — no address, date of birth, or email. */
	public record DoctorSummaryResponse(UUID id, String firstName, String lastName, String specialty) {

		public static DoctorSummaryResponse from(User doctor) {
			return new DoctorSummaryResponse(doctor.getId(), doctor.getFirstName(), doctor.getLastName(), doctor.getSpecialty());
		}
	}

	public record AvailableSlotsResponse(LocalDate date, int durationMinutes, List<LocalTime> startTimes) {
	}

	public record BookAppointmentRequest(
			@NotNull UUID clinicId,
			@NotNull UUID doctorId,
			@NotNull LocalDate date,
			@NotNull LocalTime startTime,
			int durationMinutes) {
	}

	public record AppointmentResponse(
			UUID id,
			UUID patientId,
			String patientName,
			UUID doctorId,
			String doctorName,
			UUID clinicId,
			String clinicName,
			LocalDate date,
			LocalTime startTime,
			int durationMinutes,
			AppointmentState state) {

		public static AppointmentResponse from(Appointment appointment, User patient, User doctor, Clinic clinic) {
			return new AppointmentResponse(appointment.getId(), patient.getId(), patient.getFirstName() + " " + patient.getLastName(),
					doctor.getId(), doctor.getFirstName() + " " + doctor.getLastName(), clinic.getId(), clinic.getName(),
					appointment.getDate(), appointment.getStartTime(), appointment.getDurationMinutes(), appointment.getState());
		}
	}
}
