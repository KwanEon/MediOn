CREATE TABLE medical_institutions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    external_id VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL,
    name VARCHAR(150) NOT NULL,
    phone_number VARCHAR(30),
    road_address VARCHAR(255),
    lot_address VARCHAR(255),
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_synced_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_medical_institutions_external_id UNIQUE (external_id)
);

CREATE INDEX idx_medical_institutions_type_active ON medical_institutions (type, active);
CREATE INDEX idx_medical_institutions_lat_lng ON medical_institutions (latitude, longitude);

CREATE TABLE operating_hours (
    id BIGINT NOT NULL AUTO_INCREMENT,
    institution_id BIGINT NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    open_time TIME NOT NULL,
    close_time TIME NOT NULL,
    closed BOOLEAN NOT NULL DEFAULT FALSE,
    lunch_start_time TIME,
    lunch_end_time TIME,
    PRIMARY KEY (id),
    CONSTRAINT fk_operating_hours_institution
        FOREIGN KEY (institution_id) REFERENCES medical_institutions (id)
);

CREATE INDEX idx_operating_hours_institution_day ON operating_hours (institution_id, day_of_week);
CREATE INDEX idx_operating_hours_day_closed_time ON operating_hours (day_of_week, closed, open_time, close_time);

CREATE TABLE data_sync_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source_name VARCHAR(100) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    synced_at DATETIME NOT NULL,
    message VARCHAR(1000),
    PRIMARY KEY (id)
);

