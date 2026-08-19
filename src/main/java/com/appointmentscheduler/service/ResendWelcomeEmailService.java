package com.appointmentscheduler.service;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appointmentscheduler.common.ForbiddenException;
import com.appointmentscheduler.common.NotFoundException;
import com.appointmentscheduler.model.Role;
import com.appointmentscheduler.model.User;
import com.appointmentscheduler.repository.ClinicPatientAssociationRepository;
import com.appointmentscheduler.repository.UserRepository;
import com.appointmentscheduler.security.AuthenticatedPrincipal;
import com.appointmentscheduler.security.TemporaryPasswordGenerator;

/**
 * FR-020a: resends the welcome email for an account the caller has clinic-scoped access to. The
 * original plaintext password is never stored, so a resend issues and emails a fresh one.
 */
@Service
public class ResendWelcomeEmailService {

	private final UserRepository userRepository;
	private final ClinicPatientAssociationRepository associationRepository;
	private final PasswordEncoder passwordEncoder;
	private final TemporaryPasswordGenerator passwordGenerator;
	private final WelcomeEmailService welcomeEmailService;

	public ResendWelcomeEmailService(UserRepository userRepository,
			ClinicPatientAssociationRepository associationRepository, PasswordEncoder passwordEncoder,
			TemporaryPasswordGenerator passwordGenerator, WelcomeEmailService welcomeEmailService) {
		this.userRepository = userRepository;
		this.associationRepository = associationRepository;
		this.passwordEncoder = passwordEncoder;
		this.passwordGenerator = passwordGenerator;
		this.welcomeEmailService = welcomeEmailService;
	}

	@Transactional
	public void resend(UUID targetUserId, AuthenticatedPrincipal caller) {
		User target = userRepository.findById(targetUserId).orElseThrow(() -> new NotFoundException("No such user."));

		if (caller.role() == Role.SYSTEM_ADMIN) {
			// System Admin may resend for any Clinic Admin they could have onboarded.
			if (target.getRole() != Role.CLINIC_ADMIN) {
				throw new ForbiddenException("System Admin may only resend for Clinic Admins.");
			}
		} else if (caller.role() == Role.CLINIC_ADMIN) {
			boolean sameClinicStaff = (target.getRole() == Role.DOCTOR || target.getRole() == Role.CLINIC_ADMIN)
				&& caller.clinicId().equals(target.getClinicId());
			boolean patientOfCallersClinic = target.getRole() == Role.PATIENT
				&& associationRepository.existsByClinicIdAndPatientId(caller.clinicId(), target.getId());
			if (!sameClinicStaff && !patientOfCallersClinic) {
				throw new ForbiddenException("You do not have access to this account.");
			}
		} else {
			throw new ForbiddenException("You may not resend welcome emails.");
		}

		String temporaryPassword = passwordGenerator.generate();
		target.setPasswordHash(passwordEncoder.encode(temporaryPassword));
		userRepository.save(target);
		welcomeEmailService.sendWelcomeEmail(target, temporaryPassword);
	}
}
