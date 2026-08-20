package com.appointmentscheduler.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appointmentscheduler.common.NotFoundException;
import com.appointmentscheduler.dto.MeResponse;
import com.appointmentscheduler.model.User;
import com.appointmentscheduler.repository.UserRepository;
import com.appointmentscheduler.security.AuthenticatedPrincipal;
import com.appointmentscheduler.service.AuthService;

/** GET /me: the current user's own display profile (feature 003), separate from login (research.md #1). */
@RestController
public class MeController {

	private final AuthService authService;
	private final UserRepository userRepository;

	public MeController(AuthService authService, UserRepository userRepository) {
		this.authService = authService;
		this.userRepository = userRepository;
	}

	@GetMapping("/api/v1/me")
	public MeResponse getMe(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
		User user = userRepository.findById(principal.userId()).orElseThrow(() -> new NotFoundException("User not found."));
		return authService.getProfile(user);
	}
}
