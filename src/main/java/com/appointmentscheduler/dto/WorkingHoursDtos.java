package com.appointmentscheduler.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import com.appointmentscheduler.model.ClinicWorkingHours;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class WorkingHoursDtos {

	private WorkingHoursDtos() {
	}

	public record WorkingHoursEntry(@NotNull DayOfWeek dayOfWeek, boolean isOpen, LocalTime startTime, LocalTime endTime) {

		public static WorkingHoursEntry from(ClinicWorkingHours hours) {
			return new WorkingHoursEntry(hours.getDayOfWeek(), hours.isOpen(), hours.getStartTime(), hours.getEndTime());
		}
	}

	/** FR-004/FR-007: the whole 7-day table is always submitted (and validated) together. */
	public record WorkingHoursUpdateRequest(@NotNull @Size(min = 7, max = 7) List<@Valid WorkingHoursEntry> days) {
	}
}
