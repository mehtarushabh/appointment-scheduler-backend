package com.appointmentscheduler.dto;

import com.appointmentscheduler.model.Address;

import jakarta.validation.constraints.NotBlank;

/** Request/response shape for the shared Address value object (contracts/onboarding-api.yaml). */
public record AddressDto(
		@NotBlank String addressLine1,
		String addressLine2,
		@NotBlank String city,
		@NotBlank String state,
		@NotBlank String zip,
		@NotBlank String country) {

	public Address toModel() {
		return new Address(addressLine1, addressLine2, city, state, zip, country);
	}

	public static AddressDto from(Address address) {
		return new AddressDto(address.getAddressLine1(), address.getAddressLine2(), address.getCity(),
				address.getState(), address.getZip(), address.getCountry());
	}
}
