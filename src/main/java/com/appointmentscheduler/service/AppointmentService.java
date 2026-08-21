package com.appointmentscheduler.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appointmentscheduler.common.AppointmentAccess;
import com.appointmentscheduler.common.BadRequestException;
import com.appointmentscheduler.common.ConflictException;
import com.appointmentscheduler.common.ForbiddenException;
import com.appointmentscheduler.common.NotFoundException;
import com.appointmentscheduler.dto.AppointmentDtos.AppointmentResponse;
import com.appointmentscheduler.dto.AppointmentDtos.BookAppointmentRequest;
import com.appointmentscheduler.model.Appointment;
import com.appointmentscheduler.model.AppointmentSlot;
import com.appointmentscheduler.model.Clinic;
import com.appointmentscheduler.model.Role;
import com.appointmentscheduler.model.User;
import com.appointmentscheduler.repository.AppointmentRepository;
import com.appointmentscheduler.repository.AppointmentSlotRepository;
import com.appointmentscheduler.repository.ClinicPatientAssociationRepository;
import com.appointmentscheduler.repository.ClinicRepository;
import com.appointmentscheduler.repository.UserRepository;
import com.appointmentscheduler.security.AuthenticatedPrincipal;

/** Books, lists, cancels, and completes Appointments (User Stories 2 and 4). */
@Service
public class AppointmentService {

	private static final int SLOT_MINUTES = 30;
	private static final List<Integer> VALID_DURATIONS = List.of(30, 60);

	private final AppointmentRepository appointmentRepository;
	private final AppointmentSlotRepository appointmentSlotRepository;
	private final ClinicPatientAssociationRepository clinicPatientAssociationRepository;
	private final UserRepository userRepository;
	private final ClinicRepository clinicRepository;
	private final AppointmentAvailabilityService availabilityService;
	private final AppointmentCancellationEmailService cancellationEmailService;

	public AppointmentService(AppointmentRepository appointmentRepository, AppointmentSlotRepository appointmentSlotRepository,
			ClinicPatientAssociationRepository clinicPatientAssociationRepository, UserRepository userRepository,
			ClinicRepository clinicRepository, AppointmentAvailabilityService availabilityService,
			AppointmentCancellationEmailService cancellationEmailService) {
		this.appointmentRepository = appointmentRepository;
		this.appointmentSlotRepository = appointmentSlotRepository;
		this.clinicPatientAssociationRepository = clinicPatientAssociationRepository;
		this.userRepository = userRepository;
		this.clinicRepository = clinicRepository;
		this.availabilityService = availabilityService;
		this.cancellationEmailService = cancellationEmailService;
	}

	/**
	 * FR-019–FR-021: the upfront availability check gives a clear message on the common path; the
	 * AppointmentSlot UNIQUE constraint (research.md #3) is the actual race-safety net — a
	 * concurrent double-booking surfaces as a DataIntegrityViolationException at commit, translated
	 * to 409 by GlobalExceptionHandler, not caught here.
	 */
	@Transactional
	public AppointmentResponse book(UUID patientId, BookAppointmentRequest request) {
		if (!VALID_DURATIONS.contains(request.durationMinutes())) {
			throw new BadRequestException("Duration must be 30 or 60 minutes.");
		}
		if (request.date().isBefore(LocalDate.now())) {
			throw new BadRequestException("Cannot book an appointment in the past.");
		}
		if (!clinicPatientAssociationRepository.existsByClinicIdAndPatientId(request.clinicId(), patientId)) {
			throw new ForbiddenException("You are not associated with this clinic.");
		}
		User doctor = userRepository.findById(request.doctorId())
			.filter(user -> user.getRole() == Role.DOCTOR)
			.orElseThrow(() -> new NotFoundException("Doctor not found."));
		if (!doctor.getClinicId().equals(request.clinicId())) {
			throw new BadRequestException("This doctor does not belong to the requested clinic.");
		}
		if (!availabilityService.isAvailable(request.clinicId(), request.doctorId(), request.date(), request.startTime(),
				request.durationMinutes())) {
			throw new ConflictException("The requested time is no longer available.");
		}

		Appointment appointment = appointmentRepository.save(new Appointment(patientId, request.doctorId(), request.clinicId(),
				request.date(), request.startTime(), (short) request.durationMinutes()));
		appointmentSlotRepository.saveAll(slotStartTimes(request.startTime(), request.durationMinutes()).stream()
			.map(slotStart -> new AppointmentSlot(appointment.getId(), request.doctorId(), request.date(), slotStart))
			.toList());

		User patient = userRepository.findById(patientId).orElseThrow();
		Clinic clinic = clinicRepository.findById(request.clinicId()).orElseThrow();
		return AppointmentResponse.from(appointment, patient, doctor, clinic);
	}

	public List<AppointmentResponse> listForPatient(UUID patientId) {
		return appointmentRepository.findByPatientId(patientId).stream().map(this::toResponse).toList();
	}

	public List<AppointmentResponse> listForDoctor(UUID doctorId) {
		return appointmentRepository.findByDoctorId(doctorId).stream().map(this::toResponse).toList();
	}

	public List<AppointmentResponse> listForClinic(UUID clinicId) {
		return appointmentRepository.findByClinicId(clinicId).stream().map(this::toResponse).toList();
	}

	/**
	 * FR-023/FR-024/FR-027. Also called by DoctorLeaveService (User Story 3) for each appointment a
	 * new leave conflicts with — the doctor's own principal authorizes there exactly as it does
	 * here, so this is the one cancellation code path both stories share (no duplicate logic).
	 */
	@Transactional
	public AppointmentResponse cancel(UUID appointmentId, AuthenticatedPrincipal principal) {
		Appointment appointment = findAppointmentOrThrow(appointmentId);
		AppointmentAccess.requireManagerOf(principal, appointment);
		appointment.cancel();
		User patient = userRepository.findById(appointment.getPatientId()).orElseThrow();
		cancellationEmailService.sendCancellationEmail(patient, appointment);
		return toResponse(appointment);
	}

	/** FR-026/FR-027. */
	@Transactional
	public AppointmentResponse complete(UUID appointmentId, AuthenticatedPrincipal principal) {
		Appointment appointment = findAppointmentOrThrow(appointmentId);
		AppointmentAccess.requireManagerOf(principal, appointment);
		appointment.complete();
		return toResponse(appointment);
	}

	private Appointment findAppointmentOrThrow(UUID appointmentId) {
		return appointmentRepository.findById(appointmentId).orElseThrow(() -> new NotFoundException("Appointment not found."));
	}

	private static List<LocalTime> slotStartTimes(LocalTime startTime, int durationMinutes) {
		List<LocalTime> slots = new ArrayList<>();
		for (LocalTime t = startTime; t.isBefore(startTime.plusMinutes(durationMinutes)); t = t.plusMinutes(SLOT_MINUTES)) {
			slots.add(t);
		}
		return slots;
	}

	private AppointmentResponse toResponse(Appointment appointment) {
		User patient = userRepository.findById(appointment.getPatientId()).orElseThrow();
		User doctor = userRepository.findById(appointment.getDoctorId()).orElseThrow();
		Clinic clinic = clinicRepository.findById(appointment.getClinicId()).orElseThrow();
		return AppointmentResponse.from(appointment, patient, doctor, clinic);
	}
}
