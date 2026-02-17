CREATE TABLE transactions (
    id                  BIGSERIAL       PRIMARY KEY,
    reference           VARCHAR(100)    NOT NULL UNIQUE,
    transaction_type    VARCHAR(20)     NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    amount              NUMERIC(19,4)   NOT NULL,
    currency            VARCHAR(5)      NOT NULL DEFAULT 'NGN',
    description         VARCHAR(500),
    source_wallet_id    BIGINT          REFERENCES wallets(id),
    dest_wallet_id      BIGINT          REFERENCES wallets(id),
    journal_entry_id    BIGINT          REFERENCES journal_entries(id),
    idempotency_key     VARCHAR(100)    UNIQUE,
    metadata            JSONB,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_reference ON transactions(reference);
CREATE INDEX idx_transactions_source ON transactions(source_wallet_id);
CREATE INDEX idx_transactions_dest ON transactions(dest_wallet_id);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_idempotency ON transactions(idempotency_key);
