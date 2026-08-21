package com.appointmentscheduler.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.appointmentscheduler.model.Address;
import com.appointmentscheduler.model.Clinic;
import com.appointmentscheduler.model.ClinicPatientAssociation;
import com.appointmentscheduler.model.ClinicWorkingHours;
import com.appointmentscheduler.model.Role;
import com.appointmentscheduler.model.User;
import com.appointmentscheduler.repository.ClinicPatientAssociationRepository;
import com.appointmentscheduler.repository.ClinicRepository;
import com.appointmentscheduler.repository.ClinicWorkingHoursRepository;
import com.appointmentscheduler.repository.UserRepository;
import com.appointmentscheduler.security.TokenService;

class ClinicSettingsControllerIT extends AbstractIntegrationTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ClinicRepository clinicRepository;

	@Autowired
	private ClinicWorkingHoursRepository clinicWorkingHoursRepository;

	@Autowired
	private ClinicPatientAssociationRepository associationRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TokenService tokenService;

	private static Address sampleAddress() {
		return new Address("1 Main St", null, "Metropolis", "NY", "10001", "USA");
	}

	/**
	 * Bypasses ClinicService.onboardClinic() (this test only cares about the settings endpoints,
	 * not onboarding), so the default working-hours row-set it seeds (FR-002) is seeded here
	 * instead, matching the same Mon-Fri 08:00-17:00 / Sat-Sun closed default.
	 */
	private Clinic clinic(String registeredId) {
		Clinic clinic = clinicRepository.saveAndFlush(new Clinic("Metropolis Clinic", sampleAddress(), registeredId));
		Arrays.stream(DayOfWeek.values()).forEach(day -> {
			boolean open = day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
			clinicWorkingHoursRepository.save(new ClinicWorkingHours(clinic.getId(), day, open,
					open ? LocalTime.of(8, 0) : null, open ? LocalTime.of(17, 0) : null));
		});
		return clinic;
	}

	private String clinicAdminToken(UUID clinicId) {
		User admin = userRepository.saveAndFlush(new User("Cara", "Admin", "settings-admin+" + UUID.randomUUID() + "@example.com",
				passwordEncoder.encode("password"), LocalDate.of(1985, 1, 1), sampleAddress(), Role.CLINIC_ADMIN, clinicId, null));
		return tokenService.issueToken(admin.getId(), Role.CLINIC_ADMIN, clinicId);
	}

	private static String profileUpdateJson(String name) {
		return """
			{"name": "%s", "address": {"addressLine1": "9 New Rd", "city": "Newtown", "state": "CA", "zip": "90001", "country": "USA"}}
			""".formatted(name);
	}

	@Test
	void getsOwnClinicProfile() throws Exception {
		Clinic c = clinic("REG-CS0");
		String token = clinicAdminToken(c.getId());

		mockMvc.perform(get("/api/v1/clinics/me").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Metropolis Clinic"))
			.andExpect(jsonPath("$.registeredId").value("REG-CS0"));
	}

	@Test
	void allowsAssociatedPatientToGetWorkingHours() throws Exception {
		Clinic c = clinic("REG-CS0D");
		User patient = userRepository.saveAndFlush(new User("Pat", "Ient", "settings-patient+" + UUID.randomUUID() + "@example.com",
				passwordEncoder.encode("password"), LocalDate.of(1995, 1, 1), sampleAddress(), Role.PATIENT, null, null));
		associationRepository.saveAndFlush(new ClinicPatientAssociation(c.getId(), patient.getId()));
		String token = tokenService.issueToken(patient.getId(), Role.PATIENT, null);

		mockMvc.perform(get("/api/v1/clinics/" + c.getId() + "/working-hours").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(7)));
	}

	@Test
	void rejectsUnassociatedPatientFromWorkingHours() throws Exception {
		Clinic c = clinic("REG-CS0E");
		User patient = userRepository.saveAndFlush(new User("Pat", "Ient", "settings-patient2+" + UUID.randomUUID() + "@example.com",
				passwordEncoder.encode("password"), LocalDate.of(1995, 1, 1), sampleAddress(), Role.PATIENT, null, null));
		String token = tokenService.issueToken(patient.getId(), Role.PATIENT, null);

		mockMvc.perform(get("/api/v1/clinics/" + c.getId() + "/working-hours").header("Authorization", "Bearer " + token))
			.andExpect(status().isForbidden());
	}

	@Test
	void returnsCallersOwnClinicProfileOnly() throws Exception {
		Clinic c1 = clinic("REG-CS0B");
		Clinic c2 = clinic("REG-CS0C");
		String token1 = clinicAdminToken(c1.getId());
		String token2 = clinicAdminToken(c2.getId());

		mockMvc.perform(get("/api/v1/clinics/me").header("Authorization", "Bearer " + token1))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.registeredId").value("REG-CS0B"));

		mockMvc.perform(get("/api/v1/clinics/me").header("Authorization", "Bearer " + token2))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.registeredId").value("REG-CS0C"));
	}

	@Test
	void updatesOwnClinicProfile() throws Exception {
		Clinic c = clinic("REG-CS1");
		String token = clinicAdminToken(c.getId());

		mockMvc.perform(patch("/api/v1/clinics/me").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(profileUpdateJson("Renamed Clinic")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Renamed Clinic"))
			.andExpect(jsonPath("$.address.city").value("Newtown"))
			.andExpect(jsonPath("$.registeredId").value("REG-CS1"));
	}

	@Test
	void updatingOwnProfileNeverAffectsAnotherClinic() throws Exception {
		Clinic c1 = clinic("REG-CS2");
		Clinic c2 = clinic("REG-CS3");
		String token1 = clinicAdminToken(c1.getId());

		mockMvc.perform(patch("/api/v1/clinics/me").header("Authorization", "Bearer " + token1)
				.contentType(MediaType.APPLICATION_JSON).content(profileUpdateJson("Renamed By Admin One")))
			.andExpect(status().isOk());

		Clinic reloaded = clinicRepository.findById(c2.getId()).orElseThrow();
		org.assertj.core.api.Assertions.assertThat(reloaded.getName()).isEqualTo("Metropolis Clinic");
	}

	@Test
	void getsAllSevenDefaultWorkingHoursEntries() throws Exception {
		Clinic c = clinic("REG-CS4");
		String token = clinicAdminToken(c.getId());

		mockMvc.perform(get("/api/v1/clinics/" + c.getId() + "/working-hours").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(7)));
	}

	private static String workingHoursJson(String tuesdayStart, String tuesdayEnd, boolean wednesdayOpen) {
		return """
			{"days": [
			  {"dayOfWeek": "MONDAY", "isOpen": true, "startTime": "08:00", "endTime": "17:00"},
			  {"dayOfWeek": "TUESDAY", "isOpen": true, "startTime": "%s", "endTime": "%s"},
			  {"dayOfWeek": "WEDNESDAY", "isOpen": %s, "startTime": null, "endTime": null},
			  {"dayOfWeek": "THURSDAY", "isOpen": true, "startTime": "08:00", "endTime": "17:00"},
			  {"dayOfWeek": "FRIDAY", "isOpen": true, "startTime": "08:00", "endTime": "17:00"},
			  {"dayOfWeek": "SATURDAY", "isOpen": false, "startTime": null, "endTime": null},
			  {"dayOfWeek": "SUNDAY", "isOpen": false, "startTime": null, "endTime": null}
			]}
			""".formatted(tuesdayStart, tuesdayEnd, wednesdayOpen);
	}

	@Test
	void replacesAllSevenWorkingHoursEntries() throws Exception {
		Clinic c = clinic("REG-CS5");
		String token = clinicAdminToken(c.getId());

		mockMvc.perform(put("/api/v1/clinics/me/working-hours").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(workingHoursJson("09:00", "18:00", false)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(7)));

		mockMvc.perform(get("/api/v1/clinics/" + c.getId() + "/working-hours").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[1].dayOfWeek").value("TUESDAY"))
			.andExpect(jsonPath("$[1].startTime").value("09:00:00"))
			.andExpect(jsonPath("$[2].dayOfWeek").value("WEDNESDAY"))
			.andExpect(jsonPath("$[2].isOpen").value(false));
	}

	@Test
	void rejectsWorkingDayEndTimeNotAfterStartTimeIdentifyingTheDay() throws Exception {
		Clinic c = clinic("REG-CS6");
		String token = clinicAdminToken(c.getId());

		mockMvc.perform(put("/api/v1/clinics/me/working-hours").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(workingHoursJson("18:00", "09:00", false)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("TUESDAY")));
	}
}
