package com.appointmentscheduler.integration;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

class AuthControllerIT extends AbstractIntegrationTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void loginSucceedsWithValidCredentials() throws Exception {
		userRepository.saveAndFlush(new User("Ada", "Admin", "ada@example.com",
				passwordEncoder.encode("correct-horse"), LocalDate.of(1990, 1, 1),
				new Address("1 Main St", null, "Springfield", "IL", "62704", "USA"), Role.SYSTEM_ADMIN, null, null));

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"ada@example.com\",\"password\":\"correct-horse\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token", notNullValue()))
			.andExpect(jsonPath("$.role").value("SYSTEM_ADMIN"));
	}

	@Test
	void loginRejectsInvalidCredentials() throws Exception {
		userRepository.saveAndFlush(new User("Ada", "Admin", "ada2@example.com",
				passwordEncoder.encode("correct-horse"), LocalDate.of(1990, 1, 1),
				new Address("1 Main St", null, "Springfield", "IL", "62704", "USA"), Role.SYSTEM_ADMIN, null, null));

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"ada2@example.com\",\"password\":\"wrong-password\"}"))
			.andExpect(status().isUnauthorized());
	}
}
