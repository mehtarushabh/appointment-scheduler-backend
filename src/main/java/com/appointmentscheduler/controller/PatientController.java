package com.appointmentscheduler.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appointmentscheduler.common.ClinicAccess;
import com.appointmentscheduler.common.ForbiddenException;
import com.appointmentscheduler.dto.ClinicDtos.ClinicResponse;
import com.appointmentscheduler.dto.UserOnboardingRequest;
import com.appointmentscheduler.dto.UserResponse;
import com.appointmentscheduler.model.Role;
import com.appointmentscheduler.security.AuthenticatedPrincipal;
import com.appointmentscheduler.service.PatientService;
import com.appointmentscheduler.service.PatientService.PatientOnboardingResult;

import jakarta.validation.Valid;

/** Clinic-Admin-only, own-clinic Patient onboarding, plus a Patient's own clinic list (User Story 3). */
@RestController
public class PatientController {

	private final PatientService patientService;

	public PatientController(PatientService patientService) {
		this.patientService = patientService;
	}

	@PostMapping("/api/v1/clinics/me/patients")
	public ResponseEntity<UserResponse> onboardOrLinkPatient(@Valid @RequestBody UserOnboardingRequest request,
			@AuthenticationPrincipal AuthenticatedPrincipal principal) {
		ClinicAccess.requireClinicAdmin(principal);
		PatientOnboardingResult result = patientService.onboardOrLinkPatient(principal.clinicId(), request);
		HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
		return ResponseEntity.status(status).body(result.patient());
	}

	@GetMapping("/api/v1/clinics/me/patients")
	public List<UserResponse> listPatients(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
		ClinicAccess.requireClinicAdmin(principal);
		return patientService.listPatientsForClinic(principal.clinicId());
	}

	@GetMapping("/api/v1/me/clinics")
	public List<ClinicResponse> listMyClinics(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
		if (principal.role() != Role.PATIENT) {
			throw new ForbiddenException("Only patients have a clinic list.");
		}
		return patientService.listClinicsForPatient(principal.userId());
	}
}
