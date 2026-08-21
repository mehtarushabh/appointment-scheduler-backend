package com.appointmentscheduler.common;

import com.appointmentscheduler.model.Appointment;
import com.appointmentscheduler.model.Role;
import com.appointmentscheduler.security.AuthenticatedPrincipal;

/**
 * Shared appointment-scoped authorization check, mirroring ClinicAccess: a Clinic Admin may act on
 * any appointment belonging to their own clinic; a Doctor only on their own appointments.
 */
public final class AppointmentAccess {

	private AppointmentAccess() {
	}

	public static void requireManagerOf(AuthenticatedPrincipal principal, Appointment appointment) {
		boolean isOwningClinicAdmin = principal.role() == Role.CLINIC_ADMIN && appointment.getClinicId().equals(principal.clinicId());
		boolean isOwningDoctor = principal.role() == Role.DOCTOR && appointment.getDoctorId().equals(principal.userId());
		if (!isOwningClinicAdmin && !isOwningDoctor) {
			throw new ForbiddenException("You do not have access to this appointment.");
		}
	}
}
