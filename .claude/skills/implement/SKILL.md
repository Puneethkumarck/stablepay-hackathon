---
name: implement
description: Full story lifecycle — implement a task/issue, self-review against coding standards, fix issues, push, and create PR. Use when user wants to implement a story/ticket end-to-end.
user-invocable: true
argument-hint: "[issue-number-or-task-id]"
model: claude-opus-4-6
allowed-tools: Bash(gh *), Bash(git *), Bash(./gradlew *), Read, Grep, Glob, Agent, Write, Edit
---

# Implement Story — Task $0

You are implementing task $0 end-to-end. Follow this pipeline strictly. **Do NOT skip any phase.** Each phase must fully complete before moving to the next.

---

## Phase 1 — Gather Context

1. **Read the task**: If GitHub issue, run `gh issue view $0 --json title,body,labels`. If CDA- task, read from the plan file.
2. **Extract**: title, description, acceptance criteria, deliverables
3. **Read CLAUDE.md**: Refresh on all coding standards, architecture rules, and the Pre-Commit Self-Review Checklist
4. **Read SPEC.md**: Refresh on reactive stack rules, R2DBC patterns, WebFlux conventions
5. **Explore codebase**: Understand existing patterns, related code, and where changes need to go
6. **Identify dependencies**: Check if this task depends on other tasks being complete

**Output**: Summarize what you'll implement and your approach. Ask the user to confirm before proceeding (skip confirmation if running as a sub-agent).

---

## Phase 2 — Implement

1. **Create branch**: `git checkout -b feature/cda-$0-<short-name> main`
2. **Write code**: Follow all CLAUDE.md + SPEC.md coding standards:
   - Hexagonal architecture (domain/infrastructure/application)
   - Java records + `@Builder(toBuilder = true)`
   - `@Slf4j`, `@RequiredArgsConstructor`, static imports, `var`, functional style
   - MapStruct for mapping, domain exceptions with factory methods
   - **Reactive**: Controllers return `Mono<>`/`Flux<>`, services return `Mono<>`/`Flux<>`
   - **R2DBC**: `@Table` entities, `ReactiveCrudRepository`, `DatabaseClient` for complex queries
   - **Reactive Redis**: `ReactiveRedisTemplate` for cache operations
   - **WebClient**: For all outbound HTTP (RPC, CoinGecko, webhook dispatch)
   - **No blocking**: Never `.block()`, `Thread.sleep()`, or synchronous I/O
3. **Write tests**: Follow test standards:
   - Single-assert pattern with recursive comparison
   - BDDMockito (`given`/`then`), no generic matchers
   - `// given`, `// when`, `// then` comments
   - Factory methods in `src/testFixtures/*Fixtures.java`
   - `StepVerifier` for reactive assertions
   - `@WebFluxTest` + `WebTestClient` for controller tests
   - Reactive stubs: `given(repo.findById(id)).willReturn(Mono.just(entity))`
4. **Build**: Run `./gradlew build` — must pass (compile + Spotless + all tests)
   - If build fails, fix and rebuild until green

---

## Phase 3 — Self-Review

**CRITICAL: Do NOT skip this phase. Review your own changes thoroughly before pushing.**

Run `git diff main...HEAD` to see all changes, then review every file against:

### Imports & Style
- No FQDN inline usage — proper `import` statements at the top
- No wildcard imports — every import fully qualified
- Static imports for enum constants, `assertThat`, `given`/`then`, factory methods
- `var` for local variables where type is obvious from RHS
- `@Slf4j` on all production classes
- Functional style — streams over imperative loops

### Reactive Correctness
- Controllers return `Mono<>` or `Flux<>` — never void or plain objects
- No `.block()`, `.blockFirst()`, `.blockLast()` anywhere in production code
- Error handling via `onErrorResume()`/`onErrorMap()` — not try/catch on publishers
- WebClient for outbound HTTP — never RestTemplate or JDK HttpClient
- R2DBC `@Table` on entities — never JPA `@Entity`

### Domain & Architecture
- Records use `@Builder(toBuilder = true)`
- Constructor injection via `@RequiredArgsConstructor` only
- MapStruct `@Mapper(componentModel = "spring")` for all mapping
- Domain exceptions use static factory methods
- Domain Spring imports: only `@Service`, `@Transactional`, Lombok, `reactor.core.publisher`
- No ArchUnit violations

### Test Patterns
- Single-assert pattern: build expected -> ONE `assertThat(actual).usingRecursiveComparison().isEqualTo(expected)`
- Controller tests: `@WebFluxTest` + `WebTestClient` — no MockMvc
- `StepVerifier` for reactive service/adapter tests
- BDDMockito only — never `when()`/`verify()`
- No generic matchers — never `any()`, `anyString()`, `eq()`
- `// given`, `// when`, `// then` in every test
- All factory methods in `src/testFixtures/*Fixtures.java`

### Acceptance Criteria
- Every criterion from the task is satisfied
- No scope creep — nothing extra beyond what the task asks

**Collect all issues found into a list.**

---

## Phase 4 — Fix Issues

If the self-review found ANY issues:

1. Fix every issue identified in Phase 3
2. Run `./gradlew build` again — must pass
3. **Re-review**: Go back to Phase 3 and verify all fixes are correct
4. Repeat until the review is clean

**Only proceed to Phase 5 when there are ZERO issues remaining.**

---

## Phase 5 — Commit & Push

1. **Stage files**: `git add` specific files (never `git add -A`)
2. **Commit** with a descriptive message:
   ```
   CDA-$0: <concise description of what was implemented>
   ```
   Include `Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>`
3. **Push**: `git push -u origin feature/cda-$0-<short-name>`

---

## Phase 6 — Create PR

```
gh pr create --assignee @me --title "CDA-$0: <title>" --body "$(cat <<'EOF'
## Summary
<what this PR implements, 1-3 bullets>

## Task Reference
CDA-$0

## Acceptance Criteria
| Criterion | Status |
|-----------|--------|
| (each criterion from task) | Done |

## Test Plan
- [ ] Unit tests pass
- [ ] Integration tests pass (if applicable)
- [ ] `./gradlew build` green
- [ ] Self-review checklist verified
- [ ] No blocking calls in reactive chains

## Changes
<brief list of files/packages changed and why>

Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

---

## Final Output

Report:
- PR URL
- Branch name
- Summary of what was implemented
- Number of self-review iterations needed
- Any decisions or trade-offs made during implementation
