package com.appointmentscheduler.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.appointmentscheduler.model.Appointment;
import com.appointmentscheduler.model.AppointmentState;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

	List<Appointment> findByPatientId(UUID patientId);

	List<Appointment> findByDoctorId(UUID doctorId);

	List<Appointment> findByClinicId(UUID clinicId);

	List<Appointment> findByDoctorIdAndDateAndState(UUID doctorId, LocalDate date, AppointmentState state);
}
