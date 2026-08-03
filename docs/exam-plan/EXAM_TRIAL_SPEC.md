# Exam trial — product spec (phased)

**Status:** Spec locked for implementation · **Last updated:** 2026-07-25 (rev 2)  
**Parent doc:** [GAMIFICATION_INTEGRATION_PLAN.md](../GAMIFICATION_INTEGRATION_PLAN.md) §8 Exam planner  
**Migrations:** [DATA_MIGRATIONS.md](./DATA_MIGRATIONS.md) — trial queue upgrades on app update  
**Code today:** `ExamPlanGenerator`, `PlanDayActions`, `PlanTrail`, `HomeTutorBubble` (display-only)

This document captures agreed behavior for the **exam trial** flow: day task lists, completion rules, rollover, deadline handling, celebration UX, and plan feasibility. It supersedes earlier draft ideas that used item locking and in-session celebration.

---

## Phase map

| Phase | Scope | Status |
|-------|--------|--------|
| **A — Trial shell** | Day trial screen (vertical list), tutor bubble entry, chapter-block ordering, no locks | Planned |
| **B — Completion (v1)** | Sim agent → **GE node**; sim URL → **7 clicks**; study → **7 turns**; deferred celebration on back/quit | Planned |
| **C — Rollover & deadline** | Carry incomplete tasks forward; delete plan at deadline + feed entry; no auto-regenerate | Planned |
| **D — Plan feasibility (§4)** | Validate schedule vs exam date; surface inconsistencies at setup | Planned |
| **E — Meaningful interaction count** | Refine what counts as a click/turn beyond raw counters | **Deferred — next phase** |

---

## 1. Task ordering (no locks)

### Per chapter — dynamic counts (not fixed slots)

Task count is **not** a fixed ~10 per type. For each **chapter** on a plan day, derive items from syllabus data:

| Source | Trial items |
|--------|-------------|
| **Simulation URLs** registered for concepts in the chapter | One `SIM_URL` item per URL (concept order) |
| **Study session templates** for concepts in the chapter | One `STUDY` item per template (concept order) |
| **Revision** | One `REVISION` item per concept — **REVISE days only** (see below) |

Implementation reads existing Room / NCERT metadata (same sources as concept list and agent entry points). Counts vary by chapter.

### Order within a day

For each **chapter** assigned to that day, list tasks in this order:

1. All **simulation** items for that chapter (URLs + simulation-agent entries as applicable, in concept order)
2. All **study** items for that chapter (from session templates, in concept order)

Then repeat for the next chapter on the same day.

**Example (Day 3 — Ch. 5 has 4 sim URLs + 3 study templates, Ch. 6 has 6 + 5):**

```
Ch 5 · Sim 1 … Sim 4
Ch 5 · Study 1 … Study 3
Ch 6 · Sim 1 … Sim 6
Ch 6 · Study 1 … Study 5
```

### Revision — never in lesson trial list

- **Revision agent** appears **only** on planner **`REVISE`** days.
- Scoped to **that day's concepts** only.
- **Never** mixed into `LESSON` day trial items.

### No locking

- **Do not lock** any agent or subsequent list item.
- User may open any item in any order from the day trial list.
- Completion state and rollover logic still apply; UI must not block navigation.

---

## 2. Completion rules (Phase B)

**v1 thresholds (agreed):** simulation agent → **GE node**; study agent → **first 7 user turns**; simulation URL → **7 clicks**. No in-session celebration.

### Deferred celebration (all agents)

When an item reaches its completion threshold **inside** the agent/sim screen:

- Mark progress internally (Room / `ProgressEventTracker`).
- **Do not** show celebration, XP burst, or trial-advance UI **while still on that screen**.

Celebration runs only when the user **quits the screen or navigates back** to the day trial list (or parent shell).

### Partial completion persistence

If the user reaches **GE** (sim agent) or **7 turns / 7 clicks** but **force-kills** or backgrounds before navigating back:

- Internal **`DONE`** state **must still persist** (write on threshold, not on back).
- Deferred **celebration + 3s advance** runs on **next** entry to the trial shell when a completed-but-uncelebrated item is detected.

### Simulation agent

- **Done** when the agentic flow reaches the **`GE` node** (goal/end node in the agent graph).
- Until GE: status = `PENDING` / `IN_PROGRESS`.
- On GE (persist immediately) + back/quit → deferred celebration (see §3).

### Simulation URL (7 clicks)

- **Done** after **7** interactions (raw click count for v1).
- Same persist-on-threshold + deferred celebration on back/quit.
- **Phase E** will refine “meaningful” interaction counting — do not over-engineer v1.

### Study agent (first 7 turns)

- **Done** after **7 user turns** (first 7 user messages sent in session).
- Same persist-on-threshold + deferred celebration on back/quit.

### Pending

- Any item below threshold = **`PENDING`** / **`IN_PROGRESS`** (not `DONE`).
- Incomplete items participate in rollover (§5).

---

## 3. Celebration & trial advance

When returning from a **newly completed** trial item (or on next trial open if completion was persisted without celebration — see §2):

1. Show a **3-second happy-avatar** welcome screen.
   - Copy welcomes user to the **first** or **next** item in **today's trial** (name + type).
2. After 3 seconds, **auto-launch** that next item (sim or study flow).

Notes:

- **Agreed:** 3s auto-launch is default behavior (optional skip control is polish).
- If the app is **backgrounded** during the 3s window, **pause** the countdown and auto-launch until the app is foreground again.
- If no next item (day complete), happy-avatar should reflect completion of today's trial instead of launching.
- Applies to trial sequence orchestration; does not replace deferred completion rule **inside** the agent screen.

### Entry points (same orchestrator)

- Plan trail **day tap** → day trial list → celebration chain.
- **Today's** plan node / hero focus → same.
- **Home tutor bubble** (make clickable) → opens **today's trial** with same behavior.

---

## 4. Plan generation — feasibility vs exam date

Before persisting a plan (`ExamPlanSetupPanel` / `createCustomPlan`):

1. **Estimate** total trial workload:
   - Selected chapters → count simulation URLs + study session templates per chapter (dynamic, not fixed slots).
   - Add revision items for planned REVISE days.
   - Respect **daily minutes** budget when packing lesson days.
2. **Compare** required lesson + revise + mock days to **calendar days until exam date**.
3. **Highlight inconsistencies** in setup UI (non-blocking warnings or blocking errors — product choice at implement time):
   - Not enough days to cover selected chapters at chosen daily minutes.
   - Exam date in the past.
   - Revision/mock/exam days cannot fit before exam date.
   - Carry-over backlog would exceed remaining days (post-launch enhancement).

Do **not** auto-shrink chapter selection without user confirmation.

---

## 5. Rollover & deadline

### Daily rollover

At day boundary (Asia/Kolkata, aligned with existing `ExamPlanGenerator`):

- Collect all trial items with `status != DONE` from the previous calendar day.
- **Prepend** them to the next day's trial queue (before that day's newly scheduled items).
- Mark previous day **`PARTIAL`** (new status), not `DONE`.

### Deadline (exam date / final plan day)

When the plan reaches its **deadline** and work remains incomplete:

1. **Delete** the active plan (`exam_plan` + `exam_plan_day` rows).
2. **Post a self-only updates / feed entry** flagging **incomplete study** (and optionally incomplete sims).
   - Visible only to the student (not friends-visible in v1).
   - Reuse `friend_feed_item` / local feed with `visibility = SELF` or equivalent; `FirebaseRepository.publishFriendFeed` only if self-scoped.
3. **Do not auto-regenerate** a new plan — user must create a new plan explicitly.

---

## 6. Deferred to next phase (Phase E)

### Meaningful interaction count

- Replace or augment raw **7-click** simulation counting with **meaningful interaction** rules (e.g. exclude accidental taps, debounce, required interaction types).
- Document thresholds per simulation type when spec is written.
- **Do not implement in Phase B** beyond simple counters.

---

## 7. Data model sketch (implementation hint)

```
PlanTrialItem(
  planDayId,
  conceptId,
  chapterId,
  kind: SIM_URL | SIM_AGENT | STUDY | REVISION,
  sourceId,           // simulation URL id or study template id
  sequenceIndex,
  requiredCount,      // 7 for SIM_URL / STUDY; GE=1 for SIM_AGENT; 1 for REVISION
  completedCount,
  status: PENDING | IN_PROGRESS | DONE,
  celebrated: Boolean, // false until 3s happy-avatar shown after completion
  carriedFromDayIndex: Int?,  // rollover
)
```

- Item list built at plan generation / day materialization by querying **simulation URLs** and **study session templates** per chapter.

- `REVISE` days: items with `kind = REVISION` only.
- `LESSON` days: `SIM_*` and `STUDY` only.

---

## 8. Out of scope / explicit non-goals

- Locking trial items or gating navigation.
- In-session celebration before back/quit.
- Auto-regenerating plan after deadline deletion.
- Mixing revision into lesson day lists.

---

## 9. Related files (when implementing)

| Area | Files |
|------|--------|
| Plan generation | `domain/examplan/ExamPlanGenerator.kt`, `ExamPlanRepository.kt`, `ExamPlanSetupPanel.kt` |
| Day navigation | `PlanDayActions.kt`, `PlanTrail.kt`, `PlanOverviewScreen.kt` |
| Completion | `ProgressEventTracker.kt`, `SimulationAgentViewModel.kt`, `ChatViewModel.kt`, `InteractionTracker.kt` |
| Home / tutor | `HomeRails.kt` (`HomeTutorBubble`), `GamifiedHomeRoute.kt`, `HomeScreen.kt` |
| Feed on expiry | `FirebaseRepository.kt`, `friend_feed_item`, `FriendUiMapper.kt` |

---

## Changelog

| Date | Change |
|------|--------|
| 2026-07-25 | Initial spec: no locks, GE node + 7-click/7-turn, deferred celebration, 3s happy-avatar advance, revision on REVISE days only, rollover + deadline delete + feed, feasibility §4, Phase E meaningful interactions deferred |
| 2026-07-25 | Rev 2: dynamic task counts from sim URLs + study templates per chapter; self-only feed on expiry; persist DONE on GE/7 without back; pause 3s auto-launch when backgrounded; partial GE agreed |
