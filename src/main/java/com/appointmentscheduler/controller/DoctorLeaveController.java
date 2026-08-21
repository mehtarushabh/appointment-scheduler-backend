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

import com.appointmentscheduler.common.ForbiddenException;
import com.appointmentscheduler.dto.DoctorLeaveDtos.LeaveRequest;
import com.appointmentscheduler.dto.DoctorLeaveDtos.LeaveResponse;
import com.appointmentscheduler.model.Role;
import com.appointmentscheduler.security.AuthenticatedPrincipal;
import com.appointmentscheduler.service.DoctorLeaveService;

import jakarta.validation.Valid;

/** Self-scoped, like PasswordController/MeController — the doctor is always derived from the principal. */
@RestController
@RequestMapping("/api/v1/me/leaves")
public class DoctorLeaveController {

	private final DoctorLeaveService doctorLeaveService;

	public DoctorLeaveController(DoctorLeaveService doctorLeaveService) {
		this.doctorLeaveService = doctorLeaveService;
	}

	@GetMapping
	public List<LeaveResponse> listMyLeaves(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
		requireDoctor(principal);
		return doctorLeaveService.listForDoctor(principal.userId());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public LeaveResponse addLeave(@Valid @RequestBody LeaveRequest request, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
		requireDoctor(principal);
		return doctorLeaveService.addLeave(principal.userId(), request, principal);
	}

	private void requireDoctor(AuthenticatedPrincipal principal) {
		if (principal.role() != Role.DOCTOR) {
			throw new ForbiddenException("Only doctors have their own leave.");
		}
	}
}
