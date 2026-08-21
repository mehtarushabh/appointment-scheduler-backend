package com.appointmentscheduler.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "clinics")
public class Clinic {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private String name;

	@Embedded
	private Address address;

	/** Immutable after creation (FR-003) — deliberately has no setter. */
	@Column(name = "registered_id", nullable = false, unique = true)
	private String registeredId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Clinic() {
	}

	public Clinic(String name, Address address, String registeredId) {
		this.name = name;
		this.address = address;
		this.registeredId = registeredId;
		this.createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public String getRegisteredId() {
		return registeredId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
