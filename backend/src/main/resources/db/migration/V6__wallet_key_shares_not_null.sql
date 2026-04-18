-- STA-83: lock the "wallet has both key shares or does not exist" invariant.
--
-- Before this migration, DKG could silently persist a wallet with a NULL
-- peer_key_share_data (peer sidecar timed out; primary still returned OK).
-- Such wallets cannot sign any outbound transaction in 2-of-2 setups.
-- Application-level fix (MpcWalletGrpcClient.generateKey throws when the
-- peer share is missing) prevents new NULLs; this migration makes the
-- database reject them too.
--
-- Existing NULL rows are orphaned broken wallets that can never complete
-- a fund -> remittance -> claim flow. Their funding_orders reference them
-- via FK and are already unusable, so we drop them in the same transaction.

DELETE FROM funding_orders
WHERE wallet_id IN (
    SELECT id FROM wallets
    WHERE peer_key_share_data IS NULL
       OR key_share_data IS NULL
       OR public_key IS NULL
);

DELETE FROM wallets
WHERE peer_key_share_data IS NULL
   OR key_share_data IS NULL
   OR public_key IS NULL;

ALTER TABLE wallets ALTER COLUMN public_key          SET NOT NULL;
ALTER TABLE wallets ALTER COLUMN key_share_data      SET NOT NULL;
ALTER TABLE wallets ALTER COLUMN peer_key_share_data SET NOT NULL;
