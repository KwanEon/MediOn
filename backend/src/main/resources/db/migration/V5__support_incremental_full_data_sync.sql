ALTER TABLE medical_institutions
    DROP INDEX uk_medical_institutions_external_id;

ALTER TABLE medical_institutions
    CHANGE COLUMN external_id hpid VARCHAR(100) NOT NULL;

ALTER TABLE medical_institutions
    ADD COLUMN institution_kind_code VARCHAR(10),
    ADD COLUMN institution_kind_name VARCHAR(50),
    ADD COLUMN emergency_class_code VARCHAR(10),
    ADD COLUMN emergency_class_name VARCHAR(100),
    ADD COLUMN emergency_room_available BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN emergency_phone VARCHAR(30),
    ADD COLUMN postal_code VARCHAR(10),
    ADD COLUMN note VARCHAR(1000),
    ADD COLUMN map_description VARCHAR(255),
    ADD COLUMN description VARCHAR(1000),
    ADD COLUMN night_service BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN twenty_four_hours BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN saturday_service BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN sunday_service BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN holiday_service BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN last_seen_sync_id CHAR(36),
    ADD COLUMN inactive_at DATETIME,
    ADD CONSTRAINT uk_medical_institutions_hpid UNIQUE (hpid);

ALTER TABLE operating_hours
    ADD CONSTRAINT uk_operating_hours_institution_day UNIQUE (institution_id, day_of_week);

CREATE TABLE medical_institution_departments (
    institution_id BIGINT NOT NULL,
    department_code VARCHAR(10) NOT NULL,
    department_name VARCHAR(100) NOT NULL,
    last_seen_sync_id CHAR(36) NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (institution_id, department_code),
    CONSTRAINT fk_medical_institution_departments_institution
        FOREIGN KEY (institution_id) REFERENCES medical_institutions (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_medical_institution_departments_code
    ON medical_institution_departments (department_code, institution_id);

CREATE INDEX idx_medical_institutions_sync_run
    ON medical_institutions (type, last_seen_sync_id);

UPDATE medical_institutions
SET active = FALSE,
    inactive_at = NOW()
WHERE hpid LIKE 'sample-%';
