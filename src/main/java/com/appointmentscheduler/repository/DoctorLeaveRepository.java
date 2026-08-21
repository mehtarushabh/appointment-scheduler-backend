package com.appointmentscheduler.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.appointmentscheduler.model.DoctorLeave;

public interface DoctorLeaveRepository extends JpaRepository<DoctorLeave, UUID> {

	List<DoctorLeave> findByDoctorId(UUID doctorId);
}
