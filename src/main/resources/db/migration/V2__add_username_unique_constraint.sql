ALTER TABLE users ADD CONSTRAINT uk_users_username UNIQUE (username);

ALTER TABLE follows ADD COLUMN updated_at datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE follows ADD COLUMN deleted_at datetime(6) NULL;
