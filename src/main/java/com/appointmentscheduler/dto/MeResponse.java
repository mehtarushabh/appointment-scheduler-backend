package com.appointmentscheduler.dto;

/**
 * The current user's own display profile (feature 003 GET /me), deliberately kept separate from
 * AuthDtos.LoginResponse (research.md #1) so the auth response doesn't grow with profile data.
 */
public record MeResponse(String firstName, String lastName, String clinicName) {
}
