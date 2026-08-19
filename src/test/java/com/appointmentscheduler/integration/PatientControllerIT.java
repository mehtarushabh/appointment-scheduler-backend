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

class PatientControllerIT extends AbstractIntegrationTest {

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

	private String patientRequestJson(String email) {
		return """
			{"firstName": "Pat", "lastName": "Ient", "email": "%s", "dateOfBirth": "1995-03-03",
			 "address": {"addressLine1": "9 Oak St", "city": "Metropolis", "state": "NY", "zip": "10001", "country": "USA"}}
			""".formatted(email);
	}

	@Test
	void onboardsNewPatient() throws Exception {
		Clinic c = clinic("REG-P1");
		String token = clinicAdminToken(c.getId());

		mockMvc.perform(post("/api/v1/clinics/" + c.getId() + "/patients").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(patientRequestJson("pat1@example.com")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.role").value("PATIENT"));
	}

	@Test
	void linksExistingPatientToSecondClinicWithoutDuplicating() throws Exception {
		Clinic c1 = clinic("REG-P2");
		Clinic c2 = clinic("REG-P3");
		String token1 = clinicAdminToken(c1.getId());
		String token2 = clinicAdminToken(c2.getId());

		mockMvc.perform(post("/api/v1/clinics/" + c1.getId() + "/patients").header("Authorization", "Bearer " + token1)
				.contentType(MediaType.APPLICATION_JSON).content(patientRequestJson("shared@example.com")))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/clinics/" + c2.getId() + "/patients").header("Authorization", "Bearer " + token2)
				.contentType(MediaType.APPLICATION_JSON).content(patientRequestJson("shared@example.com")))
			.andExpect(status().isOk());

		long patientCount = userRepository.findAll().stream()
			.filter(u -> u.getEmail().equals("shared@example.com") && u.getRole() == Role.PATIENT)
			.count();
		org.assertj.core.api.Assertions.assertThat(patientCount).isEqualTo(1);
	}

	@Test
	void rejectsEmailBelongingToNonPatientRole() throws Exception {
		Clinic c = clinic("REG-P4");
		String token = clinicAdminToken(c.getId());
		mockMvc.perform(post("/api/v1/clinics/" + c.getId() + "/doctors").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"firstName": "Dana", "lastName": "Doc", "email": "notapatient@example.com", "dateOfBirth": "1988-01-01",
					 "address": {"addressLine1": "1 Main St", "city": "Metropolis", "state": "NY", "zip": "10001", "country": "USA"},
					 "specialty": "Cardiology"}
					"""))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/clinics/" + c.getId() + "/patients").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(patientRequestJson("notapatient@example.com")))
			.andExpect(status().isConflict());
	}

	@Test
	void listsOnlyOwnClinicPatients() throws Exception {
		Clinic c1 = clinic("REG-P5");
		Clinic c2 = clinic("REG-P6");
		String token1 = clinicAdminToken(c1.getId());
		String token2 = clinicAdminToken(c2.getId());

		mockMvc.perform(post("/api/v1/clinics/" + c1.getId() + "/patients").header("Authorization", "Bearer " + token1)
				.contentType(MediaType.APPLICATION_JSON).content(patientRequestJson("scoped1@example.com")))
			.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/clinics/" + c1.getId() + "/patients").header("Authorization", "Bearer " + token1))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1));

		mockMvc.perform(get("/api/v1/clinics/" + c1.getId() + "/patients").header("Authorization", "Bearer " + token2))
			.andExpect(status().isForbidden());
	}

	@Test
	void patientSeesAllTheirClinicsViaMeEndpoint() throws Exception {
		Clinic c1 = clinic("REG-P7");
		Clinic c2 = clinic("REG-P8");
		String token1 = clinicAdminToken(c1.getId());
		String token2 = clinicAdminToken(c2.getId());

		mockMvc.perform(post("/api/v1/clinics/" + c1.getId() + "/patients").header("Authorization", "Bearer " + token1)
				.contentType(MediaType.APPLICATION_JSON).content(patientRequestJson("multi@example.com")))
			.andExpect(status().isCreated());
		mockMvc.perform(post("/api/v1/clinics/" + c2.getId() + "/patients").header("Authorization", "Bearer " + token2)
				.contentType(MediaType.APPLICATION_JSON).content(patientRequestJson("multi@example.com")))
			.andExpect(status().isOk());

		User patient = userRepository.findByEmail("multi@example.com").orElseThrow();
		String patientToken = tokenService.issueToken(patient.getId(), Role.PATIENT, null);

		mockMvc.perform(get("/api/v1/me/clinics").header("Authorization", "Bearer " + patientToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2));
	}
}
