package com.appointmentscheduler.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * The many-to-many bridge between Clinics and Patients (data-model.md "Entity:
 * ClinicPatientAssociation") — a Patient may belong to more than one Clinic (FR-013), unlike
 * Clinic Admins/Doctors who belong to exactly one via User.clinicId.
 */
@Entity
@Table(name = "clinic_patient_associations", uniqueConstraints = @UniqueConstraint(columnNames = { "clinic_id", "patient_id" }))
public class ClinicPatientAssociation {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "clinic_id", nullable = false)
	private UUID clinicId;

	@Column(name = "patient_id", nullable = false)
	private UUID patientId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected ClinicPatientAssociation() {
	}

	public ClinicPatientAssociation(UUID clinicId, UUID patientId) {
		this.clinicId = clinicId;
		this.patientId = patientId;
		this.createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public UUID getClinicId() {
		return clinicId;
	}

	public UUID getPatientId() {
		return patientId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
