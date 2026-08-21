package com.appointmentscheduler.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.appointmentscheduler.model.AppointmentSlot;

public interface AppointmentSlotRepository extends JpaRepository<AppointmentSlot, UUID> {
}
