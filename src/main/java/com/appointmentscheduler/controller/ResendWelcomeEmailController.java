package com.appointmentscheduler.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.appointmentscheduler.security.AuthenticatedPrincipal;
import com.appointmentscheduler.service.ResendWelcomeEmailService;

/** FR-020a: resend the welcome email for an account the caller has clinic-scoped access to. */
@RestController
public class ResendWelcomeEmailController {

	private final ResendWelcomeEmailService resendWelcomeEmailService;

	public ResendWelcomeEmailController(ResendWelcomeEmailService resendWelcomeEmailService) {
		this.resendWelcomeEmailService = resendWelcomeEmailService;
	}

	@PostMapping("/api/v1/users/{userId}/resend-welcome-email")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void resend(@PathVariable UUID userId, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
		resendWelcomeEmailService.resend(userId, principal);
	}
}
