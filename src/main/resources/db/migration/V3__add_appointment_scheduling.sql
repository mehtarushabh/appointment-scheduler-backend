CREATE TABLE clinic_working_hours (
    id              UUID PRIMARY KEY,
    clinic_id       UUID NOT NULL REFERENCES clinics (id),
    day_of_week     VARCHAR(20) NOT NULL,
    is_open         BOOLEAN NOT NULL,
    start_time      TIME,
    end_time        TIME,
    CONSTRAINT uk_clinic_working_hours_clinic_day UNIQUE (clinic_id, day_of_week)
);

CREATE INDEX ix_clinic_working_hours_clinic_id ON clinic_working_hours (clinic_id);

CREATE TABLE doctor_leaves (
    id              UUID PRIMARY KEY,
    doctor_id       UUID NOT NULL REFERENCES users (id),
    date            DATE NOT NULL,
    full_day        BOOLEAN NOT NULL,
    start_time      TIME,
    end_time        TIME,
    created_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_doctor_leaves_doctor_id ON doctor_leaves (doctor_id);

CREATE TABLE appointments (
    id                  UUID PRIMARY KEY,
    patient_id          UUID NOT NULL REFERENCES users (id),
    doctor_id           UUID NOT NULL REFERENCES users (id),
    clinic_id           UUID NOT NULL REFERENCES clinics (id),
    date                DATE NOT NULL,
    start_time          TIME NOT NULL,
    duration_minutes    SMALLINT NOT NULL,
    state               VARCHAR(20) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_appointments_patient_id ON appointments (patient_id);
CREATE INDEX ix_appointments_doctor_id ON appointments (doctor_id);
CREATE INDEX ix_appointments_clinic_id ON appointments (clinic_id);
CREATE INDEX ix_appointments_doctor_date ON appointments (doctor_id, date);

CREATE TABLE appointment_slots (
    id                  UUID PRIMARY KEY,
    appointment_id      UUID NOT NULL REFERENCES appointments (id),
    doctor_id           UUID NOT NULL,
    date                DATE NOT NULL,
    slot_start_time     TIME NOT NULL,
    CONSTRAINT uk_appointment_slots_doctor_date_time UNIQUE (doctor_id, date, slot_start_time)
);

CREATE INDEX ix_appointment_slots_appointment_id ON appointment_slots (appointment_id);
