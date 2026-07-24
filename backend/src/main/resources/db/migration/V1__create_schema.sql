CREATE TABLE medical_institutions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    hpid VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL,
    name VARCHAR(150) NOT NULL,
    phone_number VARCHAR(30),
    institution_kind_name VARCHAR(50),
    emergency_room_available BOOLEAN NOT NULL DEFAULT FALSE,
    road_address VARCHAR(255),
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    night_service BOOLEAN NOT NULL DEFAULT FALSE,
    twenty_four_hours BOOLEAN NOT NULL DEFAULT FALSE,
    saturday_service BOOLEAN NOT NULL DEFAULT FALSE,
    sunday_service BOOLEAN NOT NULL DEFAULT FALSE,
    holiday_service BOOLEAN NOT NULL DEFAULT FALSE,
    last_seen_sync_id VARCHAR(36),
    inactive_at DATETIME,
    last_synced_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_medical_institutions_hpid UNIQUE (hpid)
);

CREATE INDEX idx_medical_institutions_type_active
    ON medical_institutions (type, active);

CREATE INDEX idx_medical_institutions_lat_lng
    ON medical_institutions (latitude, longitude);

CREATE INDEX idx_medical_institutions_sync_run
    ON medical_institutions (type, last_seen_sync_id);

CREATE TABLE operating_hours (
    id BIGINT NOT NULL AUTO_INCREMENT,
    institution_id BIGINT NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    open_time TIME NOT NULL,
    close_time TIME NOT NULL,
    closed BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_operating_hours_institution_day
        UNIQUE (institution_id, day_of_week),
    CONSTRAINT fk_operating_hours_institution
        FOREIGN KEY (institution_id) REFERENCES medical_institutions (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_operating_hours_day_closed_time
    ON operating_hours (day_of_week, closed, open_time, close_time);

CREATE TABLE medical_institution_departments (
    institution_id BIGINT NOT NULL,
    department_code VARCHAR(10) NOT NULL,
    last_seen_sync_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (institution_id, department_code),
    CONSTRAINT fk_medical_institution_departments_institution
        FOREIGN KEY (institution_id) REFERENCES medical_institutions (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_medical_institution_departments_code
    ON medical_institution_departments (department_code, institution_id);

CREATE TABLE data_sync_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source_name VARCHAR(100) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    synced_at DATETIME NOT NULL,
    message VARCHAR(1000),
    PRIMARY KEY (id)
);

CREATE INDEX idx_data_sync_histories_lookup
    ON data_sync_histories (source_name, target_type, status, synced_at);

CREATE TABLE app_users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(30) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(150) NOT NULL,
    phone_number VARCHAR(30) NOT NULL,
    address VARCHAR(255) NOT NULL,
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_app_users_username UNIQUE (username),
    CONSTRAINT uk_app_users_email UNIQUE (email)
);

CREATE TABLE user_favorites (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    institution_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_favorites_user_institution
        UNIQUE (user_id, institution_id),
    CONSTRAINT fk_user_favorites_user
        FOREIGN KEY (user_id) REFERENCES app_users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_favorites_institution
        FOREIGN KEY (institution_id) REFERENCES medical_institutions (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_user_favorites_user_created_at
    ON user_favorites (user_id, created_at);
