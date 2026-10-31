# Documentation/Code Gap: "Per-task delivery claims" backstop is documented but not implemented

> **Status:** documentation leads code. As of `dev` (`15c875e4`, 2.21.0-SNAPSHOT) the
> 2.21 "at-least-once delivery" story is described as resting on **two** idempotence
> backstops — *message content dedup keys* **and** *per-task delivery claims* — but only
> the **MessageId-based duplicate handling** and the **ShedLock gate** exist in code. The
> `V2.23.0` delivery-claim mechanism (`claimed_by`/`claimed_at` + conditional claim) is
> **not present** in the working tree. This note documents the gap precisely so it can be
> either (A) implemented or (B) reconciled by softening the docs.

---

## 1. What the documentation claims

### `CHANGELOG.md` (root), § "ebms-core restructure: high availability trade-off (2.21)"

> **Guarantee: at-least-once delivery with idempotence backstops** — message content
> dedup keys, and per-task delivery claims (Flyway migration **V2.23.0**: nullable
> `claimed_by`/`claimed_at` columns + index on `delivery_task` in all db plugins; claimed
> atomically via a conditional `UPDATE ... WHERE claim IS NULL OR EXPIRED`, claim validity
> bound to the ShedLock `lockAtMostFor`).

And, on the upgrade guidance for `deliveryTaskHandler.type=JMS` + Atomikos:

> either **accept at-least-once** delivery (**dedup keys plus delivery claims already
> prevent** the double-dispatch window that motivated the XA setup), or switch to
> `deliveryTaskHandler.type=DEFAULT`.

### `ebms-core/AGENTS.md`, § "High availability (Gen3: ShedLock)"

Describes the single-writer guarantee as provided by the **ShedLock 7.10.1 JDBC gate**
(`DAODeliveryTaskExecutor`, lock `ebms-delivery-task-executor`). This part **is**
implemented (see §3).

---

## 2. What the code actually does (verified on `dev`)

### 2a. There is no `V2.23.0` migration and no claim column

- Highest Flyway migration in every `plugin/db/*/` is `V2.22.0__CreateShedLockTable.sql`
  (plus the 2.21 `V2.21.0__Drop_Quartz_Tables.sql`). There is **no `V2.23.0`** file.
- A search of the whole module for `claimed_by`, `claimed_at`, `claim IS NULL`, or
  `V2.23` returns **no hits** in any `.sql` or `.java` file (outside `target/`).

### 2b. The delivery-task poll query has **no claim predicate**

`core/src/main/java/nl/clockwork/ebms/client/delivery/task/DeliveryTaskDAOImpl.java`:

```java
// getTasksBefore(timestamp, serverId)            — line 62
" select ... from delivery_task"
" where time_stamp <= ?"
" (and server_id = ?)"
" order by time_stamp asc"
```

There is no `claimed_by IS NULL` / `claimed_at < ?` / conditional claim `UPDATE`.
`insertTask` (line 90) and `updateTask` (line 107, `set time_stamp=..., retries=... where
message_id=?`) likewise have no claim columns. So the *atomic claim* the changelog
describes does not exist; tasks are simply polled by `time_stamp` (and optional
`server_id`).

### 2c. What actually prevents double-dispatch is the **ShedLock gate**

`core/src/main/java/nl/clockwork/ebms/client/delivery/handler/DAODeliveryTaskExecutor.java`
acquires lock `ebms-delivery-task-executor` for the duration of each poll cycle (lease
`lockAtMostForMillis`, default 300 s, renewed via `SimpleLock.extend()` before each awaited
future). This guarantees **at most one node polls+dispatches per cycle** — i.e. the
single-writer property — which is the real current backstop, and it **is** tested by
`ShedLockDeliveryGateIT` (H2, real `JdbcLockProvider`) and `DAODeliveryTaskExecutorTest`.

### 2d. The "dedup" backstop is **MessageId-based**, not a **content dedup key**

`core/.../server/message/processor/duplicate/DuplicateMessageHandler.java`:

```java
// line 60 / 146 / 154
throw new EbMSProcessingException("MessageId " + ... + " already used!");
```

Duplicate detection keys off the existing `messageId` (and `refToMessageId` for async
responses), via `EbMSDAO`. There is **no content hash / content dedup key** column or
index. Whether the changelog's phrase "message content dedup keys" was meant to describe
this MessageId-based handler (terminology mismatch) or a not-yet-built content-key
mechanism is **ambiguous** and should be clarified either way.

---

## 3. Concrete gap table

| Documented backstop (2.21) | Present in code on `dev`? | Evidence |
|---|---|---|
| ShedLock single-writer gate | **Yes** | `DAODeliveryTaskExecutor` + `ShedLockDeliveryGateIT` |
| "Message content dedup keys" | **Partial / ambiguous** | Only `DuplicateMessageHandler` (MessageId-based); no content key |
| "Per-task delivery claims" (V2.23.0: `claimed_by`/`claimed_at` + conditional claim) | **No** | No migration, no column, no claim predicate in `DeliveryTaskDAOImpl` |

---

## 4. Why it matters (the risk the docs are trying to close)

With XA removed, "at-least-once" means a delivery task can, in principle, be **picked up
more than once** across a restart/lease-expiry or a JMS/Kafka redelivery. The changelog's
claim is that *dedup + delivery claims* close that window. Today the only guard is the
**ShedLock lease**: if a node is killed mid-`lockAtMostFor` (or the lease lapses) and a
task's HTTP send already succeeded on the wire but the `delivery_task` row was not yet
deleted, another node can re-send it. The *receiving* adapter de-duplicates by
`MessageId` (so a re-sent identical message is not double-processed), but:

- a **resend with a new MessageId** (the `resendMessage` API path) is *not* caught by
  MessageId dedup, and
- there is no **per-task claim** to distinguish "in-flight, do not touch" from
  "abandoned, safe to reclaim".

So the *delivery-side* at-least-once window the docs say "delivery claims already prevent"
is in fact only bounded by the ShedLock lease + receive-side MessageId dedup.

---

## 5. Recommended resolution (pick one, keep docs and code in sync)

### Option A — Implement the documented claim (makes the docs true)
1. Add Flyway migration `V2.23.0__AddDeliveryTaskClaim.sql` to **every** `plugin/db/*/`:
   nullable `claimed_by`, `claimed_at` columns + index on `delivery_task`.
2. In `DeliveryTaskDAOImpl`:
   - `getTasksBefore(...)` → only select `where ... and (claimed_by is null or claimed_at < ?)`
     (`?` = now − lease), **or**
   - replace the plain poll with a conditional claim `update delivery_task set claimed_by=?,
     claimed_at=? where message_id=? and (claimed_by is null or claimed_at < ?)` and treat
     `updatedRows==1` as "I own this task".
3. Clear the claim on success/expiry (`deleteTask` already removes the row; also clear on
   failure paths that keep the row for retry).
4. Add an IT proving a second node cannot claim a live task (complement
   `ShedLockDeliveryGateIT`).

### Option B — Reconcile the docs to the code (fastest, honest)
Rewrite the two `CHANGELOG.md` bullets (and any AGENTS.md reference) to state the actual
guarantee: **at-least-once, guarded by the ShedLock single-writer lease plus receive-side
MessageId dedup**, and drop (or clearly mark *planned*, not shipped) the `V2.23.0`
delivery-claim mechanism. Fix the "content dedup keys" wording to "MessageId-based
duplicate handling" unless a content key is really intended.

**Do not ship Option B wording as the released guarantee until the double-dispatch window
in §4 is consciously accepted**, since the changelog currently uses that sentence to tell
users it is safe to drop Atomikos XA.

---

## 6. Out of scope

- This note does **not** change the ShedLock gate (working and tested).
- It does **not** address the separate C1 pagination issue (MariaDB 10.5 `offset 0 rows
  fetch first N rows only`) also listed in the 2.21 changelog.
