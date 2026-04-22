ALTER TABLE wallets ALTER COLUMN key_share_dek SET NOT NULL;
ALTER TABLE wallets ALTER COLUMN key_share_iv SET NOT NULL;
ALTER TABLE wallets ALTER COLUMN peer_key_share_iv SET NOT NULL;
