# MPC 2-of-2 Signing Fix — Task List

## Phase 1: Data Layer
- [x] **Task 1** — Add `peer_key_share_data` column (`V4__add_peer_key_share_to_wallets.sql`)
- [x] **Task 2** — Add `peerKeyShareData` to `GeneratedKey`, `Wallet`, `WalletEntity`

## Checkpoint: `./gradlew compileJava` passes

## Phase 2: Orchestration
- [x] **Task 3** — Capture peer key share during DKG in `MpcWalletGrpcClient.generateKey()` + store in `CreateWalletHandler`
- [x] **Task 4** — Implement 2-party signing: `triggerPeerSigning()`, update `signTransaction()` signature, update `SolanaTransactionServiceAdapter`

## Checkpoint: `./gradlew build` passes (compile + format + unit tests)

## Phase 3: Test Updates
- [x] **Task 5** — Update fixtures (`MpcFixtures`, `WalletFixtures`) and all tests (`MpcWalletGrpcClientTest`, `CreateWalletHandlerTest`, `SolanaTransactionServiceAdapterTest`)

## Checkpoint: `./gradlew build` — 0 failures, 0 format issues

## Phase 4: E2E Validation
- [x] **Task 6** — MPC 2-of-2 signing works E2E (both sidecars cooperate, signature produced, transaction reaches Solana devnet)
  - Note: On-chain tx fails with "no record of a prior credit" — MPC wallet needs devnet SOL/USDC airdrop for full escrow flow
