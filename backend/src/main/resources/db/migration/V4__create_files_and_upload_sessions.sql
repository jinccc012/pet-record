CREATE TABLE files (
    id                BIGSERIAL PRIMARY KEY,
    upload_session_id BIGINT,
    uploaded_by       BIGINT       NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename   VARCHAR(255) NOT NULL,
    storage_provider  VARCHAR(50)  NOT NULL,
    bucket_name       VARCHAR(255) NOT NULL,
    object_key        TEXT         NOT NULL,
    content_type      VARCHAR(100) NOT NULL,
    file_size         BIGINT       NOT NULL,
    file_category     VARCHAR(50)  NOT NULL,
    status            VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at        TIMESTAMP WITH TIME ZONE NULL,
    CONSTRAINT fk_files_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users (id)
);

CREATE INDEX idx_files_uploaded_by ON files (uploaded_by);
CREATE INDEX idx_files_status ON files (status);

CREATE TABLE upload_sessions (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    pet_id            BIGINT,
    related_id        BIGINT,
    file_category     VARCHAR(50)  NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename   VARCHAR(255) NOT NULL,
    bucket_name       VARCHAR(255) NOT NULL,
    object_key        TEXT         NOT NULL,
    content_type      VARCHAR(100) NOT NULL,
    file_size         BIGINT       NOT NULL,
    status            VARCHAR(30)  NOT NULL,
    expires_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at      TIMESTAMP WITH TIME ZONE,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_upload_sessions_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_upload_sessions_user ON upload_sessions (user_id);
CREATE INDEX idx_upload_sessions_status ON upload_sessions (status);

ALTER TABLE pets ADD COLUMN avatar_file_id BIGINT;
ALTER TABLE pets ADD CONSTRAINT fk_pets_avatar FOREIGN KEY (avatar_file_id) REFERENCES files (id);
