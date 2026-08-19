package com.appointmentscheduler.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;

/**
 * Shared value type embedded directly into User and Clinic (data-model.md
 * "Value Object: Address"; research.md #8) — not a standalone table.
 */
@Embeddable
public class Address {

	@NotBlank
	@Column(name = "address_line1", nullable = false)
	private String addressLine1;

	@Column(name = "address_line2")
	private String addressLine2;

	@NotBlank
	@Column(nullable = false)
	private String city;

	@NotBlank
	@Column(nullable = false)
	private String state;

	@NotBlank
	@Column(nullable = false)
	private String zip;

	@NotBlank
	@Column(nullable = false)
	private String country;

	protected Address() {
	}

	public Address(String addressLine1, String addressLine2, String city, String state, String zip, String country) {
		this.addressLine1 = addressLine1;
		this.addressLine2 = addressLine2;
		this.city = city;
		this.state = state;
		this.zip = zip;
		this.country = country;
	}

	public String getAddressLine1() {
		return addressLine1;
	}

	public String getAddressLine2() {
		return addressLine2;
	}

	public String getCity() {
		return city;
	}

	public String getState() {
		return state;
	}

	public String getZip() {
		return zip;
	}

	public String getCountry() {
		return country;
	}
}
