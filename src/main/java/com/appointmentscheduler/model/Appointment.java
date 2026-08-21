package com.appointmentscheduler.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.appointmentscheduler.common.ConflictException;

/** A Patient's booking with a Doctor at a Clinic (FR-019–FR-029, data-model.md). */
@Entity
@Table(name = "appointments")
public class Appointment {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "patient_id", nullable = false)
	private UUID patientId;

	@Column(name = "doctor_id", nullable = false)
	private UUID doctorId;

	@Column(name = "clinic_id", nullable = false)
	private UUID clinicId;

	@Column(nullable = false)
	private LocalDate date;

	@Column(name = "start_time", nullable = false)
	private LocalTime startTime;

	@Column(name = "duration_minutes", nullable = false)
	private short durationMinutes;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AppointmentState state;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Appointment() {
	}

	public Appointment(UUID patientId, UUID doctorId, UUID clinicId, LocalDate date, LocalTime startTime, short durationMinutes) {
		this.patientId = patientId;
		this.doctorId = doctorId;
		this.clinicId = clinicId;
		this.date = date;
		this.startTime = startTime;
		this.durationMinutes = durationMinutes;
		this.state = AppointmentState.SCHEDULED;
		this.createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public UUID getPatientId() {
		return patientId;
	}

	public UUID getDoctorId() {
		return doctorId;
	}

	public UUID getClinicId() {
		return clinicId;
	}

	public LocalDate getDate() {
		return date;
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public short getDurationMinutes() {
		return durationMinutes;
	}

	public AppointmentState getState() {
		return state;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	/** FR-027: only a SCHEDULED appointment may transition; CANCELLED/COMPLETED are both terminal. */
	public void cancel() {
		requireScheduled();
		this.state = AppointmentState.CANCELLED;
	}

	public void complete() {
		requireScheduled();
		this.state = AppointmentState.COMPLETED;
	}

	private void requireScheduled() {
		if (state != AppointmentState.SCHEDULED) {
			throw new ConflictException("This appointment is no longer SCHEDULED.");
		}
	}
}
