package com.appointmentscheduler.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.appointmentscheduler.model.Address;
import com.appointmentscheduler.model.Role;
import com.appointmentscheduler.model.User;
import com.appointmentscheduler.repository.UserRepository;

/** FR-021: password change is available any time, never mandatory. */
class PasswordControllerIT extends AbstractIntegrationTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private String login(String email, String password) throws Exception {
		var result = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
			.andReturn();
		String body = result.getResponse().getContentAsString();
		return body.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
	}

	@Test
	void changesPasswordAndOldPasswordStopsWorking() throws Exception {
		userRepository.saveAndFlush(new User("Pat", "User", "changepw@example.com", passwordEncoder.encode("old-password"),
				LocalDate.of(1990, 1, 1), new Address("1 Main St", null, "Metropolis", "NY", "10001", "USA"),
				Role.PATIENT, null, null));
		String token = login("changepw@example.com", "old-password");

		mockMvc.perform(patch("/api/v1/me/password").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentPassword\":\"old-password\",\"newPassword\":\"new-password\"}"))
			.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"changepw@example.com\",\"password\":\"old-password\"}"))
			.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"changepw@example.com\",\"password\":\"new-password\"}"))
			.andExpect(status().isOk());
	}

	@Test
	void rejectsWrongCurrentPassword() throws Exception {
		userRepository.saveAndFlush(new User("Pat", "User", "wrongpw@example.com", passwordEncoder.encode("old-password"),
				LocalDate.of(1990, 1, 1), new Address("1 Main St", null, "Metropolis", "NY", "10001", "USA"),
				Role.PATIENT, null, null));
		String token = login("wrongpw@example.com", "old-password");

		mockMvc.perform(patch("/api/v1/me/password").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentPassword\":\"not-the-password\",\"newPassword\":\"new-password\"}"))
			.andExpect(status().isBadRequest());
	}
}
