package com.appointmentscheduler.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.appointmentscheduler.dto.ClinicDtos.ClinicOnboardingRequest;
import com.appointmentscheduler.dto.ClinicDtos.ClinicResponse;
import com.appointmentscheduler.service.ClinicService;

import jakarta.validation.Valid;

/** System-Admin-only clinic onboarding (User Story 1: FR-003, FR-016, FR-018, FR-018a). */
@RestController
@RequestMapping("/api/v1/clinics")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class ClinicController {

	private final ClinicService clinicService;

	public ClinicController(ClinicService clinicService) {
		this.clinicService = clinicService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ClinicResponse onboardClinic(@Valid @RequestBody ClinicOnboardingRequest request) {
		return clinicService.onboardClinic(request);
	}

	@GetMapping
	public List<ClinicResponse> listClinics() {
		return clinicService.listClinics();
	}
}
