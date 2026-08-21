-- Clinics onboarded before the working-hours feature shipped (V3) never got the default 7-day
-- row-set that onboardClinic() now seeds for new clinics, leaving replaceWorkingHours() unable to
-- find a row to update for any of their days. Backfill the same FR-002 default (Mon-Fri
-- 08:00-17:00, Sat-Sun closed) for any clinic that currently has no working-hours rows at all.
INSERT INTO clinic_working_hours (id, clinic_id, day_of_week, is_open, start_time, end_time)
SELECT gen_random_uuid(), c.id, d.day_of_week, d.is_open, d.start_time, d.end_time
FROM clinics c
CROSS JOIN (
    VALUES
        ('MONDAY', TRUE, TIME '08:00', TIME '17:00'),
        ('TUESDAY', TRUE, TIME '08:00', TIME '17:00'),
        ('WEDNESDAY', TRUE, TIME '08:00', TIME '17:00'),
        ('THURSDAY', TRUE, TIME '08:00', TIME '17:00'),
        ('FRIDAY', TRUE, TIME '08:00', TIME '17:00'),
        ('SATURDAY', FALSE, NULL, NULL),
        ('SUNDAY', FALSE, NULL, NULL)
) AS d(day_of_week, is_open, start_time, end_time)
WHERE NOT EXISTS (
    SELECT 1 FROM clinic_working_hours w WHERE w.clinic_id = c.id
);
