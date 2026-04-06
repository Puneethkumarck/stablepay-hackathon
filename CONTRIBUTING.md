# Contributing to StablePay

## Issue & Branch Naming

### Issue Prefix

All issues use the `STA-{N}` prefix where `{N}` is the GitHub issue number.

```
STA-23: Spring Boot project setup
STA-24: Domain models
```

### Branch Naming

```
feature/STA-{issue-number}-short-description
```

Examples:
```
feature/STA-9-anchor-project-setup
feature/STA-17-eddsa-keygen
feature/STA-23-spring-boot-setup
feature/STA-39-onboarding-home-screen
```

For fixes:
```
fix/STA-{issue-number}-short-description
```

For chores (refactoring, CI, docs):
```
chore/STA-{issue-number}-short-description
```

### Commit Messages

Follow conventional commits, referencing the STA issue:

```
feat(STA-23): add Spring Boot project scaffolding
fix(STA-35): validate UPI format on claim endpoint
chore(STA-27): add ArchUnit layer boundary rules
```

### Pull Requests

- **Title:** `STA-{N}: Description`
- **Branch:** `feature/STA-{N}-description` → `main`
- **Body:** Link to the issue with `Closes #N`
- **Review:** At least 1 approval before merge
- **Merge strategy:** Squash merge to keep main clean

## Workflow

```
1. Pick an issue (STA-N)
2. Create branch: git checkout -b feature/STA-N-description
3. Implement + test
4. Push: git push -u origin feature/STA-N-description
5. Create PR: STA-N: Description (Closes #N)
6. Get review + merge
```

## Code Standards

| Doc | What it covers |
|---|---|
| [CLAUDE.md](CLAUDE.md) | Tech stack, build commands, project structure |
| [docs/CODING_STANDARDS.md](docs/CODING_STANDARDS.md) | Backend (Java): hexagonal architecture, conventions, MapStruct |
| [docs/TESTING_STANDARDS.md](docs/TESTING_STANDARDS.md) | Backend (Java): BDDMockito, golden rule, fixtures, ArchUnit |
| [docs/SOLANA_CODING_STANDARDS.md](docs/SOLANA_CODING_STANDARDS.md) | Solana (Rust/Anchor): program structure, security, testing |
| [docs/ADR.md](docs/ADR.md) | Architecture decisions and rationale |

## Error Codes

Application error codes use the `SP-XXXX` prefix:

```
SP-0001: Remittance not found
SP-0002: Insufficient balance
SP-0003: Claim token expired
```

## Tech Stack Quick Reference

| Component | Command |
|---|---|
| Backend build | `./gradlew build` |
| Backend run | `./gradlew bootRun` |
| Anchor build | `anchor build` |
| Anchor test | `anchor test` |
| MPC sidecar test | `cd mpc-sidecar && go test ./...` |
| Mobile dev | `cd mobile && npx expo start` |
| Web claim dev | `cd web-claim && npm run dev` |
| Infrastructure | `docker compose up -d` |
