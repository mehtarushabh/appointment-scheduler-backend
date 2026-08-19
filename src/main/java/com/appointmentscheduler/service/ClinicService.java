package com.appointmentscheduler.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appointmentscheduler.common.ConflictException;
import com.appointmentscheduler.dto.ClinicDtos.ClinicOnboardingRequest;
import com.appointmentscheduler.dto.ClinicDtos.ClinicResponse;
import com.appointmentscheduler.dto.UserOnboardingRequest;
import com.appointmentscheduler.dto.UserResponse;
import com.appointmentscheduler.model.Clinic;
import com.appointmentscheduler.model.Role;
import com.appointmentscheduler.model.User;
import com.appointmentscheduler.repository.ClinicRepository;
import com.appointmentscheduler.repository.UserRepository;
import com.appointmentscheduler.security.TemporaryPasswordGenerator;

/**
 * Onboards a Clinic together with its first Clinic Admin (FR-003, FR-016, FR-018, FR-018a) and
 * lists all onboarded Clinics for a System Admin (User Story 1).
 */
@Service
public class ClinicService {

	private final ClinicRepository clinicRepository;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final TemporaryPasswordGenerator passwordGenerator;
	private final WelcomeEmailService welcomeEmailService;

	public ClinicService(ClinicRepository clinicRepository, UserRepository userRepository,
			PasswordEncoder passwordEncoder, TemporaryPasswordGenerator passwordGenerator,
			WelcomeEmailService welcomeEmailService) {
		this.clinicRepository = clinicRepository;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.passwordGenerator = passwordGenerator;
		this.welcomeEmailService = welcomeEmailService;
	}

	/** FR-016: the Clinic and its first Clinic Admin are created atomically, or neither is. */
	@Transactional
	public ClinicResponse onboardClinic(ClinicOnboardingRequest request) {
		if (clinicRepository.existsByRegisteredId(request.registeredId())) {
			throw new ConflictException("A clinic with this Registered ID already exists.");
		}
		UserOnboardingRequest adminRequest = request.firstClinicAdmin();
		if (userRepository.existsByEmail(adminRequest.email())) {
			throw new ConflictException("A user with this email already exists.");
		}

		Clinic clinic = clinicRepository.save(new Clinic(request.name(), request.address().toModel(), request.registeredId()));

		String temporaryPassword = passwordGenerator.generate();
		User clinicAdmin = userRepository.save(new User(adminRequest.firstName(), adminRequest.lastName(),
				adminRequest.email(), passwordEncoder.encode(temporaryPassword), adminRequest.dateOfBirth(),
				adminRequest.address().toModel(), Role.CLINIC_ADMIN, clinic.getId(), null));

		welcomeEmailService.sendWelcomeEmail(clinicAdmin, temporaryPassword);

		return toClinicResponse(clinic, clinicAdmin);
	}

	public List<ClinicResponse> listClinics() {
		return clinicRepository.findAll().stream()
			.map(clinic -> {
				User admin = userRepository.findByClinicIdAndRole(clinic.getId(), Role.CLINIC_ADMIN).stream()
					.findFirst()
					.orElse(null);
				return toClinicResponse(clinic, admin);
			})
			.toList();
	}

	private static ClinicResponse toClinicResponse(Clinic clinic, User admin) {
		UserResponse adminResponse = admin != null ? UserResponse.from(admin) : null;
		return ClinicResponse.from(clinic, adminResponse);
	}
}
