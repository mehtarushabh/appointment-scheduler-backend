package com.appointmentscheduler.security;

import java.util.UUID;

import com.appointmentscheduler.model.Role;

/** The identity carried by a validated bearer token for the current request. */
public record AuthenticatedPrincipal(UUID userId, Role role, UUID clinicId) {
}
