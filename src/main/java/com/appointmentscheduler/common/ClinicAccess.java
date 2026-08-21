package com.appointmentscheduler.common;

import com.appointmentscheduler.model.Role;
import com.appointmentscheduler.security.AuthenticatedPrincipal;

/**
 * Shared clinic-scoped authorization check (FR-010): endpoints under /api/v1/clinics/me act on
 * the caller's own clinic, taken from the principal rather than a caller-supplied id, so there is
 * no clinic id left to check against.
 */
public final class ClinicAccess {

	private ClinicAccess() {
	}

	public static void requireClinicAdmin(AuthenticatedPrincipal principal) {
		if (principal.role() != Role.CLINIC_ADMIN) {
			throw new ForbiddenException("You do not have access to this clinic.");
		}
	}
}
