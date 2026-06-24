-- Monotonic counter embedded in issued JWTs as the "ver" claim. Bumping it (logout, password
-- change, account disable) invalidates every token previously issued to the user.
ALTER TABLE users ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;
