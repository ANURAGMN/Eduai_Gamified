# Release checklist — v1.0.14 (versionCode 16)

**Status: LIVE on Google Play** (2026-10-30).

Ship gates for the Play upload after `1.0.13` (vc15). Status: ☑ done · ◻ deferred / open.

**Tip shipped:** `gamified/main` — whiteboard blank-board fix + home search (`575ecdb` / `28ade34`) + version bump `de07f80`.  
**Do not** resubmit versionCode **13**.

---

## 1. Blockers — must be in the build

- ☑ **Version bumped** — `versionCode = 16`, `versionName = "1.0.14"`.
- ☑ **Whiteboard Visual Board blank fix** — AndroidSVG bitmap path, viewBox sizing, `orient` sanitize.
- ☑ **Timed scene reveal** + **Show all**.
- ☑ **Home topic search** — Notes / Whiteboard / Ask doubt.
- ☑ **Prior cut (1.0.13)** — whiteboard tutor, Notes, Doubts, bite-based chapter trials.
- ☑ **Commit ship set** — `de07f80` on `gamified/main`.
- ☑ **Build release AAB** — 2026-10-25 (~24.8 MB).
- ☑ **Install release APK on device** — sideload smoke 2026-10-25+.
- ☑ **Production as 16 / 1.0.14** — **live on Play** 2026-10-30.

## 2. Device QA (release build)

- ☑ Whiteboard / search smoke covered enough to ship (further regression as needed via Vitals).
- ◻ Kannada class pick — keep as ongoing regression when touching profile/locale.
- ◻ Notes / Doubts / chapter trial — ongoing.

## 3. Play Console

- ☑ Upload / Closed testing / Pre-launch as needed.
- ☑ **Promote to Production as 16 / 1.0.14** — live 2026-10-30.
- ☑ Release notes from `docs/store-listing/release-notes-1.0.14.txt`.

## 4. Deploy re-checks (ongoing)

- ⚠ Pages `guide-coach.js` / coach coverage / KN sim URLs — re-check when changing coach content.

## 5. Ads / Analytics (carry-over)

- ◻ GA4: mark `sign_up` as Key event (if not already).
- ◻ Ads: import `sign_up`; **hold** bid switch until volume supports it.

## 6. Explicitly not in this cut

- `feature/notif-reprompt` (rebase + QA next).
- Closing stale PR #1 / leftover whiteboard PR #2.
- Math coach authoring backlog.

---

*Historical checklist for 1.0.14. Next ship: new versionCode + new checklist.*
