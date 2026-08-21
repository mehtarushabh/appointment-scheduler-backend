package com.appointmentscheduler.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appointmentscheduler.common.BadRequestException;
import com.appointmentscheduler.common.ConflictException;
import com.appointmentscheduler.common.LeaveConflictException;
import com.appointmentscheduler.dto.AppointmentDtos.AppointmentResponse;
import com.appointmentscheduler.dto.DoctorLeaveDtos.LeaveConflictResponse;
import com.appointmentscheduler.dto.DoctorLeaveDtos.LeaveRequest;
import com.appointmentscheduler.dto.DoctorLeaveDtos.LeaveResponse;
import com.appointmentscheduler.model.Appointment;
import com.appointmentscheduler.model.AppointmentState;
import com.appointmentscheduler.model.Clinic;
import com.appointmentscheduler.model.DoctorLeave;
import com.appointmentscheduler.model.User;
import com.appointmentscheduler.repository.AppointmentRepository;
import com.appointmentscheduler.repository.ClinicRepository;
import com.appointmentscheduler.repository.DoctorLeaveRepository;
import com.appointmentscheduler.repository.UserRepository;
import com.appointmentscheduler.security.AuthenticatedPrincipal;

/** A Doctor's own leave management, including the leave/appointment-conflict flow (User Story 3, research.md #7). */
@Service
public class DoctorLeaveService {

	private final DoctorLeaveRepository doctorLeaveRepository;
	private final AppointmentRepository appointmentRepository;
	private final AppointmentService appointmentService;
	private final UserRepository userRepository;
	private final ClinicRepository clinicRepository;

	public DoctorLeaveService(DoctorLeaveRepository doctorLeaveRepository, AppointmentRepository appointmentRepository,
			AppointmentService appointmentService, UserRepository userRepository, ClinicRepository clinicRepository) {
		this.doctorLeaveRepository = doctorLeaveRepository;
		this.appointmentRepository = appointmentRepository;
		this.appointmentService = appointmentService;
		this.userRepository = userRepository;
		this.clinicRepository = clinicRepository;
	}

	public List<LeaveResponse> listForDoctor(UUID doctorId) {
		return doctorLeaveRepository.findByDoctorId(doctorId).stream().map(LeaveResponse::from).toList();
	}

	/**
	 * FR-011–FR-014, FR-028, research.md #7: validates the request, checks for an overlap with this
	 * doctor's own existing leave (409, application-level only — research.md #3, no concurrent
	 * actor), then checks for SCHEDULED-appointment conflicts. If any conflicts exist and the caller
	 * has not set confirmCancelConflicts, nothing is persisted and the conflicts are reported (409).
	 * Otherwise the leave is persisted and every confirmed conflict is cancelled via
	 * AppointmentService.cancel() — the same cancellation path User Story 4 uses.
	 */
	@Transactional
	public LeaveResponse addLeave(UUID doctorId, LeaveRequest request, AuthenticatedPrincipal principal) {
		validateRequest(request);

		boolean overlapsExistingLeave = doctorLeaveRepository.findByDoctorId(doctorId).stream()
			.filter(existing -> existing.getDate().equals(request.date()))
			.anyMatch(existing -> overlapsLeave(request, existing));
		if (overlapsExistingLeave) {
			throw new ConflictException("This leave overlaps another leave already recorded for " + request.date() + ".");
		}

		List<Appointment> conflicts = appointmentRepository.findByDoctorIdAndDateAndState(doctorId, request.date(), AppointmentState.SCHEDULED)
			.stream()
			.filter(appointment -> overlapsAppointment(request, appointment))
			.toList();

		if (!conflicts.isEmpty() && !request.confirmCancelConflicts()) {
			List<AppointmentResponse> conflictResponses = conflicts.stream().map(this::toAppointmentResponse).toList();
			throw new LeaveConflictException(
					new LeaveConflictResponse("This leave conflicts with one or more scheduled appointments.", conflictResponses));
		}

		DoctorLeave leave = doctorLeaveRepository.save(new DoctorLeave(doctorId, request.date(), request.fullDay(),
				request.fullDay() ? null : request.startTime(), request.fullDay() ? null : request.endTime()));

		for (Appointment appointment : conflicts) {
			appointmentService.cancel(appointment.getId(), principal);
		}

		return LeaveResponse.from(leave);
	}

	private static void validateRequest(LeaveRequest request) {
		if (request.date().isBefore(LocalDate.now())) {
			throw new BadRequestException("Cannot add leave for a past date.");
		}
		if (!request.fullDay()) {
			if (request.startTime() == null || request.endTime() == null || !request.startTime().isBefore(request.endTime())) {
				throw new BadRequestException("End time must be after start time.");
			}
			if (!isThirtyMinuteAligned(request.startTime()) || !isThirtyMinuteAligned(request.endTime())) {
				throw new BadRequestException("Start and end time must be 30-minute aligned.");
			}
		}
	}

	private static boolean isThirtyMinuteAligned(LocalTime time) {
		return time.getMinute() % 30 == 0 && time.getSecond() == 0 && time.getNano() == 0;
	}

	private static boolean overlapsLeave(LeaveRequest request, DoctorLeave existing) {
		if (request.fullDay() || existing.isFullDay()) {
			return true;
		}
		return overlapsRange(request.startTime(), request.endTime(), existing.getStartTime(), existing.getEndTime());
	}

	private static boolean overlapsAppointment(LeaveRequest request, Appointment appointment) {
		if (request.fullDay()) {
			return true;
		}
		LocalTime appointmentEnd = appointment.getStartTime().plusMinutes(appointment.getDurationMinutes());
		return overlapsRange(request.startTime(), request.endTime(), appointment.getStartTime(), appointmentEnd);
	}

	private static boolean overlapsRange(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
		return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
	}

	private AppointmentResponse toAppointmentResponse(Appointment appointment) {
		User patient = userRepository.findById(appointment.getPatientId()).orElseThrow();
		User doctor = userRepository.findById(appointment.getDoctorId()).orElseThrow();
		Clinic clinic = clinicRepository.findById(appointment.getClinicId()).orElseThrow();
		return AppointmentResponse.from(appointment, patient, doctor, clinic);
	}
}
