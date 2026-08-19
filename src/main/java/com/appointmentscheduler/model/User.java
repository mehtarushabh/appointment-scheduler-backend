package com.appointmentscheduler.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Every login-capable account (System Admin, Clinic Admin, Doctor, Patient) — one row per
 * account, per data-model.md "Entity: User" / research.md #1 (single table with a role
 * discriminator instead of per-role tables).
 */
@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "first_name", nullable = false)
	private String firstName;

	@Column(name = "last_name", nullable = false)
	private String lastName;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "date_of_birth", nullable = false)
	private LocalDate dateOfBirth;

	@Embedded
	private Address address;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role;

	/** Required for CLINIC_ADMIN/DOCTOR; null for SYSTEM_ADMIN/PATIENT (FR-011, FR-012). */
	@Column(name = "clinic_id")
	private UUID clinicId;

	/** Required for DOCTOR only (FR-019); null otherwise. */
	@Column
	private String specialty;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected User() {
	}

	public User(String firstName, String lastName, String email, String passwordHash, LocalDate dateOfBirth,
			Address address, Role role, UUID clinicId, String specialty) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.passwordHash = passwordHash;
		this.dateOfBirth = dateOfBirth;
		this.address = address;
		this.role = role;
		this.clinicId = clinicId;
		this.specialty = specialty;
		this.createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public LocalDate getDateOfBirth() {
		return dateOfBirth;
	}

	public Address getAddress() {
		return address;
	}

	public Role getRole() {
		return role;
	}

	public UUID getClinicId() {
		return clinicId;
	}

	public String getSpecialty() {
		return specialty;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
