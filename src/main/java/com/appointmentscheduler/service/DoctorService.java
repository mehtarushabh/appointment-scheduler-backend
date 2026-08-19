package com.appointmentscheduler.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appointmentscheduler.common.ConflictException;
import com.appointmentscheduler.dto.DoctorDtos.DoctorOnboardingRequest;
import com.appointmentscheduler.dto.UserResponse;
import com.appointmentscheduler.model.Role;
import com.appointmentscheduler.model.User;
import com.appointmentscheduler.repository.UserRepository;
import com.appointmentscheduler.security.TemporaryPasswordGenerator;

/** Onboards Doctors into a Clinic Admin's own clinic (User Story 2: FR-004, FR-019). */
@Service
public class DoctorService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final TemporaryPasswordGenerator passwordGenerator;
	private final WelcomeEmailService welcomeEmailService;

	public DoctorService(UserRepository userRepository, PasswordEncoder passwordEncoder,
			TemporaryPasswordGenerator passwordGenerator, WelcomeEmailService welcomeEmailService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.passwordGenerator = passwordGenerator;
		this.welcomeEmailService = welcomeEmailService;
	}

	@Transactional
	public UserResponse onboardDoctor(UUID clinicId, DoctorOnboardingRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw new ConflictException("A user with this email already exists.");
		}
		String temporaryPassword = passwordGenerator.generate();
		User doctor = userRepository.save(new User(request.firstName(), request.lastName(), request.email(),
				passwordEncoder.encode(temporaryPassword), request.dateOfBirth(), request.address().toModel(),
				Role.DOCTOR, clinicId, request.specialty()));

		welcomeEmailService.sendWelcomeEmail(doctor, temporaryPassword);
		return UserResponse.from(doctor);
	}

	public List<UserResponse> listDoctorsForClinic(UUID clinicId) {
		return userRepository.findByClinicIdAndRole(clinicId, Role.DOCTOR).stream().map(UserResponse::from).toList();
	}
}
