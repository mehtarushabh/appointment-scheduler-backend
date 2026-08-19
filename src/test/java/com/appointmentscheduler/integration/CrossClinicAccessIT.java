package com.appointmentscheduler.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

/**
 * Cross-cutting regression test for SC-005: a Clinic Admin of one clinic must never be able to
 * list or onboard into a doctor/patient/clinic-admin endpoint scoped to a different clinic.
 */
class CrossClinicAccessIT extends AbstractIntegrationTest {

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
		User admin = userRepository.saveAndFlush(new User("Cara", "Admin", "cross+" + UUID.randomUUID() + "@example.com",
				passwordEncoder.encode("password"), LocalDate.of(1985, 1, 1), sampleAddress(), Role.CLINIC_ADMIN,
				clinicId, null));
		return tokenService.issueToken(admin.getId(), Role.CLINIC_ADMIN, clinicId);
	}

	@Test
	void deniesEveryClinicScopedEndpointAcrossClinics() throws Exception {
		Clinic ownClinic = clinic("REG-X1");
		Clinic otherClinic = clinic("REG-X2");
		String tokenForOwnClinicOnly = clinicAdminToken(ownClinic.getId());

		String doctorPayload = """
			{"firstName": "Dana", "lastName": "Doc", "email": "crossdoc@example.com", "dateOfBirth": "1988-01-01",
			 "address": {"addressLine1": "1 Main St", "city": "Metropolis", "state": "NY", "zip": "10001", "country": "USA"},
			 "specialty": "Cardiology"}
			""";
		String patientPayload = """
			{"firstName": "Pat", "lastName": "Ient", "email": "crosspat@example.com", "dateOfBirth": "1995-03-03",
			 "address": {"addressLine1": "1 Main St", "city": "Metropolis", "state": "NY", "zip": "10001", "country": "USA"}}
			""";
		String clinicAdminPayload = """
			{"firstName": "Second", "lastName": "Admin", "email": "crossadmin@example.com", "dateOfBirth": "1990-01-01",
			 "address": {"addressLine1": "1 Main St", "city": "Metropolis", "state": "NY", "zip": "10001", "country": "USA"}}
			""";

		mockMvc.perform(get("/api/v1/clinics/" + otherClinic.getId() + "/doctors").header("Authorization", "Bearer " + tokenForOwnClinicOnly))
			.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/clinics/" + otherClinic.getId() + "/doctors").header("Authorization", "Bearer " + tokenForOwnClinicOnly)
				.contentType(MediaType.APPLICATION_JSON).content(doctorPayload))
			.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/v1/clinics/" + otherClinic.getId() + "/patients").header("Authorization", "Bearer " + tokenForOwnClinicOnly))
			.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/clinics/" + otherClinic.getId() + "/patients").header("Authorization", "Bearer " + tokenForOwnClinicOnly)
				.contentType(MediaType.APPLICATION_JSON).content(patientPayload))
			.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/v1/clinics/" + otherClinic.getId() + "/clinic-admins").header("Authorization", "Bearer " + tokenForOwnClinicOnly)
				.contentType(MediaType.APPLICATION_JSON).content(clinicAdminPayload))
			.andExpect(status().isForbidden());
	}
}
