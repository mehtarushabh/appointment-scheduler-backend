package com.appointmentscheduler.dto;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Shared base fields for onboarding any user account (FR-017); Doctor adds `specialty` on top. */
public record UserOnboardingRequest(
		@NotBlank String firstName,
		@NotBlank String lastName,
		@NotBlank @Email String email,
		@NotNull LocalDate dateOfBirth,
		@NotNull @Valid AddressDto address) {
}
