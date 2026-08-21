package com.appointmentscheduler.model;

/** FR-027: SCHEDULED is the only creation state; CANCELLED and COMPLETED are both terminal. */
public enum AppointmentState {
	SCHEDULED,
	CANCELLED,
	COMPLETED
}
