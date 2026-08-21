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
 * Cross-cutting regression test for FR-010: every /api/v1/clinics/me/... endpoint derives its
 * target clinic from the caller's own principal rather than a caller-supplied id, so a Clinic
 * Admin token can never reach another clinic's data through them. What remains attackable is the
 * role check itself — a Doctor or Patient token must still be rejected outright, regardless of
 * which clinic it belongs to.
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

	private String doctorToken(UUID clinicId) {
		User doctor = userRepository.saveAndFlush(new User("Dana", "Doc", "cross-doc+" + UUID.randomUUID() + "@example.com",
				passwordEncoder.encode("password"), LocalDate.of(1988, 1, 1), sampleAddress(), Role.DOCTOR, clinicId, "Cardiology"));
		return tokenService.issueToken(doctor.getId(), Role.DOCTOR, clinicId);
	}

	private String patientToken() {
		User patient = userRepository.saveAndFlush(new User("Pat", "Ient", "cross-pat+" + UUID.randomUUID() + "@example.com",
				passwordEncoder.encode("password"), LocalDate.of(1995, 3, 3), sampleAddress(), Role.PATIENT, null, null));
		return tokenService.issueToken(patient.getId(), Role.PATIENT, null);
	}

	@Test
	void deniesEveryClinicAdminOnlyEndpointToNonAdminRoles() throws Exception {
		Clinic clinic = clinic("REG-X1");
		String doctorToken = doctorToken(clinic.getId());
		String patientToken = patientToken();

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

		for (String token : new String[] { doctorToken, patientToken }) {
			mockMvc.perform(get("/api/v1/clinics/me/doctors").header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden());
			mockMvc.perform(post("/api/v1/clinics/me/doctors").header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON).content(doctorPayload))
				.andExpect(status().isForbidden());

			mockMvc.perform(get("/api/v1/clinics/me/patients").header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden());
			mockMvc.perform(post("/api/v1/clinics/me/patients").header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON).content(patientPayload))
				.andExpect(status().isForbidden());

			mockMvc.perform(post("/api/v1/clinics/me/clinic-admins").header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON).content(clinicAdminPayload))
				.andExpect(status().isForbidden());

			mockMvc.perform(get("/api/v1/clinics/me/appointments").header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden());
		}
	}
}
