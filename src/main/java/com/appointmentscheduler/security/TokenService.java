package com.appointmentscheduler.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.appointmentscheduler.model.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** Issues and validates the bearer tokens used for stateless authentication (research.md #2). */
@Service
public class TokenService {

	private final SecretKey signingKey;
	private final Duration tokenTtl;

	public TokenService(@Value("${appointment-scheduler.security.token-secret}") String tokenSecret,
			@Value("${appointment-scheduler.security.token-expiry-minutes}") long tokenExpiryMinutes) {
		this.signingKey = Keys.hmacShaKeyFor(pad(tokenSecret).getBytes(StandardCharsets.UTF_8));
		this.tokenTtl = Duration.ofMinutes(tokenExpiryMinutes);
	}

	private static String pad(String secret) {
		StringBuilder padded = new StringBuilder(secret);
		while (padded.length() < 32) {
			padded.append(secret);
		}
		return padded.toString();
	}

	public String issueToken(UUID userId, Role role, UUID clinicId) {
		Instant now = Instant.now();
		var builder = Jwts.builder()
			.subject(userId.toString())
			.claim("role", role.name())
			.issuedAt(Date.from(now))
			.expiration(Date.from(now.plus(tokenTtl)));
		if (clinicId != null) {
			builder.claim("clinicId", clinicId.toString());
		}
		return builder.signWith(signingKey).compact();
	}

	public AuthenticatedPrincipal validate(String token) {
		try {
			Claims claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
			UUID userId = UUID.fromString(claims.getSubject());
			Role role = Role.valueOf(claims.get("role", String.class));
			String clinicIdClaim = claims.get("clinicId", String.class);
			UUID clinicId = clinicIdClaim != null ? UUID.fromString(clinicIdClaim) : null;
			return new AuthenticatedPrincipal(userId, role, clinicId);
		} catch (JwtException | IllegalArgumentException e) {
			return null;
		}
	}
}
