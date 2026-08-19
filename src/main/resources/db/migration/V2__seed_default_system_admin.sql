-- Seeds the one System Admin account required to log in and onboard the first clinic (FR-002).
-- System Admin accounts are never created by the application itself — this migration IS the
-- "direct database operation" the spec calls for.
--
-- Default local dev/test login:
--   email:    admin@appointmentscheduler.local
--   password: ChangeMe123!
-- Change this password after first login (PATCH /api/v1/me/password) for any non-local environment.
INSERT INTO users (
    id, first_name, last_name, email, password_hash, date_of_birth,
    address_line1, address_line2, city, state, zip, country,
    role, clinic_id, specialty, created_at
) VALUES (
    gen_random_uuid(), 'System', 'Admin', 'admin@appointmentscheduler.local',
    '$2a$10$N2KjxkOEvXm5clVJkUIcYO0lV77yXjVBcDEzX2vvqZD1Y.eBu83xi',
    '1990-01-01',
    '1 Admin Way', NULL, 'Capital City', 'IL', '60000', 'USA',
    'SYSTEM_ADMIN', NULL, NULL, now()
);
