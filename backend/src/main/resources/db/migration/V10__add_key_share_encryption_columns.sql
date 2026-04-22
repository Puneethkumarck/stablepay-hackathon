ALTER TABLE wallets ADD COLUMN key_share_dek BYTEA;
ALTER TABLE wallets ADD COLUMN key_share_iv BYTEA;
ALTER TABLE wallets ADD COLUMN peer_key_share_iv BYTEA;
