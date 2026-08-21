package com.appointmentscheduler.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.appointmentscheduler.model.Address;
import com.appointmentscheduler.model.Appointment;
import com.appointmentscheduler.model.Clinic;
import com.appointmentscheduler.model.ClinicPatientAssociation;
import com.appointmentscheduler.model.ClinicWorkingHours;
import com.appointmentscheduler.model.DoctorLeave;
import com.appointmentscheduler.model.Role;
import com.appointmentscheduler.model.User;
import com.appointmentscheduler.repository.AppointmentRepository;
import com.appointmentscheduler.repository.ClinicPatientAssociationRepository;
import com.appointmentscheduler.repository.ClinicRepository;
import com.appointmentscheduler.repository.ClinicWorkingHoursRepository;
import com.appointmentscheduler.repository.DoctorLeaveRepository;
import com.appointmentscheduler.repository.UserRepository;
import com.appointmentscheduler.security.TokenService;

class DoctorBookingOptionsControllerIT extends AbstractIntegrationTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ClinicRepository clinicRepository;

	@Autowired
	private ClinicWorkingHoursRepository clinicWorkingHoursRepository;

	@Autowired
	private ClinicPatientAssociationRepository associationRepository;

	@Autowired
	private DoctorLeaveRepository doctorLeaveRepository;

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

	@Test
	void listsMinimalBookableDoctorsForAssociatedPatient() throws Exception {
		Clinic clinic = clinicWithDefaultHours("REG-DBO1");
		User doc = doctor(clinic.getId(), "dbo-doc1@example.com");
		User pat = patient("dbo-pat1@example.com");
		associationRepository.saveAndFlush(new ClinicPatientAssociation(clinic.getId(), pat.getId()));

		mockMvc.perform(get("/api/v1/clinics/" + clinic.getId() + "/doctors/bookable").header("Authorization", "Bearer " + tokenFor(pat)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(doc.getId().toString()))
			.andExpect(jsonPath("$[0].specialty").value("Cardiology"))
			.andExpect(jsonPath("$[0].email").doesNotExist());
	}

	@Test
	void rejectsBookableDoctorsForUnrelatedPatient() throws Exception {
		Clinic clinic = clinicWithDefaultHours("REG-DBO2");
		User unrelatedPatient = patient("dbo-pat2@example.com");

		mockMvc.perform(get("/api/v1/clinics/" + clinic.getId() + "/doctors/bookable")
				.header("Authorization", "Bearer " + tokenFor(unrelatedPatient)))
			.andExpect(status().isForbidden());
	}

	@Test
	void computesAvailableSlotsSubtractingLeaveAndExistingAppointment() throws Exception {
		Clinic clinic = clinicWithDefaultHours("REG-DBO3");
		User doc = doctor(clinic.getId(), "dbo-doc3@example.com");
		User pat = patient("dbo-pat3@example.com");
		associationRepository.saveAndFlush(new ClinicPatientAssociation(clinic.getId(), pat.getId()));
		LocalDate monday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

		doctorLeaveRepository.saveAndFlush(new DoctorLeave(doc.getId(), monday, false, LocalTime.of(10, 0), LocalTime.of(11, 0)));
		Appointment existing = new Appointment(pat.getId(), doc.getId(), clinic.getId(), monday, LocalTime.of(13, 0), (short) 30);
		appointmentRepository.saveAndFlush(existing);

		mockMvc.perform(get("/api/v1/clinics/" + clinic.getId() + "/doctors/" + doc.getId() + "/available-slots")
				.param("date", monday.toString())
				.param("durationMinutes", "30")
				.header("Authorization", "Bearer " + tokenFor(pat)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.startTimes").isArray())
			.andExpect(jsonPath("$.startTimes", org.hamcrest.Matchers.hasItem("08:00:00")))
			.andExpect(jsonPath("$.startTimes", org.hamcrest.Matchers.hasItem("11:00:00")))
			.andExpect(jsonPath("$.startTimes", org.hamcrest.Matchers.hasItem("13:30:00")))
			.andExpect(jsonPath("$.startTimes", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("10:00:00"))))
			.andExpect(jsonPath("$.startTimes", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("10:30:00"))))
			.andExpect(jsonPath("$.startTimes", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("13:00:00"))));
	}
}
