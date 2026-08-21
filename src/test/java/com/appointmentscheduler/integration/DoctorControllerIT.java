package com.appointmentscheduler.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

class DoctorControllerIT extends AbstractIntegrationTest {

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
		return clinicRepository.saveAndFlush(new Clinic("Metropolis Clinic", sampleAddress(), registeredId));
	}

	private String clinicAdminToken(UUID clinicId) {
		User admin = userRepository.saveAndFlush(new User("Cara", "Admin", "admin+" + UUID.randomUUID() + "@example.com",
				passwordEncoder.encode("password"), LocalDate.of(1985, 1, 1), sampleAddress(), Role.CLINIC_ADMIN,
				clinicId, null));
		return tokenService.issueToken(admin.getId(), Role.CLINIC_ADMIN, clinicId);
	}

	private String doctorRequestJson(String email) {
		return """
			{"firstName": "Dana", "lastName": "Doc", "email": "%s", "dateOfBirth": "1988-01-01",
			 "address": {"addressLine1": "1 Main St", "city": "Metropolis", "state": "NY", "zip": "10001", "country": "USA"},
			 "specialty": "Cardiology"}
			""".formatted(email);
	}

	@Test
	void onboardsDoctorWithSpecialty() throws Exception {
		Clinic c = clinic("REG-D1");
		String token = clinicAdminToken(c.getId());

		mockMvc.perform(post("/api/v1/clinics/me/doctors")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(doctorRequestJson("dana1@example.com")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.specialty").value("Cardiology"))
			.andExpect(jsonPath("$.role").value("DOCTOR"));
	}

	@Test
	void rejectsDuplicateDoctorEmail() throws Exception {
		Clinic c = clinic("REG-D2");
		String token = clinicAdminToken(c.getId());
		mockMvc.perform(post("/api/v1/clinics/me/doctors").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(doctorRequestJson("dupe-doc@example.com")))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/clinics/me/doctors").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(doctorRequestJson("dupe-doc@example.com")))
			.andExpect(status().isConflict());
	}

	@Test
	void rejectsMissingSpecialty() throws Exception {
		Clinic c = clinic("REG-D3");
		String token = clinicAdminToken(c.getId());
		String missingSpecialty = """
			{"firstName": "Dana", "lastName": "Doc", "email": "nospecialty@example.com", "dateOfBirth": "1988-01-01",
			 "address": {"addressLine1": "1 Main St", "city": "Metropolis", "state": "NY", "zip": "10001", "country": "USA"}}
			""";
		mockMvc.perform(post("/api/v1/clinics/me/doctors").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(missingSpecialty))
			.andExpect(status().isBadRequest());
	}

	@Test
	void listsOnlyOwnClinicDoctors() throws Exception {
		Clinic c1 = clinic("REG-D4");
		Clinic c2 = clinic("REG-D5");
		String token1 = clinicAdminToken(c1.getId());
		String token2 = clinicAdminToken(c2.getId());

		mockMvc.perform(post("/api/v1/clinics/me/doctors").header("Authorization", "Bearer " + token1)
				.contentType(MediaType.APPLICATION_JSON).content(doctorRequestJson("indoctor1@example.com")))
			.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/clinics/me/doctors").header("Authorization", "Bearer " + token1))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1));

		mockMvc.perform(get("/api/v1/clinics/me/doctors").header("Authorization", "Bearer " + token2))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
	}
}
