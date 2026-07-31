ALTER TABLE app_users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER' AFTER password_hash;

CREATE INDEX idx_app_users_role_created_at
    ON app_users (role, created_at);
