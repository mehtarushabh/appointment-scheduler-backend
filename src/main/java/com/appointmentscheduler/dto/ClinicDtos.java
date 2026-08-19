package com.appointmentscheduler.dto;

import java.util.UUID;

import com.appointmentscheduler.model.Clinic;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class ClinicDtos {

	private ClinicDtos() {
	}

	public record ClinicOnboardingRequest(
			@NotBlank String name,
			@NotNull @Valid AddressDto address,
			@NotBlank String registeredId,
			@NotNull @Valid UserOnboardingRequest firstClinicAdmin) {
	}

	public record ClinicResponse(UUID id, String name, AddressDto address, String registeredId, UserResponse firstClinicAdmin) {

		public static ClinicResponse from(Clinic clinic, UserResponse firstClinicAdmin) {
			return new ClinicResponse(clinic.getId(), clinic.getName(), AddressDto.from(clinic.getAddress()),
					clinic.getRegisteredId(), firstClinicAdmin);
		}
	}
}
