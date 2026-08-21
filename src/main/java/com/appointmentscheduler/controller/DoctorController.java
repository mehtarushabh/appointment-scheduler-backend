package com.appointmentscheduler.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/v1/clinics/me/doctors")
public class DoctorController {

	private final DoctorService doctorService;

	public DoctorController(DoctorService doctorService) {
		this.doctorService = doctorService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public UserResponse onboardDoctor(@Valid @RequestBody DoctorOnboardingRequest request,
			@AuthenticationPrincipal AuthenticatedPrincipal principal) {
		ClinicAccess.requireClinicAdmin(principal);
		return doctorService.onboardDoctor(principal.clinicId(), request);
	}

	@GetMapping
	public List<UserResponse> listDoctors(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
		ClinicAccess.requireClinicAdmin(principal);
		return doctorService.listDoctorsForClinic(principal.clinicId());
	}
}
