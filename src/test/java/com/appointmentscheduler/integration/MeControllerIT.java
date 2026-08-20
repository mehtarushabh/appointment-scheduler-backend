package com.appointmentscheduler.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

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

/** GET /me: the current user's own display profile (feature 003, research.md #1). */
class MeControllerIT extends AbstractIntegrationTest {

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
	void returnsFirstNameLastNameAndNullClinicNameForSystemAdmin() throws Exception {
		User admin = userRepository.saveAndFlush(new User("Ada", "Admin", "ada-me@example.com",
				passwordEncoder.encode("password"), LocalDate.of(1990, 1, 1), sampleAddress(), Role.SYSTEM_ADMIN, null,
				null));
		String token = tokenService.issueToken(admin.getId(), Role.SYSTEM_ADMIN, null);

		mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.firstName").value("Ada"))
			.andExpect(jsonPath("$.lastName").value("Admin"))
			.andExpect(jsonPath("$.clinicName").doesNotExist());
	}

	@Test
	void returnsClinicNameForClinicAdmin() throws Exception {
		Clinic clinic = clinicRepository.saveAndFlush(new Clinic("Riverside Clinic", sampleAddress(), "REG-ME1"));
		User clinicAdmin = userRepository.saveAndFlush(new User("Cara", "Admin", "cara-me@example.com",
				passwordEncoder.encode("password"), LocalDate.of(1985, 1, 1), sampleAddress(), Role.CLINIC_ADMIN,
				clinic.getId(), null));
		String token = tokenService.issueToken(clinicAdmin.getId(), Role.CLINIC_ADMIN, clinic.getId());

		mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.firstName").value("Cara"))
			.andExpect(jsonPath("$.lastName").value("Admin"))
			.andExpect(jsonPath("$.clinicName").value("Riverside Clinic"));
	}

	@Test
	void rejectsUnauthenticatedRequest() throws Exception {
		// Matches this codebase's established convention: missing/invalid bearer token on a
		// protected endpoint is 403 (no custom AuthenticationEntryPoint is configured; see
		// SecurityConfig), not 401 — 401 is reserved for POST /auth/login rejecting bad credentials.
		mockMvc.perform(get("/api/v1/me")).andExpect(status().isForbidden());
	}
}
