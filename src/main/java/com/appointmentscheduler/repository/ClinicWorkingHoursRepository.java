package com.appointmentscheduler.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.appointmentscheduler.model.ClinicWorkingHours;

public interface ClinicWorkingHoursRepository extends JpaRepository<ClinicWorkingHours, UUID> {

	List<ClinicWorkingHours> findByClinicId(UUID clinicId);
}
