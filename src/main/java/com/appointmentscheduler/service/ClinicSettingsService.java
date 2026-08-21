package com.appointmentscheduler.service;

import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appointmentscheduler.common.BadRequestException;
import com.appointmentscheduler.common.NotFoundException;
import com.appointmentscheduler.dto.ClinicDtos.ClinicProfileUpdateRequest;
import com.appointmentscheduler.dto.ClinicDtos.ClinicResponse;
import com.appointmentscheduler.dto.UserResponse;
import com.appointmentscheduler.dto.WorkingHoursDtos.WorkingHoursEntry;
import com.appointmentscheduler.dto.WorkingHoursDtos.WorkingHoursUpdateRequest;
import com.appointmentscheduler.model.Clinic;
import com.appointmentscheduler.model.ClinicWorkingHours;
import com.appointmentscheduler.model.Role;
import com.appointmentscheduler.model.User;
import com.appointmentscheduler.repository.ClinicRepository;
import com.appointmentscheduler.repository.ClinicWorkingHoursRepository;
import com.appointmentscheduler.repository.UserRepository;

/** A Clinic Admin's own-clinic profile and working-hours management (User Story 1). */
@Service
public class ClinicSettingsService {

	private final ClinicRepository clinicRepository;
	private final ClinicWorkingHoursRepository clinicWorkingHoursRepository;
	private final UserRepository userRepository;

	public ClinicSettingsService(ClinicRepository clinicRepository, ClinicWorkingHoursRepository clinicWorkingHoursRepository,
			UserRepository userRepository) {
		this.clinicRepository = clinicRepository;
		this.clinicWorkingHoursRepository = clinicWorkingHoursRepository;
		this.userRepository = userRepository;
	}

	public ClinicResponse getProfile(UUID clinicId) {
		Clinic clinic = clinicRepository.findById(clinicId).orElseThrow(() -> new NotFoundException("Clinic not found."));
		return toClinicResponse(clinic);
	}

	/** FR-003: the request has no registeredId field at all, so it can never be changed here. */
	@Transactional
	public ClinicResponse updateProfile(UUID clinicId, ClinicProfileUpdateRequest request) {
		Clinic clinic = clinicRepository.findById(clinicId).orElseThrow(() -> new NotFoundException("Clinic not found."));
		clinic.setName(request.name());
		clinic.setAddress(request.address().toModel());
		return toClinicResponse(clinic);
	}

	/** Always returned Monday-first (DayOfWeek's declared enum order), matching the table UI. */
	public List<WorkingHoursEntry> getWorkingHours(UUID clinicId) {
		return clinicWorkingHoursRepository.findByClinicId(clinicId).stream()
			.sorted(Comparator.comparing(ClinicWorkingHours::getDayOfWeek))
			.map(WorkingHoursEntry::from)
			.toList();
	}

	/** FR-005/FR-007: all 7 days are validated and replaced together, or none are. */
	@Transactional
	public List<WorkingHoursEntry> replaceWorkingHours(UUID clinicId, WorkingHoursUpdateRequest request) {
		for (WorkingHoursEntry entry : request.days()) {
			if (entry.isOpen() && (entry.startTime() == null || entry.endTime() == null || !entry.startTime().isBefore(entry.endTime()))) {
				throw new BadRequestException(entry.dayOfWeek() + "'s end time must be after its start time.");
			}
		}

		Map<DayOfWeek, ClinicWorkingHours> existingByDay = clinicWorkingHoursRepository.findByClinicId(clinicId).stream()
			.collect(Collectors.toMap(ClinicWorkingHours::getDayOfWeek, hours -> hours));

		for (WorkingHoursEntry entry : request.days()) {
			ClinicWorkingHours hours = existingByDay.get(entry.dayOfWeek());
			if (hours == null) {
				throw new NotFoundException("Working hours for " + entry.dayOfWeek() + " not found.");
			}
			hours.replace(entry.isOpen(), entry.isOpen() ? entry.startTime() : null, entry.isOpen() ? entry.endTime() : null);
		}

		return getWorkingHours(clinicId);
	}

	private ClinicResponse toClinicResponse(Clinic clinic) {
		User admin = userRepository.findByClinicIdAndRole(clinic.getId(), Role.CLINIC_ADMIN).stream().findFirst().orElse(null);
		UserResponse adminResponse = admin != null ? UserResponse.from(admin) : null;
		return ClinicResponse.from(clinic, adminResponse);
	}
}
