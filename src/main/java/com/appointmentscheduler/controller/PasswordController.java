package com.appointmentscheduler.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.appointmentscheduler.common.NotFoundException;
import com.appointmentscheduler.dto.AuthDtos.ChangePasswordRequest;
import com.appointmentscheduler.model.User;
import com.appointmentscheduler.repository.UserRepository;
import com.appointmentscheduler.security.AuthenticatedPrincipal;
import com.appointmentscheduler.service.AuthService;

import jakarta.validation.Valid;

/** FR-021: any logged-in user may change their own password at any time — never mandatory. */
@RestController
public class PasswordController {

	private final AuthService authService;
	private final UserRepository userRepository;

	public PasswordController(AuthService authService, UserRepository userRepository) {
		this.authService = authService;
		this.userRepository = userRepository;
	}

	@PatchMapping("/api/v1/me/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changePassword(@Valid @RequestBody ChangePasswordRequest request,
			@AuthenticationPrincipal AuthenticatedPrincipal principal) {
		User user = userRepository.findById(principal.userId()).orElseThrow(() -> new NotFoundException("User not found."));
		authService.changePassword(user, request.currentPassword(), request.newPassword());
	}
}
