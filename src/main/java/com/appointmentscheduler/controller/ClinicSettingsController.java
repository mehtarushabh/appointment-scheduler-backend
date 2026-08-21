package com.appointmentscheduler.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appointmentscheduler.common.ClinicAccess;
import com.appointmentscheduler.common.ClinicPatientAccess;
import com.appointmentscheduler.dto.ClinicDtos.ClinicProfileUpdateRequest;
import com.appointmentscheduler.dto.ClinicDtos.ClinicResponse;
import com.appointmentscheduler.dto.WorkingHoursDtos.WorkingHoursEntry;
import com.appointmentscheduler.dto.WorkingHoursDtos.WorkingHoursUpdateRequest;
import com.appointmentscheduler.repository.ClinicPatientAssociationRepository;
import com.appointmentscheduler.security.AuthenticatedPrincipal;
import com.appointmentscheduler.service.ClinicSettingsService;

import jakarta.validation.Valid;

/**
 * Clinic-Admin-only, own-clinic profile and working-hours management (User Story 1). The
 * working-hours GET is additionally available to an associated Patient (User Story 2's booking
 * calendar) since that response carries no PII; a Patient isn't scoped to one clinic, so that
 * endpoint alone keeps clinicId in the path.
 */
@RestController
@RequestMapping("/api/v1/clinics")
public class ClinicSettingsController {

	private final ClinicSettingsService clinicSettingsService;
	private final ClinicPatientAssociationRepository clinicPatientAssociationRepository;

	public ClinicSettingsController(ClinicSettingsService clinicSettingsService,
			ClinicPatientAssociationRepository clinicPatientAssociationRepository) {
		this.clinicSettingsService = clinicSettingsService;
		this.clinicPatientAssociationRepository = clinicPatientAssociationRepository;
	}

	@GetMapping("/me")
	public ClinicResponse getProfile(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
		ClinicAccess.requireClinicAdmin(principal);
		return clinicSettingsService.getProfile(principal.clinicId());
	}

	@PatchMapping("/me")
	public ClinicResponse updateProfile(@Valid @RequestBody ClinicProfileUpdateRequest request,
			@AuthenticationPrincipal AuthenticatedPrincipal principal) {
		ClinicAccess.requireClinicAdmin(principal);
		return clinicSettingsService.updateProfile(principal.clinicId(), request);
	}

	@GetMapping("/{clinicId}/working-hours")
	public List<WorkingHoursEntry> getWorkingHours(@PathVariable UUID clinicId, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
		ClinicPatientAccess.requireAssociatedPatientOrClinicAdminOf(principal, clinicId, clinicPatientAssociationRepository);
		return clinicSettingsService.getWorkingHours(clinicId);
	}

	@PutMapping("/me/working-hours")
	public List<WorkingHoursEntry> replaceWorkingHours(@Valid @RequestBody WorkingHoursUpdateRequest request,
			@AuthenticationPrincipal AuthenticatedPrincipal principal) {
		ClinicAccess.requireClinicAdmin(principal);
		return clinicSettingsService.replaceWorkingHours(principal.clinicId(), request);
	}
}
