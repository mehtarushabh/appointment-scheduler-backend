package com.appointmentscheduler.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.appointmentscheduler.common.ClinicAccess;
import com.appointmentscheduler.common.ForbiddenException;
import com.appointmentscheduler.dto.AppointmentDtos.AppointmentResponse;
import com.appointmentscheduler.dto.AppointmentDtos.BookAppointmentRequest;
import com.appointmentscheduler.model.Role;
import com.appointmentscheduler.security.AuthenticatedPrincipal;
import com.appointmentscheduler.service.AppointmentService;

import jakarta.validation.Valid;

/** Booking, self-scoped listing (User Story 2), and cancel/complete/clinic-listing (User Story 4). */
@RestController
public class AppointmentController {

	private final AppointmentService appointmentService;

	public AppointmentController(AppointmentService appointmentService) {
		this.appointmentService = appointmentService;
	}

	@PostMapping("/api/v1/appointments")
	@ResponseStatus(HttpStatus.CREATED)
	public AppointmentResponse book(@Valid @RequestBody BookAppointmentRequest request, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
		if (principal.role() != Role.PATIENT) {
			throw new ForbiddenException("Only patients can book appointments.");
		}
		return appointmentService.book(principal.userId(), request);
	}

	@GetMapping("/api/v1/me/appointments")
	public List<AppointmentResponse> listMyAppointments(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
		if (principal.role() == Role.PATIENT) {
			return appointmentService.listForPatient(principal.userId());
		}
		if (principal.role() == Role.DOCTOR) {
			return appointmentService.listForDoctor(principal.userId());
		}
		throw new ForbiddenException("Only patients and doctors have their own appointments.");
	}

	@PatchMapping("/api/v1/appointments/{appointmentId}/cancel")
	public AppointmentResponse cancel(@PathVariable UUID appointmentId, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
		return appointmentService.cancel(appointmentId, principal);
	}

	@PatchMapping("/api/v1/appointments/{appointmentId}/complete")
	public AppointmentResponse complete(@PathVariable UUID appointmentId, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
		return appointmentService.complete(appointmentId, principal);
	}

	@GetMapping("/api/v1/clinics/me/appointments")
	public List<AppointmentResponse> listClinicAppointments(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
		ClinicAccess.requireClinicAdmin(principal);
		return appointmentService.listForClinic(principal.clinicId());
	}
}
