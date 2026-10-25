# Release checklist — v1.0.14 (versionCode 16)

Ship gates for the Play upload after `1.0.13` (vc15). Status: ☑ done · ◻ to do · ⚠ verify on device/console.

**Tip to ship:** `gamified/main` at whiteboard blank-board fix + home search (`575ecdb` and later ship bump).  
**Do not** resubmit versionCode **13**. Prefer uploading **16** (this cut) even if vc15 was only sideloaded.

---

## 1. Blockers — must be in the build

- ☑ **Version bumped** — `versionCode = 16`, `versionName = "1.0.14"` in `app/build.gradle.kts`.
- ☑ **Whiteboard Visual Board blank fix** — AndroidSVG bitmap path, viewBox sizing, `orient` sanitize
  (`WhiteboardSvgBitmapRenderer`); rejects non-SVG fetch payloads in `AgenticAIClient`.
- ☑ **Timed scene reveal** — inject `opacity` on `scene_part` ids from TTS char index; **Show all** skips timing.
- ☑ **Home topic search** — Notes / Whiteboard / Ask doubt actions from home search rail.
- ☑ **Prior cut already in tree (1.0.13)** — whiteboard tutor, Notes, Doubts, bite-based chapter trials.
- ◻ **Commit ship set** — version bump + this checklist + pending/CLAUDE update on `gamified/main`.
- ◻ **Build release AAB** — `./gradlew bundleRelease` (heap now 4g in `gradle.properties`).
  Output: `app/build/outputs/bundle/release/app-release.aab`.
- ◻ **Install release APK/AAB on device** — sideload APK or Closed testing track.
- ⚠ **R8 smoke** — onboarding → home → search → whiteboard (Angle Validity / Photosynthesis) → Notes →
  sim → settings. Confirm Visual Board paints and parts reveal with TTS; **Show all** works.

## 2. Device QA (release build)

- ◻ **Whiteboard** — Science concept with SVG: board not blank; timed reveal; Replay/Stop; voice input.
- ◻ **Home search** — query a topic → open Notes / Whiteboard / Ask doubt; EN + KN strings.
- ◻ **Kannada class pick** — signup + Edit Profile (vc13 rejection class; still regression-gate).
- ◻ **Notes / Doubts** — open from home; PDF if available.
- ◻ **Chapter trial** — 2 whiteboard + 1 sim bite path still navigates.

## 3. Play Console

- ◻ Upload AAB to **Closed testing** (Pre-launch report).
- ◻ Review Pre-launch / vitals (no new crashes).
- ◻ Promote to Production as **16** / **1.0.14**.
- ◻ Paste short release notes (EN + KN) from `docs/store-listing/release-notes-1.0.14.txt`.

## 4. Deploy re-checks (Pages / Firestore — no APK dependency)

- ⚠ `curl -I` GitHub Pages `guide-coach.js` → 200.
- ⚠ `node scripts/check-coach-coverage.js` → Science **0 silent** (EduAI_app).
- ⚠ Kannada sim URL dry-run → 0 pending.

## 5. Ads / Analytics (carry-over — not blocking upload)

- ◻ GA4: mark `sign_up` as Key event (if not already).
- ◻ Ads: import `sign_up` conversion; **hold** bid switch until daily signup volume supports it.

## 6. Explicitly not in this cut

- `feature/notif-reprompt` (rebase + QA after ship).
- Closing stale PR #1 / leftover whiteboard PR #2 (whiteboard already on main).
- Math coach authoring backlog.

---

*When an item finishes, tick it here and mirror status in `docs/PENDING_BEFORE_NEXT_RELEASE.md`.*
