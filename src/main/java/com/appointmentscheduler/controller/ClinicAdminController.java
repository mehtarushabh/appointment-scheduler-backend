package com.appointmentscheduler.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.appointmentscheduler.common.ClinicAccess;
import com.appointmentscheduler.dto.UserOnboardingRequest;
import com.appointmentscheduler.dto.UserResponse;
import com.appointmentscheduler.security.AuthenticatedPrincipal;
import com.appointmentscheduler.service.ClinicAdminService;

import jakarta.validation.Valid;

/** Clinic-Admin-only, own-clinic additional Clinic Admin onboarding (User Story 4). */
@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/clinic-admins")
public class ClinicAdminController {

	private final ClinicAdminService clinicAdminService;

	public ClinicAdminController(ClinicAdminService clinicAdminService) {
		this.clinicAdminService = clinicAdminService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public UserResponse onboardClinicAdmin(@PathVariable UUID clinicId, @Valid @RequestBody UserOnboardingRequest request,
			@AuthenticationPrincipal AuthenticatedPrincipal principal) {
		ClinicAccess.requireClinicAdminOf(principal, clinicId);
		return clinicAdminService.onboardClinicAdmin(clinicId, request);
	}
}
