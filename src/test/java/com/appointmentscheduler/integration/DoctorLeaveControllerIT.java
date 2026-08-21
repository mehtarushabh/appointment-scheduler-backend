package com.appointmentscheduler.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.appointmentscheduler.model.Address;
import com.appointmentscheduler.model.Appointment;
import com.appointmentscheduler.model.AppointmentState;
import com.appointmentscheduler.model.Clinic;
import com.appointmentscheduler.model.ClinicWorkingHours;
import com.appointmentscheduler.model.Role;
import com.appointmentscheduler.model.User;
import com.appointmentscheduler.repository.AppointmentRepository;
import com.appointmentscheduler.repository.ClinicRepository;
import com.appointmentscheduler.repository.ClinicWorkingHoursRepository;
import com.appointmentscheduler.repository.UserRepository;
import com.appointmentscheduler.security.TokenService;

class DoctorLeaveControllerIT extends AbstractIntegrationTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ClinicRepository clinicRepository;

	@Autowired
	private ClinicWorkingHoursRepository clinicWorkingHoursRepository;

	@Autowired
	private AppointmentRepository appointmentRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TokenService tokenService;

	private static Address sampleAddress() {
		return new Address("1 Main St", null, "Metropolis", "NY", "10001", "USA");
	}

	private Clinic clinicWithDefaultHours(String registeredId) {
		Clinic clinic = clinicRepository.saveAndFlush(new Clinic("Metropolis Clinic", sampleAddress(), registeredId));
		Arrays.stream(DayOfWeek.values()).forEach(day -> {
			boolean open = day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
			clinicWorkingHoursRepository.save(new ClinicWorkingHours(clinic.getId(), day, open,
					open ? LocalTime.of(8, 0) : null, open ? LocalTime.of(17, 0) : null));
		});
		return clinic;
	}

	private User doctor(UUID clinicId, String email) {
		return userRepository.saveAndFlush(new User("Dana", "Doc", email, passwordEncoder.encode("password"),
				LocalDate.of(1988, 1, 1), sampleAddress(), Role.DOCTOR, clinicId, "Cardiology"));
	}

	private User patient(String email) {
		return userRepository.saveAndFlush(new User("Pat", "Ient", email, passwordEncoder.encode("password"),
				LocalDate.of(1995, 1, 1), sampleAddress(), Role.PATIENT, null, null));
	}

	private String tokenFor(User user) {
		return tokenService.issueToken(user.getId(), user.getRole(), user.getClinicId());
	}

	private static LocalDate nextMonday() {
		return LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
	}

	private static String fullDayLeaveJson(LocalDate date) {
		return """
			{"date":"%s","fullDay":true}
			""".formatted(date);
	}

	private static String partialLeaveJson(LocalDate date, String startTime, String endTime, boolean confirm) {
		return """
			{"date":"%s","fullDay":false,"startTime":"%s","endTime":"%s","confirmCancelConflicts":%s}
			""".formatted(date, startTime, endTime, confirm);
	}

	@Test
	void listsOwnLeaveEntries() throws Exception {
		Clinic clinic = clinicWithDefaultHours("REG-DL1");
		User doc = doctor(clinic.getId(), "dl-doc1@example.com");

		mockMvc.perform(get("/api/v1/me/leaves").header("Authorization", "Bearer " + tokenFor(doc)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
	}

	@Test
	void addsFullDayLeaveWithNoConflicts() throws Exception {
		Clinic clinic = clinicWithDefaultHours("REG-DL2");
		User doc = doctor(clinic.getId(), "dl-doc2@example.com");

		mockMvc.perform(post("/api/v1/me/leaves").header("Authorization", "Bearer " + tokenFor(doc))
				.contentType(MediaType.APPLICATION_JSON).content(fullDayLeaveJson(nextMonday())))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.fullDay").value(true));
	}

	@Test
	void addsPartialDayLeaveWithNoConflicts() throws Exception {
		Clinic clinic = clinicWithDefaultHours("REG-DL3");
		User doc = doctor(clinic.getId(), "dl-doc3@example.com");

		mockMvc.perform(post("/api/v1/me/leaves").header("Authorization", "Bearer " + tokenFor(doc))
				.contentType(MediaType.APPLICATION_JSON).content(partialLeaveJson(nextMonday(), "12:30", "14:00", false)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.fullDay").value(false))
			.andExpect(jsonPath("$.startTime").value("12:30:00"));
	}

	@Test
	void rejectsPastDate() throws Exception {
		Clinic clinic = clinicWithDefaultHours("REG-DL4");
		User doc = doctor(clinic.getId(), "dl-doc4@example.com");

		mockMvc.perform(post("/api/v1/me/leaves").header("Authorization", "Bearer " + tokenFor(doc))
				.contentType(MediaType.APPLICATION_JSON).content(fullDayLeaveJson(LocalDate.now().minusDays(1))))
			.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsEndTimeNotAfterStartTime() throws Exception {
		Clinic clinic = clinicWithDefaultHours("REG-DL5");
		User doc = doctor(clinic.getId(), "dl-doc5@example.com");

		mockMvc.perform(post("/api/v1/me/leaves").header("Authorization", "Bearer " + tokenFor(doc))
				.contentType(MediaType.APPLICATION_JSON).content(partialLeaveJson(nextMonday(), "14:00", "12:30", false)))
			.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsNonThirtyMinuteAlignedWindow() throws Exception {
		Clinic clinic = clinicWithDefaultHours("REG-DL6");
		User doc = doctor(clinic.getId(), "dl-doc6@example.com");

		mockMvc.perform(post("/api/v1/me/leaves").header("Authorization", "Bearer " + tokenFor(doc))
				.contentType(MediaType.APPLICATION_JSON).content(partialLeaveJson(nextMonday(), "12:15", "14:00", false)))
			.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsOverlapWithExistingLeaveIdentifyingTheOverlap() throws Exception {
		Clinic clinic = clinicWithDefaultHours("REG-DL7");
		User doc = doctor(clinic.getId(), "dl-doc7@example.com");
		LocalDate monday = nextMonday();
		String token = tokenFor(doc);

		mockMvc.perform(post("/api/v1/me/leaves").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(partialLeaveJson(monday, "12:00", "13:00", false)))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/me/leaves").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(partialLeaveJson(monday, "12:30", "13:30", false)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(monday.toString())));
	}

	@Test
	void reportsConflictWithScheduledAppointmentWithoutPersistingUntilConfirmed() throws Exception {
		Clinic clinic = clinicWithDefaultHours("REG-DL8");
		User doc = doctor(clinic.getId(), "dl-doc8@example.com");
		User pat = patient("dl-pat8@example.com");
		LocalDate monday = nextMonday();
		Appointment appt = appointmentRepository.saveAndFlush(
				new Appointment(pat.getId(), doc.getId(), clinic.getId(), monday, LocalTime.of(9, 0), (short) 30));
		String token = tokenFor(doc);

		mockMvc.perform(post("/api/v1/me/leaves").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(fullDayLeaveJson(monday)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.conflictingAppointments[0].id").value(appt.getId().toString()));

		org.assertj.core.api.Assertions.assertThat(appointmentRepository.findById(appt.getId()).orElseThrow().getState())
			.isEqualTo(AppointmentState.SCHEDULED);

		mockMvc.perform(post("/api/v1/me/leaves").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"date":"%s","fullDay":true,"confirmCancelConflicts":true}
					""".formatted(monday)))
			.andExpect(status().isCreated());

		org.assertj.core.api.Assertions.assertThat(appointmentRepository.findById(appt.getId()).orElseThrow().getState())
			.isEqualTo(AppointmentState.CANCELLED);
	}
}
