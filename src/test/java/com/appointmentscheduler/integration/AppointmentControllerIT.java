package com.appointmentscheduler.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
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

class AppointmentControllerIT extends AbstractIntegrationTest {

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

	private static LocalDate nextMonday() {
		return LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
	}

	private static String bookingJson(UUID clinicId, UUID doctorId, LocalDate date, String startTime, int durationMinutes) {
		return """
			{"clinicId":"%s","doctorId":"%s","date":"%s","startTime":"%s","durationMinutes":%d}
			""".formatted(clinicId, doctorId, date, startTime, durationMinutes);
	}

	@Test
	void booksAppointmentSuccessfully() throws Exception {
		Clinic clinic = clinicWithDefaultHours("REG-AC1");
		User doc = doctor(clinic.getId(), "ac-doc1@example.com");
		User pat = patient("ac-pat1@example.com");
		associationRepository.saveAndFlush(new ClinicPatientAssociation(clinic.getId(), pat.getId()));
		LocalDate monday = nextMonday();

		mockMvc.perform(post("/api/v1/appointments").header("Authorization", "Bearer " + tokenFor(pat))
				.contentType(MediaType.APPLICATION_JSON).content(bookingJson(clinic.getId(), doc.getId(), monday, "09:00", 30)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.state").value("SCHEDULED"))
			.andExpect(jsonPath("$.doctorName").value("Dana Doc"));
	}

	@Test
	void rejectsInvalidDuration() throws Exception {
		Clinic clinic = clinicWithDefaultHours("REG-AC2");
		User doc = doctor(clinic.getId(), "ac-doc2@example.com");
		User pat = patient("ac-pat2@example.com");
		associationRepository.saveAndFlush(new ClinicPatientAssociation(clinic.getId(), pat.getId()));

		mockMvc.perform(post("/api/v1/appointments").header("Authorization", "Bearer " + tokenFor(pat))
				.contentType(MediaType.APPLICATION_JSON).content(bookingJson(clinic.getId(), doc.getId(), nextMonday(), "09:00", 45)))
			.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsBookingOutsideClinicHours() throws Exception {
		Clinic clinic = clinicWithDefaultHours("REG-AC3");
		User doc = doctor(clinic.getId(), "ac-doc3@example.com");
		User pat = patient("ac-pat3@example.com");
		associationRepository.saveAndFlush(new ClinicPatientAssociation(clinic.getId(), pat.getId()));

		mockMvc.perform(post("/api/v1/appointments").header("Authorization", "Bearer " + tokenFor(pat))
				.contentType(MediaType.APPLICATION_JSON).content(bookingJson(clinic.getId(), doc.getId(), nextMonday(), "07:00", 30)))
			.andExpect(status().isConflict());
	}

	@Test
	void rejectsBookingInsideDoctorLeave() throws Exception {
		Clinic clinic = clinicWithDefaultHours("REG-AC4");
		User doc = doctor(clinic.getId(), "ac-doc4@example.com");
		User pat = patient("ac-pat4@example.com");
		associationRepository.saveAndFlush(new ClinicPatientAssociation(clinic.getId(), pat.getId()));
		LocalDate monday = nextMonday();
		doctorLeaveRepository.saveAndFlush(new DoctorLeave(doc.getId(), monday, true, null, null));

		mockMvc.perform(post("/api/v1/appointments").header("Authorization", "Bearer " + tokenFor(pat))
				.contentType(MediaType.APPLICATION_JSON).content(bookingJson(clinic.getId(), doc.getId(), monday, "09:00", 30)))
			.andExpect(status().isConflict());
	}

	@Test
	void rejectsBookingOverlappingExistingAppointment() throws Exception {
		Clinic clinic = clinicWithDefaultHours("REG-AC5");
		User doc = doctor(clinic.getId(), "ac-doc5@example.com");
		User pat = patient("ac-pat5@example.com");
		associationRepository.saveAndFlush(new ClinicPatientAssociation(clinic.getId(), pat.getId()));
		LocalDate monday = nextMonday();
		appointmentRepository.saveAndFlush(new Appointment(pat.getId(), doc.getId(), clinic.getId(), monday, LocalTime.of(9, 0), (short) 30));

		mockMvc.perform(post("/api/v1/appointments").header("Authorization", "Bearer " + tokenFor(pat))
				.contentType(MediaType.APPLICATION_JSON).content(bookingJson(clinic.getId(), doc.getId(), monday, "09:00", 30)))
			.andExpect(status().isConflict());
	}

	@Test
	void exactlyOneOfTwoConcurrentBookingsForTheSameSlotSucceeds() throws Exception {
		Clinic clinic = clinicWithDefaultHours("REG-AC6");
		User doc = doctor(clinic.getId(), "ac-doc6@example.com");
		User pat1 = patient("ac-pat6a@example.com");
		User pat2 = patient("ac-pat6b@example.com");
		associationRepository.saveAndFlush(new ClinicPatientAssociation(clinic.getId(), pat1.getId()));
		associationRepository.saveAndFlush(new ClinicPatientAssociation(clinic.getId(), pat2.getId()));
		LocalDate monday = nextMonday();
		String body = bookingJson(clinic.getId(), doc.getId(), monday, "09:00", 30);

		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch readyLatch = new CountDownLatch(2);
		CountDownLatch goLatch = new CountDownLatch(1);

		Future<MvcResult> resultA = executor.submit(() -> race(tokenFor(pat1), body, readyLatch, goLatch));
		Future<MvcResult> resultB = executor.submit(() -> race(tokenFor(pat2), body, readyLatch, goLatch));

		readyLatch.await(10, TimeUnit.SECONDS);
		goLatch.countDown();

		int statusA = resultA.get(10, TimeUnit.SECONDS).getResponse().getStatus();
		int statusB = resultB.get(10, TimeUnit.SECONDS).getResponse().getStatus();
		executor.shutdown();

		org.assertj.core.api.Assertions.assertThat(List.of(statusA, statusB)).containsExactlyInAnyOrder(201, 409);
	}

	private MvcResult race(String token, String body, CountDownLatch readyLatch, CountDownLatch goLatch) throws Exception {
		readyLatch.countDown();
		goLatch.await(10, TimeUnit.SECONDS);
		return mockMvc.perform(post("/api/v1/appointments").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(body))
			.andReturn();
	}

	@Test
	void listsOnlyOwnAppointmentsForPatient() throws Exception {
		Clinic clinic = clinicWithDefaultHours("REG-AC7");
		User doc = doctor(clinic.getId(), "ac-doc7@example.com");
		User pat1 = patient("ac-pat7a@example.com");
		User pat2 = patient("ac-pat7b@example.com");
		LocalDate monday = nextMonday();
		appointmentRepository.saveAndFlush(new Appointment(pat1.getId(), doc.getId(), clinic.getId(), monday, LocalTime.of(9, 0), (short) 30));
		appointmentRepository.saveAndFlush(new Appointment(pat2.getId(), doc.getId(), clinic.getId(), monday, LocalTime.of(10, 0), (short) 30));

		mockMvc.perform(get("/api/v1/me/appointments").header("Authorization", "Bearer " + tokenFor(pat1)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].patientId").value(pat1.getId().toString()));
	}

	@Test
	void rejectsMyAppointmentsForClinicAdmin() throws Exception {
		Clinic clinic = clinicWithDefaultHours("REG-AC8");
		User admin = userRepository.saveAndFlush(new User("Cara", "Admin", "ac-admin8@example.com", passwordEncoder.encode("password"),
				LocalDate.of(1985, 1, 1), sampleAddress(), Role.CLINIC_ADMIN, clinic.getId(), null));

		mockMvc.perform(get("/api/v1/me/appointments").header("Authorization", "Bearer " + tokenFor(admin)))
			.andExpect(status().isForbidden());
	}

	@Test
	void listsOnlyOwnAppointmentsForDoctor() throws Exception {
		Clinic clinic = clinicWithDefaultHours("REG-AC9");
		User doc1 = doctor(clinic.getId(), "ac-doc9a@example.com");
		User doc2 = doctor(clinic.getId(), "ac-doc9b@example.com");
		User pat = patient("ac-pat9@example.com");
		LocalDate monday = nextMonday();
		appointmentRepository.saveAndFlush(new Appointment(pat.getId(), doc1.getId(), clinic.getId(), monday, LocalTime.of(9, 0), (short) 30));
		appointmentRepository.saveAndFlush(new Appointment(pat.getId(), doc2.getId(), clinic.getId(), monday, LocalTime.of(10, 0), (short) 30));

		mockMvc.perform(get("/api/v1/me/appointments").header("Authorization", "Bearer " + tokenFor(doc1)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].doctorId").value(doc1.getId().toString()));
	}

	@Test
	void clinicAdminCancelsAppointment() throws Exception {
		Clinic clinic = clinicWithDefaultHours("REG-AC10");
		User admin = userRepository.saveAndFlush(new User("Cara", "Admin", "ac-admin10@example.com", passwordEncoder.encode("password"),
				LocalDate.of(1985, 1, 1), sampleAddress(), Role.CLINIC_ADMIN, clinic.getId(), null));
		User doc = doctor(clinic.getId(), "ac-doc10@example.com");
		User pat = patient("ac-pat10@example.com");
		Appointment appt = appointmentRepository.saveAndFlush(
				new Appointment(pat.getId(), doc.getId(), clinic.getId(), nextMonday(), LocalTime.of(9, 0), (short) 30));

		mockMvc.perform(patch("/api/v1/appointments/" + appt.getId() + "/cancel").header("Authorization", "Bearer " + tokenFor(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.state").value("CANCELLED"));
	}

	@Test
	void owningDoctorCancelsAppointment() throws Exception {
		Clinic clinic = clinicWithDefaultHours("REG-AC11");
		User doc = doctor(clinic.getId(), "ac-doc11@example.com");
		User pat = patient("ac-pat11@example.com");
		Appointment appt = appointmentRepository.saveAndFlush(
				new Appointment(pat.getId(), doc.getId(), clinic.getId(), nextMonday(), LocalTime.of(9, 0), (short) 30));

		mockMvc.perform(patch("/api/v1/appointments/" + appt.getId() + "/cancel").header("Authorization", "Bearer " + tokenFor(doc)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.state").value("CANCELLED"));
	}

	@Test
	void rejectsCancelForUnrelatedClinicAdminAndDoctor() throws Exception {
		Clinic clinic = clinicWithDefaultHours("REG-AC12");
		Clinic otherClinic = clinicWithDefaultHours("REG-AC12B");
		User doc = doctor(clinic.getId(), "ac-doc12@example.com");
		User otherDoc = doctor(otherClinic.getId(), "ac-doc12b@example.com");
		User otherAdmin = userRepository.saveAndFlush(new User("Other", "Admin", "ac-admin12@example.com", passwordEncoder.encode("password"),
				LocalDate.of(1985, 1, 1), sampleAddress(), Role.CLINIC_ADMIN, otherClinic.getId(), null));
		User pat = patient("ac-pat12@example.com");
		Appointment appt = appointmentRepository.saveAndFlush(
				new Appointment(pat.getId(), doc.getId(), clinic.getId(), nextMonday(), LocalTime.of(9, 0), (short) 30));

		mockMvc.perform(patch("/api/v1/appointments/" + appt.getId() + "/cancel").header("Authorization", "Bearer " + tokenFor(otherDoc)))
			.andExpect(status().isForbidden());
		mockMvc.perform(patch("/api/v1/appointments/" + appt.getId() + "/cancel").header("Authorization", "Bearer " + tokenFor(otherAdmin)))
			.andExpect(status().isForbidden());
	}

	@Test
	void rejectsCancelWhenAlreadyCancelled() throws Exception {
		Clinic clinic = clinicWithDefaultHours("REG-AC13");
		User doc = doctor(clinic.getId(), "ac-doc13@example.com");
		User pat = patient("ac-pat13@example.com");
		Appointment appt = appointmentRepository.saveAndFlush(
				new Appointment(pat.getId(), doc.getId(), clinic.getId(), nextMonday(), LocalTime.of(9, 0), (short) 30));

		mockMvc.perform(patch("/api/v1/appointments/" + appt.getId() + "/cancel").header("Authorization", "Bearer " + tokenFor(doc)))
			.andExpect(status().isOk());
		mockMvc.perform(patch("/api/v1/appointments/" + appt.getId() + "/cancel").header("Authorization", "Bearer " + tokenFor(doc)))
			.andExpect(status().isConflict());
	}

	@Test
	void owningDoctorCompletesAppointment() throws Exception {
		Clinic clinic = clinicWithDefaultHours("REG-AC14");
		User doc = doctor(clinic.getId(), "ac-doc14@example.com");
		User pat = patient("ac-pat14@example.com");
		Appointment appt = appointmentRepository.saveAndFlush(
				new Appointment(pat.getId(), doc.getId(), clinic.getId(), nextMonday(), LocalTime.of(9, 0), (short) 30));

		mockMvc.perform(patch("/api/v1/appointments/" + appt.getId() + "/complete").header("Authorization", "Bearer " + tokenFor(doc)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.state").value("COMPLETED"));
	}

	@Test
	void rejectsCompleteWhenAlreadyCompleted() throws Exception {
		Clinic clinic = clinicWithDefaultHours("REG-AC15");
		User doc = doctor(clinic.getId(), "ac-doc15@example.com");
		User pat = patient("ac-pat15@example.com");
		Appointment appt = appointmentRepository.saveAndFlush(
				new Appointment(pat.getId(), doc.getId(), clinic.getId(), nextMonday(), LocalTime.of(9, 0), (short) 30));

		mockMvc.perform(patch("/api/v1/appointments/" + appt.getId() + "/complete").header("Authorization", "Bearer " + tokenFor(doc)))
			.andExpect(status().isOk());
		mockMvc.perform(patch("/api/v1/appointments/" + appt.getId() + "/complete").header("Authorization", "Bearer " + tokenFor(doc)))
			.andExpect(status().isConflict());
	}

	@Test
	void listsAllClinicAppointmentsForClinicAdmin() throws Exception {
		Clinic clinic = clinicWithDefaultHours("REG-AC16");
		User admin = userRepository.saveAndFlush(new User("Cara", "Admin", "ac-admin16@example.com", passwordEncoder.encode("password"),
				LocalDate.of(1985, 1, 1), sampleAddress(), Role.CLINIC_ADMIN, clinic.getId(), null));
		User doc = doctor(clinic.getId(), "ac-doc16@example.com");
		User pat = patient("ac-pat16@example.com");
		LocalDate monday = nextMonday();
		appointmentRepository.saveAndFlush(new Appointment(pat.getId(), doc.getId(), clinic.getId(), monday, LocalTime.of(9, 0), (short) 30));
		appointmentRepository.saveAndFlush(new Appointment(pat.getId(), doc.getId(), clinic.getId(), monday, LocalTime.of(10, 0), (short) 30));

		mockMvc.perform(get("/api/v1/clinics/me/appointments").header("Authorization", "Bearer " + tokenFor(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void excludesAnotherClinicsAppointmentsForAnotherClinicAdmin() throws Exception {
		Clinic clinic = clinicWithDefaultHours("REG-AC17");
		Clinic otherClinic = clinicWithDefaultHours("REG-AC17B");
		User doc = doctor(clinic.getId(), "ac-doc17@example.com");
		User pat = patient("ac-pat17@example.com");
		appointmentRepository.saveAndFlush(new Appointment(pat.getId(), doc.getId(), clinic.getId(), nextMonday(), LocalTime.of(9, 0), (short) 30));
		User otherAdmin = userRepository.saveAndFlush(new User("Other", "Admin", "ac-admin17@example.com", passwordEncoder.encode("password"),
				LocalDate.of(1985, 1, 1), sampleAddress(), Role.CLINIC_ADMIN, otherClinic.getId(), null));

		mockMvc.perform(get("/api/v1/clinics/me/appointments").header("Authorization", "Bearer " + tokenFor(otherAdmin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
	}
}
