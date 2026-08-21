package com.appointmentscheduler.model;

import java.time.DayOfWeek;
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

/**
 * One row per (Clinic, day of week) — always exactly 7 rows per Clinic, seeded at clinic creation
 * (research.md #8) and bulk-replaced whenever a Clinic Admin saves the working-hours table
 * (FR-007, data-model.md).
 */
@Entity
@Table(name = "clinic_working_hours")
public class ClinicWorkingHours {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "clinic_id", nullable = false)
	private UUID clinicId;

	@Enumerated(EnumType.STRING)
	@Column(name = "day_of_week", nullable = false)
	private DayOfWeek dayOfWeek;

	@Column(name = "is_open", nullable = false)
	private boolean isOpen;

	@Column(name = "start_time")
	private LocalTime startTime;

	@Column(name = "end_time")
	private LocalTime endTime;

	protected ClinicWorkingHours() {
	}

	public ClinicWorkingHours(UUID clinicId, DayOfWeek dayOfWeek, boolean isOpen, LocalTime startTime, LocalTime endTime) {
		this.clinicId = clinicId;
		this.dayOfWeek = dayOfWeek;
		this.isOpen = isOpen;
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public UUID getId() {
		return id;
	}

	public UUID getClinicId() {
		return clinicId;
	}

	public DayOfWeek getDayOfWeek() {
		return dayOfWeek;
	}

	public boolean isOpen() {
		return isOpen;
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public LocalTime getEndTime() {
		return endTime;
	}

	/** FR-007: a day's open/closed state and hours are always replaced together, never partially. */
	public void replace(boolean isOpen, LocalTime startTime, LocalTime endTime) {
		this.isOpen = isOpen;
		this.startTime = startTime;
		this.endTime = endTime;
	}
}
