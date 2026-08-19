package com.appointmentscheduler.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.appointmentscheduler.model.Address;
import com.appointmentscheduler.model.Clinic;
import com.appointmentscheduler.model.Role;
import com.appointmentscheduler.model.User;
import com.appointmentscheduler.repository.ClinicRepository;
import com.appointmentscheduler.repository.UserRepository;
import com.appointmentscheduler.security.TokenService;

class ResendWelcomeEmailControllerIT extends AbstractIntegrationTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ClinicRepository clinicRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TokenService tokenService;

	private static Address sampleAddress() {
		return new Address("1 Main St", null, "Metropolis", "NY", "10001", "USA");
	}

	@Test
	void clinicAdminCanResendForOwnClinicDoctor() throws Exception {
		Clinic clinic = clinicRepository.saveAndFlush(new Clinic("Clinic Resend", sampleAddress(), "REG-R1"));
		User admin = userRepository.saveAndFlush(new User("Cara", "Admin", "resendadmin@example.com",
				passwordEncoder.encode("password"), LocalDate.of(1985, 1, 1), sampleAddress(), Role.CLINIC_ADMIN,
				clinic.getId(), null));
		User doctor = userRepository.saveAndFlush(new User("Dana", "Doc", "resenddoctor@example.com",
				passwordEncoder.encode("password"), LocalDate.of(1988, 1, 1), sampleAddress(), Role.DOCTOR,
				clinic.getId(), "Cardiology"));
		String token = tokenService.issueToken(admin.getId(), Role.CLINIC_ADMIN, clinic.getId());

		mockMvc.perform(post("/api/v1/users/" + doctor.getId() + "/resend-welcome-email").header("Authorization", "Bearer " + token))
			.andExpect(status().isAccepted());
	}

	@Test
	void rejectsResendForAccountOutsideCallersClinic() throws Exception {
		Clinic clinic1 = clinicRepository.saveAndFlush(new Clinic("Clinic A", sampleAddress(), "REG-R2"));
		Clinic clinic2 = clinicRepository.saveAndFlush(new Clinic("Clinic B", sampleAddress(), "REG-R3"));
		User admin1 = userRepository.saveAndFlush(new User("Cara", "Admin", "admin1resend@example.com",
				passwordEncoder.encode("password"), LocalDate.of(1985, 1, 1), sampleAddress(), Role.CLINIC_ADMIN,
				clinic1.getId(), null));
		User doctorInClinic2 = userRepository.saveAndFlush(new User("Dana", "Doc", "doctorinclinic2@example.com",
				passwordEncoder.encode("password"), LocalDate.of(1988, 1, 1), sampleAddress(), Role.DOCTOR,
				clinic2.getId(), "Cardiology"));
		String token = tokenService.issueToken(admin1.getId(), Role.CLINIC_ADMIN, clinic1.getId());

		mockMvc.perform(post("/api/v1/users/" + doctorInClinic2.getId() + "/resend-welcome-email").header("Authorization", "Bearer " + token))
			.andExpect(status().isForbidden());
	}

	@Test
	void returnsNotFoundForUnknownUser() throws Exception {
		Clinic clinic = clinicRepository.saveAndFlush(new Clinic("Clinic Missing", sampleAddress(), "REG-R4"));
		User admin = userRepository.saveAndFlush(new User("Cara", "Admin", "missingadmin@example.com",
				passwordEncoder.encode("password"), LocalDate.of(1985, 1, 1), sampleAddress(), Role.CLINIC_ADMIN,
				clinic.getId(), null));
		String token = tokenService.issueToken(admin.getId(), Role.CLINIC_ADMIN, clinic.getId());

		mockMvc.perform(post("/api/v1/users/" + UUID.randomUUID() + "/resend-welcome-email").header("Authorization", "Bearer " + token))
			.andExpect(status().isNotFound());
	}
}
