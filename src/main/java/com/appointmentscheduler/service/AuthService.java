package com.appointmentscheduler.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.appointmentscheduler.common.BadRequestException;
import com.appointmentscheduler.common.UnauthorizedException;
import com.appointmentscheduler.dto.AuthDtos.LoginResponse;
import com.appointmentscheduler.dto.MeResponse;
import com.appointmentscheduler.model.Clinic;
import com.appointmentscheduler.model.User;
import com.appointmentscheduler.repository.ClinicRepository;
import com.appointmentscheduler.repository.UserRepository;
import com.appointmentscheduler.security.TokenService;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokenService;
	private final ClinicRepository clinicRepository;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService,
			ClinicRepository clinicRepository) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
		this.clinicRepository = clinicRepository;
	}

	public LoginResponse login(String email, String password) {
		User user = userRepository.findByEmail(email)
			.filter(u -> passwordEncoder.matches(password, u.getPasswordHash()))
			.orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

		String token = tokenService.issueToken(user.getId(), user.getRole(), user.getClinicId());
		return new LoginResponse(token, user.getRole(), user.getClinicId());
	}

	/** FR-021: available any time, never a forced step at login. */
	public void changePassword(User user, String currentPassword, String newPassword) {
		if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
			throw new BadRequestException("Current password is incorrect.");
		}
		user.setPasswordHash(passwordEncoder.encode(newPassword));
		userRepository.save(user);
	}

	/** GET /me (feature 003): the caller's own display profile, kept separate from login (research.md #1). */
	public MeResponse getProfile(User user) {
		String clinicName = user.getClinicId() == null
				? null
				: clinicRepository.findById(user.getClinicId()).map(Clinic::getName).orElse(null);
		return new MeResponse(user.getFirstName(), user.getLastName(), clinicName);
	}
}
