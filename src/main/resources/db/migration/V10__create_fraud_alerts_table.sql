CREATE TABLE fraud_alerts (
    id                      BIGSERIAL       PRIMARY KEY,
    user_id                 BIGINT          NOT NULL REFERENCES users(id),
    rule_type               VARCHAR(50)     NOT NULL,
    severity                VARCHAR(20)     NOT NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'OPEN',
    operation_type          VARCHAR(20)     NOT NULL,
    amount                  NUMERIC(19,4),
    currency                VARCHAR(5),
    recipient_wallet_number VARCHAR(50),
    wallet_id               BIGINT          REFERENCES wallets(id),
    transaction_reference   VARCHAR(100),
    message                 VARCHAR(500)    NOT NULL,
    metadata                JSONB,
    resolved_at             TIMESTAMP,
    resolved_by_user_id     BIGINT          REFERENCES users(id),
    resolution_note         VARCHAR(500),
    created_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fraud_alerts_user ON fraud_alerts(user_id);
CREATE INDEX idx_fraud_alerts_status ON fraud_alerts(status);
CREATE INDEX idx_fraud_alerts_created_at ON fraud_alerts(created_at);
