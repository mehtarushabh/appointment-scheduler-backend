package com.appointmentscheduler.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.appointmentscheduler.model.Role;
import com.appointmentscheduler.model.User;

public record UserResponse(
		UUID id,
		String firstName,
		String lastName,
		String email,
		LocalDate dateOfBirth,
		AddressDto address,
		Role role,
		UUID clinicId,
		String specialty) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
				user.getDateOfBirth(), AddressDto.from(user.getAddress()), user.getRole(), user.getClinicId(),
				user.getSpecialty());
	}
}
