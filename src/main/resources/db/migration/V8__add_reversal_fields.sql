ALTER TABLE transactions ADD COLUMN original_transaction_id BIGINT;

ALTER TABLE transactions ADD CONSTRAINT fk_transactions_original
    FOREIGN KEY (original_transaction_id) REFERENCES transactions(id);

CREATE INDEX idx_transactions_original_id ON transactions(original_transaction_id);
