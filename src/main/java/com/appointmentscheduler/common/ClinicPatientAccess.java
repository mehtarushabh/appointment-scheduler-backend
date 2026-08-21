package com.appointmentscheduler.common;

import java.util.UUID;

import com.appointmentscheduler.model.Role;
import com.appointmentscheduler.repository.ClinicPatientAssociationRepository;
import com.appointmentscheduler.security.AuthenticatedPrincipal;

/**
 * Shared authorization check for endpoints available to a clinic's own Clinic Admin OR a Patient
 * associated with that clinic (e.g. booking options, working hours) — mirrors ClinicAccess, but
 * for the broader caller set these particular endpoints serve.
 */
public final class ClinicPatientAccess {

	private ClinicPatientAccess() {
	}

	public static void requireAssociatedPatientOrClinicAdminOf(AuthenticatedPrincipal principal, UUID clinicId,
			ClinicPatientAssociationRepository associationRepository) {
		boolean isOwningClinicAdmin = principal.role() == Role.CLINIC_ADMIN && clinicId.equals(principal.clinicId());
		boolean isAssociatedPatient = principal.role() == Role.PATIENT
				&& associationRepository.existsByClinicIdAndPatientId(clinicId, principal.userId());
		if (!isOwningClinicAdmin && !isAssociatedPatient) {
			throw new ForbiddenException("You are not associated with this clinic.");
		}
	}
}
