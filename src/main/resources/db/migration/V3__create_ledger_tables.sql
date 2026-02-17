CREATE TABLE ledger_accounts (
    id              BIGSERIAL       PRIMARY KEY,
    account_name    VARCHAR(255)    NOT NULL,
    account_type    VARCHAR(20)     NOT NULL,
    wallet_id       BIGINT          REFERENCES wallets(id),
    balance         NUMERIC(19,4)   NOT NULL DEFAULT 0.0000,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ledger_accounts_wallet_id ON ledger_accounts(wallet_id);
CREATE INDEX idx_ledger_accounts_type ON ledger_accounts(account_type);

CREATE TABLE journal_entries (
    id              BIGSERIAL       PRIMARY KEY,
    reference       VARCHAR(100)    NOT NULL UNIQUE,
    description     VARCHAR(500)    NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_journal_entries_reference ON journal_entries(reference);

CREATE TABLE ledger_entries (
    id                  BIGSERIAL       PRIMARY KEY,
    journal_entry_id    BIGINT          NOT NULL REFERENCES journal_entries(id),
    ledger_account_id   BIGINT          NOT NULL REFERENCES ledger_accounts(id),
    entry_type          VARCHAR(10)     NOT NULL,
    amount              NUMERIC(19,4)   NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ledger_entries_journal ON ledger_entries(journal_entry_id);
CREATE INDEX idx_ledger_entries_account ON ledger_entries(ledger_account_id);
