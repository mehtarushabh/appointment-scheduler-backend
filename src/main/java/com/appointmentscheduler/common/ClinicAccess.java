package com.appointmentscheduler.common;

import java.util.UUID;

import com.appointmentscheduler.model.Role;
import com.appointmentscheduler.security.AuthenticatedPrincipal;

/**
 * Shared clinic-scoped authorization check (FR-010): a Clinic Admin may only act on their own
 * clinic's resources. Used by every clinic-scoped controller (Doctor, Patient, Clinic Admin).
 */
public final class ClinicAccess {

	private ClinicAccess() {
	}

	public static void requireClinicAdminOf(AuthenticatedPrincipal principal, UUID clinicId) {
		if (principal.role() != Role.CLINIC_ADMIN || !clinicId.equals(principal.clinicId())) {
			throw new ForbiddenException("You do not have access to this clinic.");
		}
	}
}
