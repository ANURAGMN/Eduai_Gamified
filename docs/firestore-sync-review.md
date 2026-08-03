# Firestore Quota & Sync Architecture — Review and Plan

**Scope:** Android app (`Eduapp`) Firestore read/write patterns.
**Goal:** Reduce Firestore quota consumption (shared ~50k reads / 20k writes per day on the free tier) without regressing product data.
**Status of this doc:** Findings below were verified against the codebase (file + line references given). Recommendations are proposed, not yet implemented.

---

## 1. TL;DR

- The **data model is already correct**: Room is the source of truth, entities carry an `isSynced` outbox flag, uploads are batched (`BATCH_SIZE = 100`), and there are WorkManager workers with retry/backoff.
- The **sync *trigger policy* is wrong**: several hot paths do **immediate, per-event Firestore I/O**, and one path (`triggerFullSync()`) re-uploads *everything unsynced across all collections* on **every** progress/streak milestone and on network reconnect.
- A **flag meant to stop high-frequency mirroring (`AnalyticsFirestoreMirror.ENABLED = false`) is bypassed for screen analytics** — clicks/ads respect it, screens do not — so every screen entry + exit writes to Firestore.
- **The agentic (chat) path is not the quota problem.** Chat/sim/math messages go over REST to EC2; only *completion milestones* touch Firestore (via the progress path).
- The fix is **not a new architecture** — it is: stop eager syncs, coalesce into a debounced/lifecycle flush, and gate/relocate low-value analytics.

**Estimated impact:** roughly **5–10× fewer writes per session** and a large drop in reads once progress restore uses a delta filter.

---

## Implementation status

- **Phase 1 (write wins) — DONE, builds.** Screen analytics gated + routed to GA4 (`screen_view` / `screen_time`); `scheduleDeferredUpload()` (5-min coalesced, stable unique name); `ProgressRepository` + `StreakRepository` rewired off `triggerFullSync()` (streak trio dropped); full-outbox flush moved into `AppLifecycleObserver.onStop`; one-time worker unique-name bug fixed.
- **Phase 2 (read wins) — DONE, pending build/QA.** Progress restore is now a **delta** (`whereGreaterThan("updatedAt", lastSync)`) with per-user cursor and last-write-wins (no clobber of newer local). Concept catalog pull is **gated** (skip full `Concept.get()` when content exists and was pulled within a 3-day TTL; empty DB always pulls). New prefs: `progress_last_sync_{uid}`, `content_last_pull`. New DAO: `ConceptDao.getConceptCount()`.
- **Tests:** the pure decisions were extracted into `SyncPolicy` (catalog gate / delta cursor+self-heal / last-write-wins) and covered by `SyncPolicyTest` (plain JUnit); the TTS sanitizer's non-breaking-space case was added to `SimulationIntroTtsSanitizerTest`. Remaining §12: instrumented/Robolectric coverage for the DAO + WorkManager paths, plus the device QA in §12.3.
- **Pending:** optional backend `content_meta` version doc to make the catalog gate *precise* (pull exactly when content changes) instead of TTL-bounded.
- **Known gaps after Phase 2 (not blockers):**
  1. Delta uses strict `>` on `updatedAt`, so any doc with `updatedAt == 0`/missing won't restore — fine for current uploads (all use `progressRecordPayload`, which always writes it).
  2. **Chapter agent progress** restore now also uses the same delta + LWW + self-heal pattern (cursor `chapter_progress_last_sync_{uid}`). ✅ done.
  3. Delta cursor is per-user (`progress_last_sync_{uid}`) and **not** cleared on logout (logout keeps local progress, so a re-login should stay delta). The **self-heal** (§3.3) covers the case where progress is actually wiped, so a stale cursor can't strand the device.

---

## 2. What already exists (and is correct)

| Piece | Status | Evidence |
|---|---|---|
| Room as source of truth (progress, streak, sessions, analytics) | ✅ | `SessionManager`, `ProgressRepository`, DAOs |
| `isSynced` outbox flag on entities | ✅ | `AppAnalyticsEntity(... isSynced = false)` `SessionManager.kt:172` |
| Batched upload of unsynced rows | ✅ | `BATCH_SIZE = 100` `ProgressAnalyticsSessionSyncManager.kt:36`; `unsynced.chunked(BATCH_SIZE)` `:83, :121, :150, :217` |
| One-time sync worker with backoff | ✅ | `DataSyncWorker` via `scheduleBackgroundSync()` `DataSyncService.kt:268` |
| Daily periodic worker | ✅ | `PeriodicWorkRequestBuilder<WeeklySyncWorker>(1, TimeUnit.DAYS)`, `enqueueUniquePeriodicWork("DAILY_SYNC_WORK", KEEP)` `EduAiApplication.kt:148` |
| Background hook on app → background | ⚠️ Partial | `AppLifecycleObserver.onStop` only calls `DataSyncService.syncSimulationInteractions()` `AppLifecycleObserver.kt:39` |

> Note: despite the name, `WeeklySyncWorker` runs **daily**.

---

## 3. What's fighting the pattern (verified findings)

### 3.1 Screen analytics leak to Firestore despite the mirror flag — **CONFIRMED**

- The flag exists and is `false`: `AnalyticsFirestoreMirror.ENABLED = false` (`AnalyticsFirestoreMirror.kt:8`).
- **Clicks/ads respect it** — `AnalyticsEventRecorder` sets `isSynced = !ENABLED` and only syncs `if (ENABLED)` (`AnalyticsEventRecorder.kt:53, :56`). With the flag off, clicks/ads never hit Firestore. ✅
- **Screens bypass it** — a different path: `TrackScreenEvent` → `SessionManager.trackScreenEntry/Exit` inserts a row with `isSynced = false` and calls `DataSyncService.syncAnalyticsUpdate(...)` **unconditionally** (`SessionManager.kt:172, :175, :209`).
- That lands in `ProgressAnalyticsSessionSyncManager.syncAnalyticsUpdate`, which does a live `docRef.set(data)` to `analytics/{uid}/events/{id}` **with no `ENABLED` check** (`ProgressAnalyticsSessionSyncManager.kt:299–316`).

**Effect:** ~2 Firestore writes per screen visit (entry + exit), plus extra on `ON_START` re-entry. A 15-screen session ≈ **30 writes** just from navigation.

### 3.6 Login / auth burst — **CONFIRMED**

`DataSyncService.onUserAuthenticated()` (`:311`) runs, in one shot: analytics + session `backfillEmptyStudentId(studentId)` (marks pre-login rows dirty), `gardenSyncManager.restoreGarden(studentId)` (reads), then `triggerFullSync()` (`:320`, full multi-collection upload). Around the same login flow there is also a `users` query (1–2 reads), an `auth_index` write, and — on an empty local DB — a full `Concept.get()` catalog pull (`FirebaseSyncManager.syncAllContent:50`). Net: **login is both a read spike** (catalog + full progress restore + users) **and a write spike** (backfill + full sync + auth_index).

### 3.7 Other (smaller) Firestore sources

- **Error logging** — `FirestoreErrorLogger` does `errors/{app}/logs.add(...)` (`:90–95`): **1 write per logged error**. This can spike *exactly* during quota failures (failed ops log errors, which are themselves writes).
- **The "daily" worker is not just a catalog pull** — `WeeklySyncWorker.doWork` runs `syncAllContent()` (catalog read, `:47`), `syncAllUnsyncedData()` (full upload, `:67`), **and** `syncUserProgress()` (full progress restore, `:71`). So it is a daily **read + write** spike, not a read-only refresh.
- **Friends on Home** — opening Home touches friend data (`friend_codes` write + feed read). Low volume, but non-zero.

### 3.2 `triggerFullSync()` on every milestone — **CONFIRMED (and broader than expected)**

Callers:
- `ProgressRepository.kt:55`, `:180` (progress milestones)
- `StreakRepository.kt:97` (streak events)
- `DataSyncService.kt:254`, `:320` (network reconnect / chained after progress sync)

Each call runs `syncAllUnsyncedData()`, which uploads **progress + analytics + sessions + streak + chapter progress**, then also `syncSimulationInteractionsInternal()` and `gardenSyncManager.pushGarden()` (`DataSyncService.kt:183–198`). So **one milestone fans out to a multi-collection upload**, and a reconnect blip repeats it.

**Streak is worse — a *triple* write per event.** `StreakRepository.updateStreak()` does all three in sequence: `triggerFullSync()` (`:97`, full outbox), then a **direct** `firebaseRepository.updateStreak(...)` doc write (`:100`), then `friendFeedService.onStreakUpdated(...)` (`:108`, friend-feed write). So a single streak update = full-outbox sync **+** direct streak write **+** friend-feed write.

### 3.3 Login/restore reads the whole progress subcollection — **CONFIRMED → fixed in Phase 2**

Originally `FirebaseSyncManager.syncUserProgress` did `collection("records").get()` and filtered **client-side** with `shouldRestoreProgressRecord(...)` — every restore downloaded *all* progress docs, with no server-side delta.

**Phase 2 replaced this** with a delta query `whereGreaterThan("updatedAt", lastSync)`: a per-user cursor (`progress_last_sync_{uid}`), **last-write-wins** so a newer local (possibly unsynced) row is never clobbered, and a **self-heal** — if the cursor is set but local progress is empty (a wipe), it ignores the cursor and re-pulls in full. See *Implementation status*.

### 3.4 Latent WorkManager dedup bug — **CONFIRMED**

`scheduleBackgroundSync()` enqueues with a **timestamped unique name** `"DATA_SYNC_WORK_${System.currentTimeMillis()}"` + `ExistingWorkPolicy.KEEP` (`DataSyncService.kt:270–283`). Because the name is always different, `KEEP` never dedupes and multiple workers can pile up. (The daily periodic worker uses a *stable* name and is fine.)

### 3.5 Screens are **not** in GA4 today — **IMPORTANT ASSUMPTION CORRECTION**

`SessionManager` (the screen path) contains **no** `FirebaseAnalytics`/`logEvent` call — screen entry/exit exist only as Room rows synced to Firestore. The comment "screens go to GA4 only" is aspirational. **Consequence:** simply removing the screen Firestore write **loses screen-duration analytics** unless GA4 `screen_view` logging is added first.

---

## 4. Agentic vs non-agentic (why the agent API is not the quota problem)

| Path | Storage | Firestore footprint |
|---|---|---|
| Chat / sim / math / revision **messages** | EC2 via REST (`SessionUseCase` → `agenticAIClient`) + Postgres | **None** for messages |
| Users allowlist read | Firestore | ~1 read / 5 min / user (cached) |
| App state (progress, streak, chapter, garden, sessions, analytics) | Room → Firestore | **Continuous, multiplies per user** |

**Nuance:** the agentic *flow* still writes Firestore at **completion milestones** — solving/finishing a sim/study/math/revision item runs the progress path (`markSimulationCompleted` → `syncProgressUpdate` / `triggerFullSync`). So "agent chat never writes Firestore" is true per-message but not per-completion. Fixing §3.2 also trims that completion burst.

---

## 5. Key mental-model correction: batching ≠ quota reduction

Firestore quota is billed **per document operation**. 100 docs in one `WriteBatch` still counts as 100 writes.

- **`WriteBatch` / `BATCH_SIZE`** → fewer round-trips, atomicity, cheaper retries. Keep it. **Does not reduce the write count.**
- **Coalescing** (write each dirty doc *once per flush* instead of on every intermediate change) → the real quota win.
- **Not writing at all** (screen analytics when the mirror is off) → the biggest, free win.

Any quota-reduction estimate should be attributed to coalescing + not-writing, never to batch size.

---

## 6. Recommended sync policy (tiers)

### Tier 0 — Always local (instant UI)
- All reads/writes go to Room first; UI never waits on Firestore.
- Any local change sets `isSynced = false`.
- Already true for progress locally — just stop calling Firestore from the same code path.

### Tier 1 — Debounced push (while app is open)
- Replace `triggerFullSync()` with `scheduleDeferredUpload()`:
  - `enqueueUniqueWork("firestore_upload", ExistingWorkPolicy.KEEP, <delayed OneTimeWork>)` (stable name), **or** an in-process debounce timer.
  - Window **N = 5–15 min** (a Class‑7 tutor app does not need second-level cross-device sync).
  - Coalesces e.g. 10 progress events in the window into **1** batched upload.

### Tier 2 — Lifecycle flush (background / closing) — highest value
- In `AppLifecycleObserver.onStop`, extend beyond simulations: flush the **whole outbox** (progress, chapter progress, streak, sessions, analytics) once.
- Best moment: user switches apps / locks phone / goes home — most learning sessions end this way, so one flush captures the session.
- Optional: catch-up flush on `onStart` after a long background.

### Tier 3 — Periodic safety net (WorkManager)
- Keep the daily worker. Optionally add/repurpose a lighter **upload-only** worker at **15–30 min** (WorkManager periodic minimum is 15 min) to catch anything missed (killed app, never-backgrounded).
- Separate **upload** (cheap, frequent) from **download** (expensive, rare).

### Tier 4 — Pull from Firestore (reads have different rules)
| Data | Pull when |
|---|---|
| Concept catalog | App install, manual refresh, or server `contentVersion` changed |
| User progress restore | Login only, with `where updatedAt > lastSync` (delta) |
| Streak / garden | Login + after a successful upload (conflict resolution) |

---

## 7. Techniques

1. **Outbox pattern** — `isSynced = false` → worker uploads only dirty rows → mark synced. *(Have it.)*
2. **Debounce + coalesce** — one scheduled worker per window, not one sync per event. *(Need it.)*
3. **Lifecycle flush** — sync when the user leaves the app. *(Partially there — simulations only.)*
4. **Batch commits** — keep `BATCH_SIZE = 100` for reliability (not for quota). *(Have it.)*
5. **Stop mirroring low-value data** — screens shouldn't hit Firestore; **but** add GA4 `screen_view` first (see §3.5) or accept dropping the metric.
6. **Hot vs cold paths:**
   | Hot (defer/batch) | Cold (immediate OK) |
   |---|---|
   | Progress, chapter progress, streak | New user registration |
   | Sessions, simulation daily blob | Settings/profile save (user expects it) |
   | Screen analytics (prefer: don't sync) | Rare social milestones |
7. **Network-aware, not network-triggered** — do not `triggerFullSync()` on every reconnect. Instead: *if outbox non-empty and last sync > N min → schedule upload.*

---

## 8. Implementation plan (sequenced, smallest high-impact first)

1. **Gate screen sync + preserve the metric.**
   - Make `SessionManager` respect `AnalyticsFirestoreMirror.ENABLED` (mirror `AnalyticsEventRecorder`: insert with `isSynced = !ENABLED`, and only `syncAnalyticsUpdate(...)` if `ENABLED`).
   - **Before/with this:** add a GA4 `screen_view` `logEvent` in `SessionManager`, or explicitly decide screens stay Room-only. *(See §3.5 — do not skip.)*
2. **Add `DataSyncService.scheduleDeferredUpload()`** — debounced (5–15 min), **stable** unique work name, `ExistingWorkPolicy.KEEP`.
3. **Rewire hot paths** — replace `triggerFullSync()` in `ProgressRepository` (`:55`, `:180`) with `scheduleDeferredUpload()`. For **streak** (`StreakRepository:96–108`) drop the eager *trio*: remove `triggerFullSync()`, make the direct `firebaseRepository.updateStreak(...)` deferred (write Room + `isSynced = false`; let the flush push it), and let `friendFeedService.onStreakUpdated(...)` ride the flush instead of writing immediately. Make the reconnect/login paths (`DataSyncService:254/320`) **schedule-if-dirty** rather than sync-always. **Model to copy:** `ConceptSimulationViewModel:373` already does a **targeted single-doc** `syncProgressUpdate(progressId)` — that (or full deferral) is the pattern the other hot paths should follow, instead of `triggerFullSync()`.
4. **Full outbox flush in `AppLifecycleObserver.onStop`** — flush all outbox types, ordered **after** `SessionManager.endSession()` so the final rows are included.
5. **Fix the one-time worker name** — give `scheduleBackgroundSync()` a stable unique name so `KEEP` actually dedupes.
6. **(Reads) Delta restore** — add `whereGreaterThan("updatedAt", lastSync)` to `FirebaseSyncManager.syncUserProgress` and persist `lastSync`. Version-gate the Concept catalog pull.

Items 1–5 are the write-side wins (small, contained). Item 6 is the biggest read-side win (slightly larger — needs an `updatedAt` index + `lastSync` bookkeeping).

---

## 9. Risks & UX impact

| Concern | Impact |
|---|---|
| Progress on same device | None — Room is instant |
| Progress on a second device | Delayed to next flush (5–15 min or background) — acceptable for this app |
| Login on a new phone | Unchanged — still pulls at login (now delta-filtered) |
| App force-killed | No data loss (Room persists); flush on next `onStart`/reconnect. Small staleness window only |
| Conflict on pull | Use `updatedAt` last-write-wins **both directions** so a delayed local write isn't clobbered by an older server doc |
| Quota | Drops sharply — writes coalesced, reads delta-gated |

---

## 10. Rough quota impact (illustrative)

**Today — one study session**
- ~4 × `triggerFullSync()` × ~5–20 writes ≈ 20–80 writes
- 15 screen hops × 2 analytics writes ≈ 30 writes
- **≈ 50–110 writes/session**

**With Room-first + debounced batch + no screen Firestore**
- 1 batch on background ≈ 5–10 writes
- Optional periodic batch ≈ 0–5 more
- **≈ 5–15 writes/session (~5–10× reduction)**

Reads drop similarly once progress restore is delta-filtered and the catalog pull is version-gated instead of a daily full `Concept.get()`.

---

## 11. Open items to confirm before coding

- **GA4 screen logging**: decide (a) add `screen_view` to GA4, (b) keep screens Room-only, or (c) drop the metric. Required before gating §3.1.
- **Concept catalog**: confirmed — full `Concept.get()` (`FirebaseSyncManager.syncAllContent:50`), run on empty-DB login and **daily** via `WeeklySyncWorker`. Remaining: confirm the exact **doc count (~N concepts)** for precise quota math, then version-gate.
- **Debounce mechanism**: WorkManager delayed unique work (survives process death, 15-min-ish granularity) vs in-process timer (finer, lost on kill). Recommended: WorkManager for durability + `onStop` flush for immediacy.

---

## 12. Testing plan (cases & scenarios)

Organized as **unit → integration → manual/QA → regression → acceptance**. Each row lists the trigger and the exact assertion. Use fakes for Firestore (a `WriteCounter` / recording `FirebaseFirestore` double), Room in-memory DB, and a `TestListenableWorkerFactory` / `WorkManagerTestInitHelper` for workers.

### 12.1 Unit tests

**A. Screen-analytics gating (`SessionManager` + `AnalyticsFirestoreMirror`)**

| # | Scenario | Expected |
|---|---|---|
| A1 | `ENABLED = false`, `trackScreenEntry()` | Room row inserted; `isSynced = true` (i.e. `!ENABLED`); `DataSyncService.syncAnalyticsUpdate` **not** called |
| A2 | `ENABLED = false`, `trackScreenExit()` | Room row updated; **no** Firestore sync call |
| A3 | `ENABLED = false`, entry + exit | GA4 `logEvent("screen_view", …)` called **once per entry** (metric preserved) |
| A4 | `ENABLED = true`, entry | `isSynced = false`; `syncAnalyticsUpdate` **is** called (old behaviour restored) |
| A5 | No active session id | Early-return; no Room insert, no sync, no crash |

**B. Debounced upload (`scheduleDeferredUpload`)**

| # | Scenario | Expected |
|---|---|---|
| B1 | 10 calls within the window | `enqueueUniqueWork(name, KEEP, …)` results in **exactly one** enqueued worker (dedup by stable name) |
| B2 | Window elapses, worker runs, outbox non-empty | `syncAllUnsyncedData()` (or targeted) called **once** |
| B3 | Outbox empty when scheduled/flushed | **No** Firestore write (no-op) |
| B4 | New dirty event *after* a completed flush | A **new** window is scheduled |
| B5 | Stable unique name used | Two rapid schedules → second is `KEEP`-deduped, not a second worker |

**C. Repository rewire (hot paths)**

| # | Scenario | Expected |
|---|---|---|
| C1 | `ProgressRepository.mark…()` | Room row `isSynced = false`; `scheduleDeferredUpload()` called; `triggerFullSync()` **not** called |
| C2 | `StreakRepository` streak event | Same as C1 |
| C3 | Reconnect with **empty** outbox | **No** upload scheduled |
| C4 | Reconnect with dirty outbox AND `now − lastSync > N` | Upload scheduled (once) |
| C5 | Reconnect with dirty outbox but `now − lastSync ≤ N` | No immediate upload (debounce still owns it) |

**D. Lifecycle flush (`AppLifecycleObserver.onStop`)**

| # | Scenario | Expected |
|---|---|---|
| D1 | `onStop` with dirty rows across types | Flush covers progress + chapter + streak + sessions + analytics (not just simulations) |
| D2 | Ordering | `SessionManager.endSession()` completes **before** flush, so the final screen-exit row is included |
| D3 | `onStop` with empty outbox | **No** Firestore write |
| D4 | `onStart` after long background (if catch-up enabled) | Pending outbox flushed once |

**E. Worker dedup fix (`scheduleBackgroundSync`)**

| # | Scenario | Expected |
|---|---|---|
| E1 | Two rapid `scheduleBackgroundSync()` | Stable unique name + `KEEP` → **one** worker, not two |
| E2 | Worker success | Unsynced rows marked `isSynced = true` |
| E3 | Worker failure (Firestore throws) | Rows remain `isSynced = false`; `Result.retry()`; backoff applies |

**F. Delta restore (`FirebaseSyncManager.syncUserProgress`)**

| # | Scenario | Expected |
|---|---|---|
| F1 | `lastSync = 0` (fresh login) | Full pull (no `whereGreaterThan`, or `> 0`) |
| F2 | `lastSync = T` | Query uses `whereGreaterThan("updatedAt", T)`; only newer docs fetched/applied |
| F3 | After restore | `lastSync` persisted = max `updatedAt` seen |
| F4 | Server doc older than local | Local kept (last-write-wins by `updatedAt`) — no clobber |
| F5 | Malformed/missing `lastSync` | Safe fallback to full pull; no crash |

**G. Outbox / batch integrity**

| # | Scenario | Expected |
|---|---|---|
| G1 | 101 unsynced rows | Chunked into **2** batches (`BATCH_SIZE = 100`) |
| G2 | Successful batch | All rows in batch marked synced |
| G3 | Partial/failed commit | Committed rows synced; failed ones stay dirty for retry |
| G4 | **Coalescing** — same progress row changed 5× in a window | Firestore write count at flush = **1** for that doc (not 5) |

### 12.2 Integration tests (Room + WorkManager + fake Firestore)

| # | Scenario | Expected |
|---|---|---|
| I1 | Scripted session: 15 screen hops + 4 progress events, then background | Total Firestore writes ≤ target (~5–15), not ~50–110 |
| I2 | Offline during session → go online | Events accrue in Room offline; **one** coalesced flush on reconnect |
| I3 | Progress event → wait window → worker | Exactly one upload; rows synced; second identical event before window = still one upload |
| I4 | Full login flow, second run | First run full pull; second run delta pull only |

### 12.3 Manual / QA device scenarios

| # | Scenario | How to verify | Expected |
|---|---|---|---|
| M1 | Background the app mid-session | Firebase console write count / logcat `AppLifecycleObserver` | One flush burst, then quiet |
| M2 | Force-kill → reopen | Re-open, check data present | No data loss; outbox flushes on next `onStart`/reconnect |
| M3 | Airplane mode during a session, then re-enable | Console writes | Local progress intact; single flush on reconnect |
| M4 | Rotate device during a flush | No crash / no duplicate docs | Stable |
| M5 | Rapid app switch (background↔foreground ×10) | Console writes | Debounce holds — not 10 flushes |
| M6 | Second device sees progress | Open same account on device B | Appears after flush window / backgrounding (not instantly — expected) |
| M7 | Screen analytics routing | GA4 DebugView + Firestore `analytics/{uid}/events` | GA4 receives `screen_view`; Firestore analytics **not** written when `ENABLED = false` |
| M8 | New device login | Fresh install + login | Full progress restore succeeds |
| M9 | Existing login | Re-login same device | Only delta pulled (fewer reads in console) |
| M10 | **Quota before/after** | Same scripted session on old vs new build | ~5–10× fewer writes; reads down once delta/version-gating lands |

### 12.4 Regression / data-integrity (must not break)

| # | Scenario | Expected |
|---|---|---|
| R1 | Progress across kill/restart | No loss; final state correct |
| R2 | Streak increment then background | Persists locally + appears on device B after flush |
| R3 | Garden state | Consistent after deferred sync |
| R4 | Gems / balance | **N/A today** — gems are Room-only (`GamificationRepository` updates the local profile; no real-time Firestore write). If a profile sync is added later, route it through the outbox |
| R5 | Simulation daily blob | Still flushed on `onStop` (existing behaviour preserved) |
| R6 | `ENABLED` toggled back to `true` | Screens sync to Firestore again (flag fully controls behaviour) |
| R7 | Trial completion (sim/study/math/revision) | Item still marks complete + progress persists after deferred flush |

### 12.5 Edge cases

- Empty-outbox schedule/flush is a strict no-op (zero writes).
- Outbox larger than one batch chunks correctly and marks each chunk synced independently.
- Debounce timer/worker cancelled cleanly on logout (no cross-user upload).
- `lastSync` per-user (namespaced by `studentId`) so switching accounts doesn't leak a delta window.
- Clock skew: `updatedAt` uses a consistent source (device vs server) — pick one and assert conflict resolution still holds.
- Worker constraints: uploads require network; assert they don't run (and don't drop data) when offline.

### 12.6 Acceptance criteria

- **Writes/session** drop from ~50–110 to ~5–15 on the I1 scripted session (target ≥ 5× reduction).
- **Zero** Firestore writes to `analytics/{uid}/events` from screen navigation while `ENABLED = false`, **and** GA4 still records `screen_view`.
- **No** `triggerFullSync()` call remains on progress/streak/reconnect hot paths (grep gate in CI).
- Progress restore issues a **delta** query (`whereGreaterThan`) for returning users.
- No data-integrity regressions across R1–R7.
- Only **one** `firestore_upload` worker exists at a time under rapid events (dedup verified).

---

## 13. Expected optimization / efficiency (quantified)

All numbers below are **modeled estimates** from the verified mechanics (§3), with assumptions stated. Firestore Spark (free) tier reference: **20,000 writes/day** and **50,000 reads/day**, *shared across all users in the project*.

**Session assumptions used:** one "study session" ≈ 15 screen navigations + 4 completion milestones (sim/study/math/revision) + occasional reconnects. A returning user's login restore currently reads the full progress subcollection (~150 records, growing) and the app pulls the full Concept catalog (~300 docs) roughly daily.

### 13.1 Writes per session (highest confidence)

| Source | Today | After | Why |
|---|---:|---:|---|
| Screen analytics (entry + exit) | ~30 (15 × 2) | **0** | Screens → GA4 only; no Firestore write |
| Progress/streak milestones (`triggerFullSync` fan-out) | ~20–80 (4 × 5–20) | ~5–10 | Coalesced into **one** batched flush on background |
| Reconnect-triggered full syncs | 0–N (variable) | ~0 | "Schedule-if-dirty" instead of "sync-on-every-reconnect" |
| **Total** | **~50–110** | **~5–15** | **≈ 5–10× fewer writes (~80–90% reduction)** |

Note: the "today" milestone figure is conservative — a **streak** event is a *triple* write (§3.2) and **login** is its own burst (§3.6), so heavy sessions/logins sit at the top of (or above) the range, which only increases the reduction factor.

The single biggest, cheapest win is the **screen-analytics gate**: it removes ~30 writes/session (~40–55% of write volume) at near-zero risk and a few lines of code. Coalescing removes most of the rest.

### 13.2 Reads (high impact; slightly lower confidence — catalog cadence not fully traced)

| Source | Today | After | Reduction |
|---|---|---|---|
| Progress restore on login | Full subcollection — `O(all progress)`, grows unbounded (~150+) | Delta `where updatedAt > lastSync` — `O(changed)` (single digits) | **~90–95%** for returning users |
| Concept catalog | Full `Concept.get()` ~daily — `O(catalog)` (~300) | Version-gated — ~0 on unchanged days | **~100%** on unchanged days |
| Users allowlist | ~1 / 5 min / user (cached) | Unchanged | — |

Reads can actually drop *more* than writes for returning users, because the two dominant read sources (daily full catalog + full restore) collapse to near-constant.

### 13.3 System level — how many more users the free tier supports

| Bound | Today (capacity) | After | Headroom gain |
|---|---:|---:|---:|
| **Writes** (20k/day ÷ writes-per-session) | ~250–400 sessions/day (@ ~50–80) | ~1,300–4,000 sessions/day (@ ~5–15) | **~5–8×** |
| **Reads** (50k/day, returning-user login+catalog) | ~100–200 users/day (@ ~250–450 reads) | ~2,000–5,000 users/day (@ ~10–25) | **~10–40×** (dominated by catalog/restore) |

Interpretation: the app is currently **write-bound *and* read-bound** at a few hundred active users/day on the free tier. After the changes it becomes bound in the **low thousands**, i.e. roughly a **5–10× lift on writes** and a **larger, catalog-dependent lift on reads**.

### 13.4 Effort vs impact (where the efficiency comes from)

| Change | Effort | Write impact | Read impact | Confidence |
|---|---|---|---|---|
| Gate screen sync (+ GA4 `screen_view`) | XS (few lines) | **High** (~30/session gone) | — | **High** |
| Debounced upload + `onStop` full flush; drop `triggerFullSync` on hot paths | S | **High** (coalesces the rest) | — | **High** |
| Fix one-time worker unique name | XS | Low (prevents pile-ups) | — | High |
| Delta progress restore (`whereGreaterThan`) | M | — | **High** | Med–High |
| Version-gate Concept catalog pull | M | — | **High** | Med (cadence unverified) |

### 13.5 Caveats

- These are **modeled**, not measured. Validate with the §12.3 M10 before/after write count in the Firebase console on a scripted session — that is the real acceptance number.
- The write figures are high-confidence (mechanics verified). The read figures depend on actual progress-record counts per student and the true catalog pull cadence (flagged as an open item in §11).
- "Efficiency" here = **quota consumption**, not device performance. UI latency is already decoupled from Firestore (Room-first), so the user-visible speed is unchanged; the win is headroom + cost, plus fixing the login/"Service busy" failures that came from hitting the shared ceiling.

---

*File references reflect the repository state at the time of review; line numbers may drift as the code changes.*
