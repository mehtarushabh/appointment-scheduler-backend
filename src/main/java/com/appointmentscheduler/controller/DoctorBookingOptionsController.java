package com.appointmentscheduler.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.appointmentscheduler.common.ClinicPatientAccess;
import com.appointmentscheduler.dto.AppointmentDtos.AvailableSlotsResponse;
import com.appointmentscheduler.dto.AppointmentDtos.DoctorSummaryResponse;
import com.appointmentscheduler.model.Role;
import com.appointmentscheduler.repository.ClinicPatientAssociationRepository;
import com.appointmentscheduler.repository.UserRepository;
import com.appointmentscheduler.security.AuthenticatedPrincipal;
import com.appointmentscheduler.service.AppointmentAvailabilityService;

/**
 * Patient-facing booking-options endpoints (User Story 2, research.md #1): a minimal doctor list
 * and available start times, deliberately separate from Feature 001's Clinic-Admin-only
 * `GET /clinics/{clinicId}/doctors`.
 */
@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/doctors")
public class DoctorBookingOptionsController {

	private final AppointmentAvailabilityService availabilityService;
	private final UserRepository userRepository;
	private final ClinicPatientAssociationRepository clinicPatientAssociationRepository;

	public DoctorBookingOptionsController(AppointmentAvailabilityService availabilityService, UserRepository userRepository,
			ClinicPatientAssociationRepository clinicPatientAssociationRepository) {
		this.availabilityService = availabilityService;
		this.userRepository = userRepository;
		this.clinicPatientAssociationRepository = clinicPatientAssociationRepository;
	}

	@GetMapping("/bookable")
	public List<DoctorSummaryResponse> listBookableDoctors(@PathVariable UUID clinicId,
			@AuthenticationPrincipal AuthenticatedPrincipal principal) {
		ClinicPatientAccess.requireAssociatedPatientOrClinicAdminOf(principal, clinicId, clinicPatientAssociationRepository);
		return userRepository.findByClinicIdAndRole(clinicId, Role.DOCTOR).stream().map(DoctorSummaryResponse::from).toList();
	}

	@GetMapping("/{doctorId}/available-slots")
	public AvailableSlotsResponse getAvailableSlots(@PathVariable UUID clinicId, @PathVariable UUID doctorId,
			@RequestParam LocalDate date, @RequestParam int durationMinutes, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
		ClinicPatientAccess.requireAssociatedPatientOrClinicAdminOf(principal, clinicId, clinicPatientAssociationRepository);
		return availabilityService.getAvailableSlots(clinicId, doctorId, date, durationMinutes);
	}
}
