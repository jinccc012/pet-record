CREATE TABLE pets (
    id         BIGSERIAL PRIMARY KEY,
    owner_id   BIGINT       NOT NULL,
    name       VARCHAR(100) NOT NULL,
    species    VARCHAR(30)  NOT NULL,
    breed      VARCHAR(100),
    gender     VARCHAR(20),
    birth_date DATE,
    color      VARCHAR(50),
    notes      TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    CONSTRAINT fk_pets_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);

CREATE INDEX idx_pets_owner_active ON pets (owner_id) WHERE deleted_at IS NULL;
