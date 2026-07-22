ALTER TABLE medical_institutions
    ADD COLUMN department_codes VARCHAR(255);

UPDATE medical_institutions institution
LEFT JOIN (
    SELECT institution_id,
           GROUP_CONCAT(
               department_code
               ORDER BY department_code
               SEPARATOR '|'
           ) AS department_codes
    FROM medical_institution_departments
    GROUP BY institution_id
) departments ON departments.institution_id = institution.id
SET institution.department_codes = departments.department_codes
WHERE institution.type = 'HOSPITAL';

ALTER TABLE medical_institution_departments
    DROP COLUMN department_name;
