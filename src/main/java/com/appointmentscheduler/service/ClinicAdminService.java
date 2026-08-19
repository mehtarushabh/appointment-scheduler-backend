package com.appointmentscheduler.service;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appointmentscheduler.common.ConflictException;
import com.appointmentscheduler.dto.UserOnboardingRequest;
import com.appointmentscheduler.dto.UserResponse;
import com.appointmentscheduler.model.Role;
import com.appointmentscheduler.model.User;
import com.appointmentscheduler.repository.UserRepository;
import com.appointmentscheduler.security.TemporaryPasswordGenerator;

/** Onboards an additional Clinic Admin for the caller's own clinic (User Story 4: FR-005). */
@Service
public class ClinicAdminService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final TemporaryPasswordGenerator passwordGenerator;
	private final WelcomeEmailService welcomeEmailService;

	public ClinicAdminService(UserRepository userRepository, PasswordEncoder passwordEncoder,
			TemporaryPasswordGenerator passwordGenerator, WelcomeEmailService welcomeEmailService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.passwordGenerator = passwordGenerator;
		this.welcomeEmailService = welcomeEmailService;
	}

	@Transactional
	public UserResponse onboardClinicAdmin(UUID clinicId, UserOnboardingRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw new ConflictException("A user with this email already exists.");
		}
		String temporaryPassword = passwordGenerator.generate();
		User clinicAdmin = userRepository.save(new User(request.firstName(), request.lastName(), request.email(),
				passwordEncoder.encode(temporaryPassword), request.dateOfBirth(), request.address().toModel(),
				Role.CLINIC_ADMIN, clinicId, null));

		welcomeEmailService.sendWelcomeEmail(clinicAdmin, temporaryPassword);
		return UserResponse.from(clinicAdmin);
	}
}
