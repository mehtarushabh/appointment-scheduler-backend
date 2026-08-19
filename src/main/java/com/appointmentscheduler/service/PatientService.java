package com.appointmentscheduler.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appointmentscheduler.common.ConflictException;
import com.appointmentscheduler.dto.ClinicDtos.ClinicResponse;
import com.appointmentscheduler.dto.UserOnboardingRequest;
import com.appointmentscheduler.dto.UserResponse;
import com.appointmentscheduler.model.Clinic;
import com.appointmentscheduler.model.ClinicPatientAssociation;
import com.appointmentscheduler.model.Role;
import com.appointmentscheduler.model.User;
import com.appointmentscheduler.repository.ClinicPatientAssociationRepository;
import com.appointmentscheduler.repository.ClinicRepository;
import com.appointmentscheduler.repository.UserRepository;
import com.appointmentscheduler.security.TemporaryPasswordGenerator;

/**
 * Onboards Patients, linking an existing Patient (matched by email) to an additional Clinic
 * instead of duplicating them (User Story 3: FR-006, FR-007, FR-008, FR-013).
 */
@Service
public class PatientService {

	public record PatientOnboardingResult(UserResponse patient, boolean created) {
	}

	private final UserRepository userRepository;
	private final ClinicRepository clinicRepository;
	private final ClinicPatientAssociationRepository associationRepository;
	private final PasswordEncoder passwordEncoder;
	private final TemporaryPasswordGenerator passwordGenerator;
	private final WelcomeEmailService welcomeEmailService;

	public PatientService(UserRepository userRepository, ClinicRepository clinicRepository,
			ClinicPatientAssociationRepository associationRepository, PasswordEncoder passwordEncoder,
			TemporaryPasswordGenerator passwordGenerator, WelcomeEmailService welcomeEmailService) {
		this.userRepository = userRepository;
		this.clinicRepository = clinicRepository;
		this.associationRepository = associationRepository;
		this.passwordEncoder = passwordEncoder;
		this.passwordGenerator = passwordGenerator;
		this.welcomeEmailService = welcomeEmailService;
	}

	@Transactional
	public PatientOnboardingResult onboardOrLinkPatient(UUID clinicId, UserOnboardingRequest request) {
		Optional<User> existing = userRepository.findByEmail(request.email());

		User patient;
		boolean created;
		if (existing.isPresent()) {
			User existingUser = existing.get();
			if (existingUser.getRole() != Role.PATIENT) {
				throw new ConflictException("This email belongs to an existing account that is not a Patient.");
			}
			patient = existingUser;
			created = false;
		} else {
			String temporaryPassword = passwordGenerator.generate();
			patient = userRepository.save(new User(request.firstName(), request.lastName(), request.email(),
					passwordEncoder.encode(temporaryPassword), request.dateOfBirth(), request.address().toModel(),
					Role.PATIENT, null, null));
			welcomeEmailService.sendWelcomeEmail(patient, temporaryPassword);
			created = true;
		}

		if (!associationRepository.existsByClinicIdAndPatientId(clinicId, patient.getId())) {
			associationRepository.save(new ClinicPatientAssociation(clinicId, patient.getId()));
		}

		return new PatientOnboardingResult(UserResponse.from(patient), created);
	}

	public List<UserResponse> listPatientsForClinic(UUID clinicId) {
		return associationRepository.findByClinicId(clinicId).stream()
			.map(assoc -> userRepository.findById(assoc.getPatientId()).orElseThrow())
			.map(UserResponse::from)
			.toList();
	}

	public List<ClinicResponse> listClinicsForPatient(UUID patientId) {
		return associationRepository.findByPatientId(patientId).stream()
			.map(assoc -> clinicRepository.findById(assoc.getClinicId()).orElseThrow())
			.map(PatientService::toClinicResponseWithoutAdmin)
			.toList();
	}

	private static ClinicResponse toClinicResponseWithoutAdmin(Clinic clinic) {
		return ClinicResponse.from(clinic, null);
	}
}
