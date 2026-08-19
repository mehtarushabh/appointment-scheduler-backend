package com.appointmentscheduler.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import com.appointmentscheduler.model.Address;
import com.appointmentscheduler.model.Role;
import com.appointmentscheduler.model.User;
import com.appointmentscheduler.repository.ClinicRepository;
import com.appointmentscheduler.repository.UserRepository;
import com.appointmentscheduler.security.TokenService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * FR-016: onboarding a Clinic either creates both the Clinic and its first Clinic Admin, or
 * neither. Forces the failure path by racing two onboarding requests for the same admin email
 * (each with a distinct Registered ID) — the database's unique email constraint guarantees one
 * request's User insert fails, and @Transactional must roll back that request's Clinic insert
 * too, leaving no orphaned Clinic behind.
 */
class ClinicOnboardingAtomicityIT extends AbstractIntegrationTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ClinicRepository clinicRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TokenService tokenService;

	private String systemAdminToken() {
		User admin = userRepository.saveAndFlush(new User("Sam", "Admin", "atomicity-admin+" + java.util.UUID.randomUUID() + "@example.com",
				passwordEncoder.encode("password"), LocalDate.of(1980, 1, 1),
				new Address("1 Gov St", null, "Capital City", "IL", "60000", "USA"), Role.SYSTEM_ADMIN, null, null));
		return tokenService.issueToken(admin.getId(), Role.SYSTEM_ADMIN, null);
	}

	private String clinicRequestJson(String registeredId) {
		return """
			{
			  "name": "Race Clinic",
			  "address": {"addressLine1": "1 River Rd", "city": "Riverside", "state": "CA", "zip": "92501", "country": "USA"},
			  "registeredId": "%s",
			  "firstClinicAdmin": {
			    "firstName": "Race",
			    "lastName": "Admin",
			    "email": "race-admin@example.com",
			    "dateOfBirth": "1985-05-05",
			    "address": {"addressLine1": "2 Elm St", "city": "Riverside", "state": "CA", "zip": "92501", "country": "USA"}
			  }
			}
			""".formatted(registeredId);
	}

	@Test
	void losingRequestLeavesNoOrphanedClinic() throws Exception {
		String token = systemAdminToken();
		String registeredIdA = "REG-RACE-A";
		String registeredIdB = "REG-RACE-B";

		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch readyLatch = new CountDownLatch(2);
		CountDownLatch goLatch = new CountDownLatch(1);

		Future<MvcResult> resultA = executor.submit(() -> race(token, registeredIdA, readyLatch, goLatch));
		Future<MvcResult> resultB = executor.submit(() -> race(token, registeredIdB, readyLatch, goLatch));

		readyLatch.await(10, TimeUnit.SECONDS);
		goLatch.countDown();

		int statusA = resultA.get(10, TimeUnit.SECONDS).getResponse().getStatus();
		int statusB = resultB.get(10, TimeUnit.SECONDS).getResponse().getStatus();
		executor.shutdown();

		// Exactly one of the two concurrent same-email onboarding attempts must succeed.
		assertThat(java.util.List.of(statusA, statusB)).containsExactlyInAnyOrder(201, 409);

		boolean aSucceeded = statusA == 201;
		String winningRegisteredId = aSucceeded ? registeredIdA : registeredIdB;
		String losingRegisteredId = aSucceeded ? registeredIdB : registeredIdA;

		assertThat(clinicRepository.existsByRegisteredId(winningRegisteredId)).isTrue();
		assertThat(clinicRepository.existsByRegisteredId(losingRegisteredId)).isFalse();
	}

	private MvcResult race(String token, String registeredId, CountDownLatch readyLatch, CountDownLatch goLatch) throws Exception {
		readyLatch.countDown();
		goLatch.await(10, TimeUnit.SECONDS);
		return mockMvc.perform(post("/api/v1/clinics")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(clinicRequestJson(registeredId)))
			.andReturn();
	}
}
