package com.appointmentscheduler.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.appointmentscheduler.common.ClinicAccess;
import com.appointmentscheduler.dto.DoctorDtos.DoctorOnboardingRequest;
import com.appointmentscheduler.dto.UserResponse;
import com.appointmentscheduler.security.AuthenticatedPrincipal;
import com.appointmentscheduler.service.DoctorService;

import jakarta.validation.Valid;

/** Clinic-Admin-only, own-clinic Doctor onboarding (User Story 2). */
@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/doctors")
public class DoctorController {

	private final DoctorService doctorService;

	public DoctorController(DoctorService doctorService) {
		this.doctorService = doctorService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public UserResponse onboardDoctor(@PathVariable UUID clinicId, @Valid @RequestBody DoctorOnboardingRequest request,
			@AuthenticationPrincipal AuthenticatedPrincipal principal) {
		ClinicAccess.requireClinicAdminOf(principal, clinicId);
		return doctorService.onboardDoctor(clinicId, request);
	}

	@GetMapping
	public List<UserResponse> listDoctors(@PathVariable UUID clinicId, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
		ClinicAccess.requireClinicAdminOf(principal, clinicId);
		return doctorService.listDoctorsForClinic(clinicId);
	}
}
