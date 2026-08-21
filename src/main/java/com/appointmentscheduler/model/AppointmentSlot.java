package com.appointmentscheduler.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Internal 30-minute-unit occupancy ledger — never returned by any endpoint (data-model.md,
 * research.md #3). The {@code UNIQUE(doctor_id, date, slot_start_time)} constraint on its table is
 * the actual database-level guarantee behind FR-020/SC-003, not the application-level check alone.
 */
@Entity
@Table(name = "appointment_slots")
public class AppointmentSlot {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "appointment_id", nullable = false)
	private UUID appointmentId;

	@Column(name = "doctor_id", nullable = false)
	private UUID doctorId;

	@Column(nullable = false)
	private LocalDate date;

	@Column(name = "slot_start_time", nullable = false)
	private LocalTime slotStartTime;

	protected AppointmentSlot() {
	}

	public AppointmentSlot(UUID appointmentId, UUID doctorId, LocalDate date, LocalTime slotStartTime) {
		this.appointmentId = appointmentId;
		this.doctorId = doctorId;
		this.date = date;
		this.slotStartTime = slotStartTime;
	}

	public UUID getId() {
		return id;
	}

	public UUID getAppointmentId() {
		return appointmentId;
	}

	public UUID getDoctorId() {
		return doctorId;
	}

	public LocalDate getDate() {
		return date;
	}

	public LocalTime getSlotStartTime() {
		return slotStartTime;
	}
}
