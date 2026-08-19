package com.appointmentscheduler.dto;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class DoctorDtos {

	private DoctorDtos() {
	}

	/** UserOnboardingRequest fields + specialty (FR-019). */
	public record DoctorOnboardingRequest(
			@NotBlank String firstName,
			@NotBlank String lastName,
			@NotBlank @Email String email,
			@NotNull LocalDate dateOfBirth,
			@NotNull @Valid AddressDto address,
			@NotBlank String specialty) {
	}
}
