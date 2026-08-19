CREATE TABLE clinics (
    id              UUID PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    address_line1   VARCHAR(255) NOT NULL,
    address_line2   VARCHAR(255),
    city            VARCHAR(255) NOT NULL,
    state           VARCHAR(255) NOT NULL,
    zip             VARCHAR(20)  NOT NULL,
    country         VARCHAR(255) NOT NULL,
    registered_id   VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_clinics_registered_id UNIQUE (registered_id)
);

CREATE TABLE users (
    id              UUID PRIMARY KEY,
    first_name      VARCHAR(255) NOT NULL,
    last_name       VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    date_of_birth   DATE NOT NULL,
    address_line1   VARCHAR(255) NOT NULL,
    address_line2   VARCHAR(255),
    city            VARCHAR(255) NOT NULL,
    state           VARCHAR(255) NOT NULL,
    zip             VARCHAR(20)  NOT NULL,
    country         VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    clinic_id       UUID REFERENCES clinics (id),
    specialty       VARCHAR(255),
    created_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE INDEX ix_users_clinic_id ON users (clinic_id);

CREATE TABLE clinic_patient_associations (
    id              UUID PRIMARY KEY,
    clinic_id       UUID NOT NULL REFERENCES clinics (id),
    patient_id      UUID NOT NULL REFERENCES users (id),
    created_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_clinic_patient UNIQUE (clinic_id, patient_id)
);

CREATE INDEX ix_clinic_patient_patient_id ON clinic_patient_associations (patient_id);
