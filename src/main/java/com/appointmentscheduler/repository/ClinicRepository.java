package com.appointmentscheduler.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.appointmentscheduler.model.Clinic;

public interface ClinicRepository extends JpaRepository<Clinic, UUID> {

	boolean existsByRegisteredId(String registeredId);
}
