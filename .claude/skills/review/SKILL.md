---
name: review
description: Code review a PR against its task, acceptance criteria, and all coding standards from CLAUDE.md/SPEC.md. Use when user wants to review a PR or says "review".
user-invocable: true
argument-hint: "[pr-number]"
model: claude-opus-4-6
allowed-tools: Bash(gh *), Bash(git *), Read, Grep, Glob, Agent
---

# Code Review — PR $0

You are performing a **thorough code review** of PR #$0 against the project's task, acceptance criteria, and coding standards.

## Step 1 — Gather Context

1. **PR details**: `gh pr view $0 --json title,body,headRefName,baseRefName,additions,deletions,changedFiles`
2. **PR diff**: `gh pr diff $0`
3. **Linked task**: Extract the CDA number from the PR title/branch. Read task details from the plan file.
4. **Read CLAUDE.md and SPEC.md**: Refresh on coding standards, reactive rules.

## Step 2 — Review Against Task

- [ ] **Scope**: Does the PR implement exactly what the task asks for? No more, no less.
- [ ] **Acceptance criteria**: Is every criterion satisfied?
- [ ] **Edge cases**: Are edge cases handled?
- [ ] **Missing pieces**: Is anything NOT implemented?

## Step 3 — Coding Standards Review

Check every changed file against ALL standards:

### Imports & Style
- [ ] No wildcard imports
- [ ] Static imports used for enum constants, assertThat, given/then
- [ ] `var` for obvious local variables
- [ ] `@Slf4j` on all production classes
- [ ] Functional style — streams over imperative loops
- [ ] `@UtilityClass` for static-only helpers

### Reactive Correctness
- [ ] Controllers return `Mono<>`/`Flux<>` — never block
- [ ] No `.block()`, `.blockFirst()`, `.blockLast()` in production code
- [ ] No `Mono.just(blockingCall())` — use `Mono.fromCallable().subscribeOn()`
- [ ] Error handling via `onErrorResume()`/`onErrorMap()`
- [ ] WebClient for outbound HTTP — never RestTemplate/JDK HttpClient
- [ ] R2DBC `@Table` — never JPA `@Entity`
- [ ] `ReactiveRedisTemplate` — never blocking `RedisTemplate`

### Domain & Architecture
- [ ] Records use `@Builder(toBuilder = true)`
- [ ] Constructor injection via `@RequiredArgsConstructor` only
- [ ] MapStruct for all layer-boundary mapping
- [ ] Domain exceptions use static factory methods
- [ ] Domain imports: only @Service, @Transactional, Lombok, reactor.core.publisher
- [ ] No ArchUnit violations

### Test Patterns
- [ ] Single-assert pattern with `usingRecursiveComparison()`
- [ ] Controller tests: `@WebFluxTest` + `WebTestClient` — no MockMvc
- [ ] `StepVerifier` for reactive service/adapter tests
- [ ] BDDMockito only — `given()`/`then()`
- [ ] No generic matchers — never `any()`, `anyString()`, `eq()`
- [ ] `// given`, `// when`, `// then` comments
- [ ] All factory methods in `src/testFixtures/*Fixtures.java`
- [ ] Reactive stubs: `given(repo.findById(id)).willReturn(Mono.just(entity))`

### General Quality
- [ ] No security vulnerabilities
- [ ] No unnecessary complexity
- [ ] Thread safety (reactive context — no shared mutable state)
- [ ] Minimal diff — no unrelated changes

## Step 4 — Read Changed Files

For each file in the diff, **read the full file** to check context and consistency.

## Step 5 — Output Review

### Summary
One paragraph: what this PR does, whether it meets the task requirements.

### Verdict
**APPROVE** / **REQUEST CHANGES** / **COMMENT**

### Task Compliance
| Criterion | Status | Notes |
|-----------|--------|-------|
| (each acceptance criterion) | Pass/Fail | (details) |

### Issues Found
Categorize as:
- **BLOCKER**: Must fix before merge (correctness, architecture violations, blocking in reactive chain)
- **MAJOR**: Should fix (coding standard violations, missing tests)
- **MINOR**: Nice to fix (style, naming)
- **NIT**: Optional

Format:
> **[SEVERITY]** `file:line` — Description
> **Suggestion**: How to fix

### What's Good
Highlight things done well.
