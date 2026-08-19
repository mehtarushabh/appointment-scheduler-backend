package com.appointmentscheduler.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

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

class ClinicControllerIT extends AbstractIntegrationTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ClinicRepository clinicRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TokenService tokenService;

	private String systemAdminToken() {
		User admin = userRepository.saveAndFlush(new User("Sam", "Admin", "sysadmin+" + java.util.UUID.randomUUID() + "@example.com",
				passwordEncoder.encode("password"), LocalDate.of(1980, 1, 1),
				new Address("1 Gov St", null, "Capital City", "IL", "60000", "USA"), Role.SYSTEM_ADMIN, null, null));
		return tokenService.issueToken(admin.getId(), Role.SYSTEM_ADMIN, null);
	}

	private String clinicRequestJson(String adminEmail, String registeredId) {
		return """
			{
			  "name": "Riverside Clinic",
			  "address": {"addressLine1": "1 River Rd", "city": "Riverside", "state": "CA", "zip": "92501", "country": "USA"},
			  "registeredId": "%s",
			  "firstClinicAdmin": {
			    "firstName": "Cara",
			    "lastName": "Admin",
			    "email": "%s",
			    "dateOfBirth": "1985-05-05",
			    "address": {"addressLine1": "2 Elm St", "city": "Riverside", "state": "CA", "zip": "92501", "country": "USA"}
			  }
			}
			""".formatted(registeredId, adminEmail);
	}

	@Test
	void onboardsClinicWithFirstClinicAdmin() throws Exception {
		mockMvc.perform(post("/api/v1/clinics")
				.header("Authorization", "Bearer " + systemAdminToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content(clinicRequestJson("cara1@example.com", "REG-100")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("Riverside Clinic"))
			.andExpect(jsonPath("$.firstClinicAdmin.email").value("cara1@example.com"));
	}

	@Test
	void rejectsDuplicateClinicAdminEmail() throws Exception {
		String token = systemAdminToken();
		mockMvc.perform(post("/api/v1/clinics").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(clinicRequestJson("dupe@example.com", "REG-200")))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/clinics").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(clinicRequestJson("dupe@example.com", "REG-201")))
			.andExpect(status().isConflict());
	}

	@Test
	void rejectsDuplicateRegisteredId() throws Exception {
		String token = systemAdminToken();
		mockMvc.perform(post("/api/v1/clinics").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(clinicRequestJson("first@example.com", "REG-300")))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/clinics").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(clinicRequestJson("second@example.com", "REG-300")))
			.andExpect(status().isConflict());
	}

	@Test
	void rejectsMissingRequiredField() throws Exception {
		String token = systemAdminToken();
		String invalidJson = """
			{"name": "", "address": {"addressLine1": "1 River Rd", "city": "Riverside", "state": "CA", "zip": "92501", "country": "USA"}, "registeredId": "REG-400"}
			""";
		mockMvc.perform(post("/api/v1/clinics").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(invalidJson))
			.andExpect(status().isBadRequest());
	}

	@Test
	void listsAllClinicsForSystemAdmin() throws Exception {
		String token = systemAdminToken();
		mockMvc.perform(post("/api/v1/clinics").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(clinicRequestJson("lister@example.com", "REG-500")))
			.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/clinics").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(org.hamcrest.Matchers.greaterThanOrEqualTo(1))));
	}

	@Test
	void rejectsNonSystemAdminCaller() throws Exception {
		Clinic clinic = clinicRepository.saveAndFlush(new Clinic("Metropolis Clinic",
				new Address("1 Clinic Rd", null, "Metropolis", "NY", "10001", "USA"), "REG-600"));
		User doctor = userRepository.saveAndFlush(new User("Dana", "Doc", "dana+" + java.util.UUID.randomUUID() + "@example.com",
				passwordEncoder.encode("password"), LocalDate.of(1988, 1, 1),
				new Address("1 Clinic Rd", null, "Metropolis", "NY", "10001", "USA"), Role.DOCTOR,
				clinic.getId(), "Cardiology"));
		String token = tokenService.issueToken(doctor.getId(), Role.DOCTOR, doctor.getClinicId());

		mockMvc.perform(get("/api/v1/clinics").header("Authorization", "Bearer " + token))
			.andExpect(status().isForbidden());
	}
}
