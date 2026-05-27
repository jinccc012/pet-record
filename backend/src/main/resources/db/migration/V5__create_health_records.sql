CREATE TABLE health_records (
    id            BIGSERIAL PRIMARY KEY,
    pet_id        BIGINT       NOT NULL,
    visit_date    DATE         NOT NULL,
    hospital_name VARCHAR(255),
    doctor_name   VARCHAR(255),
    medical_note  VARCHAR(5000),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMP WITH TIME ZONE NULL,
    CONSTRAINT fk_health_records_pet FOREIGN KEY (pet_id) REFERENCES pets (id)
);

CREATE INDEX idx_health_records_pet_active
    ON health_records (pet_id) WHERE deleted_at IS NULL;

CREATE TABLE health_record_files (
    id                BIGSERIAL PRIMARY KEY,
    health_record_id  BIGINT NOT NULL,
    file_id           BIGINT NOT NULL,
    CONSTRAINT fk_hrf_health_record FOREIGN KEY (health_record_id)
        REFERENCES health_records (id) ON DELETE CASCADE,
    CONSTRAINT fk_hrf_file FOREIGN KEY (file_id) REFERENCES files (id)
);

CREATE INDEX idx_hrf_health_record ON health_record_files (health_record_id);
CREATE INDEX idx_hrf_file ON health_record_files (file_id);
-- No DB-level UNIQUE on (health_record_id, file_id): the service deduplicates
-- per-request via LinkedHashSet, and Hibernate's orphanRemoval insert-before-delete
-- ordering would otherwise collide on PUT-replace when keeping any same fileId.
