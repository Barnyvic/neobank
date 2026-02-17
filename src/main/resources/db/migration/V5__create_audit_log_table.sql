CREATE TABLE audit_logs (
    id              BIGSERIAL       PRIMARY KEY,
    entity_type     VARCHAR(100)    NOT NULL,
    entity_id       BIGINT          NOT NULL,
    action          VARCHAR(50)     NOT NULL,
    performed_by    BIGINT,
    old_value       JSONB,
    new_value       JSONB,
    ip_address      VARCHAR(45),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_performed_by ON audit_logs(performed_by);

CREATE TABLE refresh_tokens (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES users(id),
    token           VARCHAR(500)    NOT NULL UNIQUE,
    expiry_date     TIMESTAMP       NOT NULL,
    revoked         BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
