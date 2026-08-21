package com.appointmentscheduler.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.appointmentscheduler.dto.AppointmentDtos.AvailableSlotsResponse;
import com.appointmentscheduler.model.Appointment;
import com.appointmentscheduler.model.AppointmentState;
import com.appointmentscheduler.model.ClinicWorkingHours;
import com.appointmentscheduler.model.DoctorLeave;
import com.appointmentscheduler.repository.AppointmentRepository;
import com.appointmentscheduler.repository.ClinicWorkingHoursRepository;
import com.appointmentscheduler.repository.DoctorLeaveRepository;

/**
 * Computes a doctor's bookable start times on-the-fly (research.md #2): the clinic's working-hours
 * window for the date, minus the doctor's leave, minus their other SCHEDULED appointments.
 */
@Service
public class AppointmentAvailabilityService {

	private static final int SLOT_MINUTES = 30;

	private final ClinicWorkingHoursRepository clinicWorkingHoursRepository;
	private final DoctorLeaveRepository doctorLeaveRepository;
	private final AppointmentRepository appointmentRepository;

	public AppointmentAvailabilityService(ClinicWorkingHoursRepository clinicWorkingHoursRepository,
			DoctorLeaveRepository doctorLeaveRepository, AppointmentRepository appointmentRepository) {
		this.clinicWorkingHoursRepository = clinicWorkingHoursRepository;
		this.doctorLeaveRepository = doctorLeaveRepository;
		this.appointmentRepository = appointmentRepository;
	}

	public AvailableSlotsResponse getAvailableSlots(UUID clinicId, UUID doctorId, LocalDate date, int durationMinutes) {
		List<LocalTime[]> free = freeIntervals(clinicId, doctorId, date);
		List<LocalTime> startTimes = new ArrayList<>();
		for (LocalTime[] interval : free) {
			LocalTime candidate = interval[0];
			while (!candidate.plusMinutes(durationMinutes).isAfter(interval[1])) {
				startTimes.add(candidate);
				candidate = candidate.plusMinutes(SLOT_MINUTES);
			}
		}
		return new AvailableSlotsResponse(date, durationMinutes, startTimes);
	}

	/** FR-020: the upfront half of the double-booking guard — the AppointmentSlot UNIQUE constraint is the other half. */
	public boolean isAvailable(UUID clinicId, UUID doctorId, LocalDate date, LocalTime startTime, int durationMinutes) {
		LocalTime endTime = startTime.plusMinutes(durationMinutes);
		return freeIntervals(clinicId, doctorId, date).stream()
			.anyMatch(interval -> !startTime.isBefore(interval[0]) && !endTime.isAfter(interval[1]));
	}

	private List<LocalTime[]> freeIntervals(UUID clinicId, UUID doctorId, LocalDate date) {
		DayOfWeek dayOfWeek = date.getDayOfWeek();
		ClinicWorkingHours today = clinicWorkingHoursRepository.findByClinicId(clinicId).stream()
			.filter(hours -> hours.getDayOfWeek() == dayOfWeek)
			.findFirst()
			.orElse(null);
		if (today == null || !today.isOpen()) {
			return List.of();
		}

		List<LocalTime[]> free = new ArrayList<>();
		free.add(new LocalTime[] { today.getStartTime(), today.getEndTime() });

		for (DoctorLeave leave : doctorLeaveRepository.findByDoctorId(doctorId)) {
			if (!leave.getDate().equals(date)) {
				continue;
			}
			if (leave.isFullDay()) {
				return List.of();
			}
			free = subtract(free, leave.getStartTime(), leave.getEndTime());
		}

		for (Appointment appointment : appointmentRepository.findByDoctorIdAndDateAndState(doctorId, date, AppointmentState.SCHEDULED)) {
			LocalTime appointmentEnd = appointment.getStartTime().plusMinutes(appointment.getDurationMinutes());
			free = subtract(free, appointment.getStartTime(), appointmentEnd);
		}

		return free;
	}

	private static List<LocalTime[]> subtract(List<LocalTime[]> free, LocalTime blockedStart, LocalTime blockedEnd) {
		List<LocalTime[]> result = new ArrayList<>();
		for (LocalTime[] interval : free) {
			LocalTime start = interval[0];
			LocalTime end = interval[1];
			boolean noOverlap = !blockedStart.isBefore(end) || !blockedEnd.isAfter(start);
			if (noOverlap) {
				result.add(interval);
				continue;
			}
			if (blockedStart.isAfter(start)) {
				result.add(new LocalTime[] { start, blockedStart });
			}
			if (blockedEnd.isBefore(end)) {
				result.add(new LocalTime[] { blockedEnd, end });
			}
		}
		return result;
	}
}
