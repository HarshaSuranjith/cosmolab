-- Ward overview read-model: one row per patient with their latest vital signs
-- and a count of active problems. Used by WardOverviewService to avoid N+1 queries.
--
-- The derived table ranks vital sign rows per EHR by recorded_at DESC using
-- ROW_NUMBER() so the outer join can pick rn = 1 (most recent only).
-- No WHERE clause here — ward and status filtering is done by the query layer.

CREATE VIEW vw_ward_patient_summary AS
SELECT
    p.id             AS patient_id,
    p.first_name,
    p.last_name,
    p.personal_number,
    p.ward,
    p.status         AS patient_status,
    e.ehr_id,
    v.recorded_at    AS vitals_recorded_at,
    v.systolic_bp,
    v.diastolic_bp,
    v.heart_rate,
    v.respiratory_rate,
    v.temperature,
    v.oxygen_saturation,
    v.weight,
    (SELECT COUNT(*)
     FROM   problem_list_entries pl
     WHERE  pl.ehr_id = e.ehr_id
       AND  pl.status = 'ACTIVE') AS active_problem_count
FROM patients p
JOIN ehr_records e ON e.subject_id = p.id
LEFT JOIN (
    SELECT
        v2.id,
        c.ehr_id,
        v2.recorded_at,
        v2.systolic_bp,
        v2.diastolic_bp,
        v2.heart_rate,
        v2.respiratory_rate,
        v2.temperature,
        v2.oxygen_saturation,
        v2.weight,
        ROW_NUMBER() OVER (PARTITION BY c.ehr_id ORDER BY v2.recorded_at DESC) AS rn
    FROM vital_signs v2
    JOIN compositions c ON v2.composition_id = c.id
) v ON v.ehr_id = e.ehr_id AND v.rn = 1;
