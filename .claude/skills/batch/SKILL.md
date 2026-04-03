---
name: batch
description: Implement multiple tasks in parallel using isolated worktree agents. Each agent runs the full implement pipeline autonomously. Use when user wants to implement multiple stories at once.
user-invocable: true
argument-hint: "[task-ids, space-separated]"
model: claude-opus-4-6
allowed-tools: Bash(gh *), Bash(git *), Read, Agent
---

# Batch Implement — Tasks $ARGUMENTS

You are the **orchestrator**. You launch parallel agents to implement multiple tasks simultaneously. Each agent works in an **isolated git worktree** so there are no conflicts.

**IMPORTANT RULES:**
- You (the orchestrator) NEVER write code. You only coordinate.
- Each agent runs autonomously — no user confirmation pauses.
- All agents branch from `main`.

---

## Step 1 — Validate Tasks

For each task ID in `$ARGUMENTS`:
1. If GitHub issue: `gh issue view <number> --json title,body,labels,state`
2. If CDA- task: read from the plan file (`.claude/plans/`)
3. Check the Dependency Graph in the plan to confirm these tasks can run in parallel (no inter-dependencies)

If any tasks depend on each other, **warn the user** and suggest splitting into sequential batches.

**List all tasks with titles and confirm with the user before launching agents.**

---

## Step 2 — Launch Parallel Agents

Launch one Agent per task using **`isolation: "worktree"`** and **`run_in_background: true`**. Send ALL agent launches in a **single message** for true parallelism.

For each task, use this agent prompt template:

````
You are implementing task CDA-<TASK_ID> (<TASK_TITLE>) end-to-end in an isolated worktree. Follow every phase below. Do NOT skip any phase.

## Phase 1 — Gather Context
1. Read the task details from the plan file or GitHub issue
2. Read CLAUDE.md and SPEC.md for all coding standards
3. Explore the codebase to understand existing patterns

## Phase 2 — Implement
1. Create branch: `git checkout -b feature/cda-<TASK_ID>-<short-name> main`
2. Write code following ALL CLAUDE.md + SPEC.md standards:
   - Hexagonal architecture (domain/infrastructure/application)
   - Java records + @Builder(toBuilder = true)
   - @Slf4j, @RequiredArgsConstructor, static imports, var, functional style
   - MapStruct for mapping, domain exceptions with factory methods
   - REACTIVE: Controllers return Mono<>/Flux<>, services return Mono<>/Flux<>
   - R2DBC: @Table entities, ReactiveCrudRepository, DatabaseClient
   - ReactiveRedisTemplate for cache, WebClient for outbound HTTP
   - NEVER .block(), Thread.sleep(), or synchronous I/O
3. Write tests:
   - Single-assert pattern with recursive comparison
   - BDDMockito (given/then), no generic matchers
   - // given, // when, // then comments
   - Factory methods in src/testFixtures/*Fixtures.java
   - StepVerifier for reactive assertions
   - @WebFluxTest + WebTestClient for controller tests
4. Run `./gradlew build` — must pass. Fix and rebuild until green.

## Phase 3 — Self-Review
Run `git diff main...HEAD` and review EVERY changed file against:

BEFORE COMMITTING — verify:
- No wildcard imports, use static imports properly
- @Slf4j on all production classes, var for obvious types
- All records have @Builder(toBuilder = true)
- Controllers return Mono<>/Flux<> — no blocking
- No .block(), .blockFirst(), .blockLast() in production code
- WebClient for outbound HTTP — never RestTemplate
- R2DBC @Table — never JPA @Entity
- StepVerifier for reactive tests, WebTestClient for controllers
- Single-assert pattern, BDDMockito only (given/then)
- No generic matchers (never any(), anyString(), eq())
- // given, // when, // then comments in every test
- All test factory methods in src/testFixtures/*Fixtures.java
- MapStruct for all mapping, domain exceptions with factory methods
- Domain imports: only @Service, @Transactional, Lombok, reactor.core.publisher
- Constructor injection via @RequiredArgsConstructor only
- ./gradlew build passes
Fix any violations before committing.

## Phase 4 — Fix Issues
If self-review found issues: fix them, rebuild, re-review. Loop until clean.

## Phase 5 — Commit & Push
1. Stage specific files (never git add -A)
2. Commit: `CDA-<TASK_ID>: <description>`
   Include: Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>
3. Push: `git push -u origin feature/cda-<TASK_ID>-<short-name>`

## Phase 6 — Create PR
gh pr create --assignee @me --title "CDA-<TASK_ID>: <title>" --body "$(cat <<'EOF'
## Summary
<1-3 bullets>

## Task Reference
CDA-<TASK_ID>

## Acceptance Criteria
| Criterion | Status |
|-----------|--------|
| (each from task) | Done |

## Test Plan
- [ ] Unit tests pass
- [ ] ./gradlew build green
- [ ] Self-review checklist verified
- [ ] No blocking calls in reactive chains

Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"

## Final Output
Report: PR URL, branch name, summary, self-review iterations count.
````

---

## Step 3 — Monitor & Report

As each agent completes, collect its results.

Once ALL agents have finished, produce a **batch summary**:

### Batch Results

| Task | Title | Branch | PR | Status |
|------|-------|--------|-----|--------|
| CDA-XX | ... | feature/cda-XX-... | #YY | Done / Failed |

### Next Steps
- List any tasks that failed and why
- Suggest the next batch from the Dependency Graph if applicable
- Remind the user to merge these PRs before starting dependent batches
