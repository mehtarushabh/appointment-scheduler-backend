package com.appointmentscheduler.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.appointmentscheduler.model.ClinicPatientAssociation;

public interface ClinicPatientAssociationRepository extends JpaRepository<ClinicPatientAssociation, UUID> {

	boolean existsByClinicIdAndPatientId(UUID clinicId, UUID patientId);

	List<ClinicPatientAssociation> findByClinicId(UUID clinicId);

	List<ClinicPatientAssociation> findByPatientId(UUID patientId);
}
