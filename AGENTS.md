# ebms-core — agent notes

## Build / test

- CI command: `mvn -B -T 1C clean install -Ddependency-check.skip=true` (GitHub Actions, JDK 21 Temurin, Maven 3.10).
- Fast unit-test loop: `mvn -o -B -pl core test -Dtest=SomeTest -Dsurefire.failIfNoSpecifiedTests=false -Ddependency-check.skip=true -Djapicmp.skip=true -Dlicense.skip=true -Dcheckstyle.skip=true -Denforcer.skip=true`.
- Integration tests (`*IT`) run under failsafe via `verify`, same skips plus `-Dfailsafe.failIfNoSpecifiedTests=false`.
- GOTCHA: stale `target/` classes cause spurious `VerifyError`/`ClassFormatError` ("Invalid method Code length 0") in Lombok `@Builder` classes — always `clean` the module (or use `clean verify`) before trusting such failures; it is a build artifact issue, not a code issue.
- Java sources in `core/` use **tabs** for indentation; whitespace-sensitive edits (str_replace/sed) must use tabs.
- `core/logs/`, `server/logs/` are log4j runtime output (gitignored); delete after test runs if created.

## High availability (Gen3: ShedLock)

- JGroups/Raft and Atomikos JTA were removed; delivery-task single-writer is a ShedLock 7.10.1 JDBC gate: `DAODeliveryTaskExecutor` (core, `client.delivery.handler`) acquires lock `ebms-delivery-task-executor` each poll cycle via `JdbcLockProvider`.
- Lease semantics: `lockAtMostForMillis` (default 300s) must be > `taskAwaitTimeoutMillis` (default 60s), enforced in the constructor; the lease is renewed with `SimpleLock.extend()` before each awaited future; on lost lease the worker cancels remaining futures and never calls `unlock()` (JDBC unlock has no owner check).
- `shedlock` table: columns `name/lock_until/locked_at/locked_by`; create scripts under `plugin/db/*/` and Flyway migrations.
- Two-node behavioural tests: `ShedLockDeliveryGateIT` (H2, real JdbcLockProvider); unit-level: `DAODeliveryTaskExecutorTest` with `TestableSimpleLock`.
- FLAKY (CI, observed 2026-10): `ShedLockDeliveryGateIT.liveLeaseBlocksTheOtherNodeUntilItExpires` can fail under parallel `-T 1C` load — node B polls a few extra times within the 1.5s startup window (assertion tolerates <=1), e.g. "node B polled 1445 times..." on GitHub runners while passing 10/10 serial local runs and on commits without dependency changes; treat a lone failure of this test as a flake and rerun, do not bisect dependency bumps for it.

## Naming

- `ebms-admin` no longer exists: it is `ebms-server` (module `server/`, mainClass `nl.clockwork.ebms.server.embedded.startup.StartEmbedded`). Do not reintroduce admin/raft references.

## Smoke test

- `./smoke-test.sh` brings up two adapters (local shaded server + docker `eluinstra/ebms-adapter-bin`) and runs 22 REST/EbMS assertions; needs Docker running. Green = "22 passed, 0 failed".

