-- Bulk local dev/test-data fixture — NOT business-meaningful seed data like V2's System Admin
-- account, purely a large, realistic dataset for manually exercising list/pagination-heavy screens
-- (Doctors, Patients, Appointments, Schedule appointment) end-to-end. Generates:
--   - 5 clinics, each with its own Clinic Admin and Mon-Fri 08:00-17:00 working hours
--     (Sat/Sun closed), matching the app's existing default working-hours pattern (see V4)
--   - 7 doctors per clinic (35 total), across a rotating set of specialties
--   - 22 patients per clinic (110 total), each linked to their clinic via
--     clinic_patient_associations (patients are NOT linked via users.clinic_id — see
--     PatientService.listPatientsForClinic, which joins through that table)
--   - SCHEDULED appointments across the coming ~4 months (weekdays only): a random 0-4 per
--     doctor per day, each a distinct 30-minute slot between 08:00 and 16:30. Every appointment is
--     exactly 30 minutes so each one occupies exactly one row in appointment_slots — this sidesteps
--     the collision bookkeeping a mix of 30/60-minute appointments would need to stay within the
--     UNIQUE(doctor_id, date, slot_start_time) constraint, which is a deliberate simplification for
--     bulk fixture data, not a business rule.
--
-- Every generated user (clinic admins, doctors, patients) shares the same bcrypt hash as the
-- seeded System Admin from V2 — password: ChangeMe123!
DO $$
DECLARE
    clinic_names  TEXT[] := ARRAY['Lakeside Family Medicine', 'Summit Ridge Health', 'Cedar Grove Clinic', 'Harborview Medical Group', 'Willowbrook Wellness Center'];
    clinic_cities TEXT[] := ARRAY['Portland', 'Denver', 'Austin', 'Seattle', 'Raleigh'];
    clinic_states TEXT[] := ARRAY['OR', 'CO', 'TX', 'WA', 'NC'];
    specialties   TEXT[] := ARRAY['General Physician', 'Cardiology', 'Dermatology', 'Pediatrics', 'Orthopedics', 'ENT', 'Neurology', 'Psychiatry'];
    first_names   TEXT[] := ARRAY['James','Mary','Robert','Patricia','John','Jennifer','Michael','Linda','David','Elizabeth','William','Susan','Richard','Jessica','Joseph','Sarah','Thomas','Karen','Charles','Nancy','Christopher','Lisa','Daniel','Betty','Matthew','Margaret','Anthony','Sandra','Mark','Ashley','Paul','Kimberly','Steven','Emily','Andrew','Donna','Joshua','Michelle','Kenneth','Carol'];
    last_names    TEXT[] := ARRAY['Smith','Johnson','Williams','Brown','Jones','Garcia','Miller','Davis','Rodriguez','Martinez','Hernandez','Lopez','Gonzalez','Wilson','Anderson','Thomas','Taylor','Moore','Jackson','Martin','Lee','Perez','Thompson','White','Harris','Sanchez','Clark','Ramirez','Lewis','Robinson'];
    known_hash    TEXT := '$2a$10$N2KjxkOEvXm5clVJkUIcYO0lV77yXjVBcDEzX2vvqZD1Y.eBu83xi';

    v_clinic_id   UUID;
    v_clinic_slug TEXT;
    v_admin_id    UUID;
    v_doctor_id   UUID;
    v_doctor_ids  UUID[];
    v_patient_id  UUID;
    v_patient_ids UUID[];
    v_appt_id     UUID;
    v_appt_date   DATE;
    v_slot_time   TIME;
    v_appt_count  INT;
    v_patient_pick UUID;
    i INT;
    j INT;
    d INT;
BEGIN
    FOR i IN 1..5 LOOP
        v_clinic_id := gen_random_uuid();
        v_clinic_slug := lower(regexp_replace(clinic_names[i], '[^A-Za-z]', '', 'g'));

        INSERT INTO clinics (id, name, address_line1, address_line2, city, state, zip, country, registered_id, created_at)
        VALUES (v_clinic_id, clinic_names[i], (100 + i)::text || ' Main St', NULL, clinic_cities[i], clinic_states[i],
                (10000 + i * 111)::text, 'USA', 'TESTCLINIC-' || i, now());

        INSERT INTO clinic_working_hours (id, clinic_id, day_of_week, is_open, start_time, end_time)
        SELECT gen_random_uuid(), v_clinic_id, wh.day_of_week, wh.is_open, wh.start_time, wh.end_time
        FROM (VALUES
            ('MONDAY', TRUE, TIME '08:00', TIME '17:00'),
            ('TUESDAY', TRUE, TIME '08:00', TIME '17:00'),
            ('WEDNESDAY', TRUE, TIME '08:00', TIME '17:00'),
            ('THURSDAY', TRUE, TIME '08:00', TIME '17:00'),
            ('FRIDAY', TRUE, TIME '08:00', TIME '17:00'),
            ('SATURDAY', FALSE, NULL, NULL),
            ('SUNDAY', FALSE, NULL, NULL)
        ) AS wh(day_of_week, is_open, start_time, end_time);

        -- Clinic Admin
        v_admin_id := gen_random_uuid();
        INSERT INTO users (id, first_name, last_name, email, password_hash, date_of_birth,
                            address_line1, address_line2, city, state, zip, country, role, clinic_id, specialty, created_at)
        VALUES (v_admin_id,
                first_names[1 + (i % array_length(first_names, 1))],
                last_names[1 + (i % array_length(last_names, 1))],
                'admin' || i || '.' || v_clinic_slug || '@testclinic.local', known_hash, DATE '1980-01-01',
                (200 + i)::text || ' Admin Ave', NULL, clinic_cities[i], clinic_states[i], (10000 + i * 222)::text, 'USA',
                'CLINIC_ADMIN', v_clinic_id, NULL, now());

        -- Doctors
        v_doctor_ids := ARRAY[]::UUID[];
        FOR j IN 1..7 LOOP
            v_doctor_id := gen_random_uuid();
            v_doctor_ids := array_append(v_doctor_ids, v_doctor_id);
            INSERT INTO users (id, first_name, last_name, email, password_hash, date_of_birth,
                                address_line1, address_line2, city, state, zip, country, role, clinic_id, specialty, created_at)
            VALUES (v_doctor_id,
                    first_names[1 + ((i * 7 + j) % array_length(first_names, 1))],
                    last_names[1 + ((i * 13 + j) % array_length(last_names, 1))],
                    'doctor' || j || '.' || v_clinic_slug || '@testclinic.local', known_hash,
                    (DATE '1970-01-01' + ((i * 100 + j * 37) % 9000)),
                    (300 + j)::text || ' Clinician Ct', NULL, clinic_cities[i], clinic_states[i],
                    (10000 + i * 333 + j)::text, 'USA',
                    'DOCTOR', v_clinic_id, specialties[1 + ((i + j) % array_length(specialties, 1))], now());
        END LOOP;

        -- Patients
        v_patient_ids := ARRAY[]::UUID[];
        FOR j IN 1..22 LOOP
            v_patient_id := gen_random_uuid();
            v_patient_ids := array_append(v_patient_ids, v_patient_id);
            INSERT INTO users (id, first_name, last_name, email, password_hash, date_of_birth,
                                address_line1, address_line2, city, state, zip, country, role, clinic_id, specialty, created_at)
            VALUES (v_patient_id,
                    first_names[1 + ((i * 17 + j) % array_length(first_names, 1))],
                    last_names[1 + ((i * 23 + j) % array_length(last_names, 1))],
                    'patient' || j || '.' || v_clinic_slug || '@testclinic.local', known_hash,
                    (DATE '1950-01-01' + ((i * 211 + j * 53) % 25000)),
                    (400 + j)::text || ' Patient Pl', NULL, clinic_cities[i], clinic_states[i],
                    (10000 + i * 444 + j)::text, 'USA',
                    'PATIENT', NULL, NULL, now());

            INSERT INTO clinic_patient_associations (id, clinic_id, patient_id, created_at)
            VALUES (gen_random_uuid(), v_clinic_id, v_patient_id, now());
        END LOOP;

        -- Appointments: weekdays over the next 120 days (~4 months), 0-4 per doctor per day, each a
        -- distinct 30-minute slot (08:00-16:30) so no two appointments for the same doctor collide.
        FOR d IN 0..119 LOOP
            v_appt_date := CURRENT_DATE + d;
            IF EXTRACT(ISODOW FROM v_appt_date) IN (6, 7) THEN
                CONTINUE;
            END IF;

            FOREACH v_doctor_id IN ARRAY v_doctor_ids LOOP
                v_appt_count := floor(random() * 5)::int; -- 0..4 appointments this doctor, this day

                FOR v_slot_time IN
                    SELECT slot FROM (
                        SELECT (TIME '08:00' + (n * interval '30 min'))::time AS slot
                        FROM generate_series(0, 17) AS n
                        ORDER BY random()
                        LIMIT v_appt_count
                    ) sub
                LOOP
                    v_patient_pick := v_patient_ids[1 + floor(random() * array_length(v_patient_ids, 1))::int];
                    v_appt_id := gen_random_uuid();

                    INSERT INTO appointments (id, patient_id, doctor_id, clinic_id, date, start_time,
                                               duration_minutes, state, created_at)
                    VALUES (v_appt_id, v_patient_pick, v_doctor_id, v_clinic_id, v_appt_date, v_slot_time,
                            30, 'SCHEDULED', now());

                    INSERT INTO appointment_slots (id, appointment_id, doctor_id, date, slot_start_time)
                    VALUES (gen_random_uuid(), v_appt_id, v_doctor_id, v_appt_date, v_slot_time);
                END LOOP;
            END LOOP;
        END LOOP;
    END LOOP;
END $$;
