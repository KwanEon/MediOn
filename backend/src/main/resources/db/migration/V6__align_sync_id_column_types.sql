ALTER TABLE medical_institutions
    MODIFY COLUMN last_seen_sync_id VARCHAR(36);

ALTER TABLE medical_institution_departments
    MODIFY COLUMN last_seen_sync_id VARCHAR(36) NOT NULL;
