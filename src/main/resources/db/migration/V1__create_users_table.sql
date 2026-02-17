CREATE TABLE users (
    id              BIGSERIAL       PRIMARY KEY,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    phone_number    VARCHAR(20)     NOT NULL UNIQUE,
    first_name      VARCHAR(100)    NOT NULL,
    last_name       VARCHAR(100)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    transaction_pin VARCHAR(255),
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    kyc_level       VARCHAR(20)     NOT NULL DEFAULT 'TIER_1',
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone_number);
