CREATE TABLE daily_records (
    id          BIGSERIAL PRIMARY KEY,
    pet_id      BIGINT        NOT NULL,
    record_date DATE          NOT NULL,
    weight_kg   NUMERIC(5, 2),
    water_ml    INTEGER,
    daily_note  VARCHAR(1000),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP WITH TIME ZONE NULL,
    CONSTRAINT fk_daily_records_pet FOREIGN KEY (pet_id) REFERENCES pets (id)
);

-- One active record per pet per date; soft-deleted rows don't block re-creation.
CREATE UNIQUE INDEX uk_daily_records_pet_date_active
    ON daily_records (pet_id, record_date) WHERE deleted_at IS NULL;

CREATE TABLE feeding_records (
    id              BIGSERIAL PRIMARY KEY,
    daily_record_id BIGINT      NOT NULL,
    feeding_time    TIME        NOT NULL,
    food_gram       INTEGER,
    condition_text  VARCHAR(500),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_feeding_records_daily FOREIGN KEY (daily_record_id)
        REFERENCES daily_records (id) ON DELETE CASCADE
);

CREATE INDEX idx_feeding_records_daily ON feeding_records (daily_record_id);

CREATE TABLE stool_records (
    id              BIGSERIAL PRIMARY KEY,
    daily_record_id BIGINT      NOT NULL,
    stool_time      TIME        NOT NULL,
    condition_text  VARCHAR(500),
    abnormal        BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_stool_records_daily FOREIGN KEY (daily_record_id)
        REFERENCES daily_records (id) ON DELETE CASCADE
);

CREATE INDEX idx_stool_records_daily ON stool_records (daily_record_id);
