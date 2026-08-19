package com.appointmentscheduler.integration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.appointmentscheduler.model.Address;
import com.appointmentscheduler.model.Clinic;
import com.appointmentscheduler.model.Role;
import com.appointmentscheduler.model.User;
import com.appointmentscheduler.repository.ClinicRepository;
import com.appointmentscheduler.repository.UserRepository;

class SchemaConstraintsIT extends AbstractIntegrationTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ClinicRepository clinicRepository;

	private static Address sampleAddress() {
		return new Address("1 Main St", null, "Springfield", "IL", "62704", "USA");
	}

	@Test
	void rejectsDuplicateUserEmail() {
		userRepository.saveAndFlush(new User("Ada", "Admin", "dup@example.com", "hash", LocalDate.of(1990, 1, 1),
				sampleAddress(), Role.SYSTEM_ADMIN, null, null));

		assertThatThrownBy(() -> userRepository.saveAndFlush(new User("Bea", "Bee", "dup@example.com", "hash",
				LocalDate.of(1991, 2, 2), sampleAddress(), Role.SYSTEM_ADMIN, null, null)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsDuplicateClinicRegisteredId() {
		clinicRepository.saveAndFlush(new Clinic("Clinic A", sampleAddress(), "REG-001"));

		assertThatThrownBy(() -> clinicRepository.saveAndFlush(new Clinic("Clinic B", sampleAddress(), "REG-001")))
			.isInstanceOf(DataIntegrityViolationException.class);
	}
}
