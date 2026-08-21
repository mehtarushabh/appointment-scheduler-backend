package com.appointmentscheduler.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A Doctor's own full-day or partial-day unavailability window (FR-011–FR-014, data-model.md). */
@Entity
@Table(name = "doctor_leaves")
public class DoctorLeave {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "doctor_id", nullable = false)
	private UUID doctorId;

	@Column(nullable = false)
	private LocalDate date;

	@Column(name = "full_day", nullable = false)
	private boolean fullDay;

	@Column(name = "start_time")
	private LocalTime startTime;

	@Column(name = "end_time")
	private LocalTime endTime;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected DoctorLeave() {
	}

	public DoctorLeave(UUID doctorId, LocalDate date, boolean fullDay, LocalTime startTime, LocalTime endTime) {
		this.doctorId = doctorId;
		this.date = date;
		this.fullDay = fullDay;
		this.startTime = startTime;
		this.endTime = endTime;
		this.createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public UUID getDoctorId() {
		return doctorId;
	}

	public LocalDate getDate() {
		return date;
	}

	public boolean isFullDay() {
		return fullDay;
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public LocalTime getEndTime() {
		return endTime;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
