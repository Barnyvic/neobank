CREATE TABLE wallets (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES users(id),
    wallet_number   VARCHAR(20)     NOT NULL UNIQUE,
    currency        VARCHAR(5)      NOT NULL DEFAULT 'NGN',
    balance         NUMERIC(19,4)   NOT NULL DEFAULT 0.0000,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_wallets_user_id ON wallets(user_id);
CREATE INDEX idx_wallets_wallet_number ON wallets(wallet_number);
