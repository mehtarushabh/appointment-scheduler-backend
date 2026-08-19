package com.appointmentscheduler.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.appointmentscheduler.model.Address;
import com.appointmentscheduler.model.Clinic;
import com.appointmentscheduler.model.Role;
import com.appointmentscheduler.model.User;
import com.appointmentscheduler.repository.ClinicRepository;
import com.appointmentscheduler.repository.UserRepository;
import com.appointmentscheduler.security.TokenService;

class ClinicAdminControllerIT extends AbstractIntegrationTest {

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

	private Clinic clinic(String registeredId) {
		return clinicRepository.saveAndFlush(new Clinic("Clinic " + registeredId, sampleAddress(), registeredId));
	}

	private String clinicAdminToken(UUID clinicId) {
		User admin = userRepository.saveAndFlush(new User("Cara", "Admin", "admin+" + UUID.randomUUID() + "@example.com",
				passwordEncoder.encode("password"), LocalDate.of(1985, 1, 1), sampleAddress(), Role.CLINIC_ADMIN,
				clinicId, null));
		return tokenService.issueToken(admin.getId(), Role.CLINIC_ADMIN, clinicId);
	}

	private String clinicAdminRequestJson(String email) {
		return """
			{"firstName": "Second", "lastName": "Admin", "email": "%s", "dateOfBirth": "1990-01-01",
			 "address": {"addressLine1": "1 Main St", "city": "Metropolis", "state": "NY", "zip": "10001", "country": "USA"}}
			""".formatted(email);
	}

	@Test
	void onboardsAnotherClinicAdminForTheSameClinic() throws Exception {
		Clinic c = clinic("REG-CA1");
		String token = clinicAdminToken(c.getId());

		mockMvc.perform(post("/api/v1/clinics/" + c.getId() + "/clinic-admins").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(clinicAdminRequestJson("second1@example.com")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.role").value("CLINIC_ADMIN"))
			.andExpect(jsonPath("$.clinicId").value(c.getId().toString()));
	}

	@Test
	void rejectsDuplicateEmail() throws Exception {
		Clinic c = clinic("REG-CA2");
		String token = clinicAdminToken(c.getId());
		mockMvc.perform(post("/api/v1/clinics/" + c.getId() + "/clinic-admins").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(clinicAdminRequestJson("dupe-admin@example.com")))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/clinics/" + c.getId() + "/clinic-admins").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(clinicAdminRequestJson("dupe-admin@example.com")))
			.andExpect(status().isConflict());
	}

	@Test
	void rejectsMissingRequiredField() throws Exception {
		Clinic c = clinic("REG-CA3");
		String token = clinicAdminToken(c.getId());
		String invalid = """
			{"firstName": "", "lastName": "Admin", "email": "invalidadmin@example.com", "dateOfBirth": "1990-01-01",
			 "address": {"addressLine1": "1 Main St", "city": "Metropolis", "state": "NY", "zip": "10001", "country": "USA"}}
			""";
		mockMvc.perform(post("/api/v1/clinics/" + c.getId() + "/clinic-admins").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(invalid))
			.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsCallerFromDifferentClinic() throws Exception {
		Clinic c1 = clinic("REG-CA4");
		Clinic c2 = clinic("REG-CA5");
		String otherClinicToken = clinicAdminToken(c2.getId());

		mockMvc.perform(post("/api/v1/clinics/" + c1.getId() + "/clinic-admins").header("Authorization", "Bearer " + otherClinicToken)
				.contentType(MediaType.APPLICATION_JSON).content(clinicAdminRequestJson("crossclinic@example.com")))
			.andExpect(status().isForbidden());
	}
}
