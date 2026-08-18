# Cursor: rebuild + verify — subject-row chapter count + % ring

All **uncommitted**; pull latest first. **Not compiled here** — clean-build and run. On device the count/ring showed **nothing**, which most likely means the app wasn't rebuilt from this code (app uses `implementation(project(":ui-kit"))`, so source edits *do* compile in — a stale APK / un-recompiled `:ui-kit` is the prime suspect).

## What changed (Home "Subjects" rows → "Option B" list rows)
- `ui-kit .../components/HomeRails.kt` — `SubjectsRail` is now full-width **`SubjectRow`** list (icon chip · name + "N chapters" · **`SubjectProgressRing`** · chevron), not the old 110dp square `HorizontalRail`. `SubjectTile` gained `subtitle: String?` and `progress: Float?`. Ring = Canvas, `colors.border` track + role-`fg` arc + centred "N%", shown only when `progress != null`. All theme tokens (dark-mode safe).
- `app .../data/local/dao/ChapterDao.kt` — `getChapterCountsBySubject()` + reactive `getChapterCountsBySubjectFlow(): Flow<List<SubjectChapterCount>>`; `SubjectChapterCount(subjectId, chapterCount)` projection.
- `app .../data/local/dao/ChapterAgentProgressDao.kt` — `getCompletedChapterCountsBySubject(studentId, language, appName, completedStatus="COMPLETED")` (grouped COMPLETED count, joined to `chapters`).
- `app .../ui/screens/home/viewmodel/HomeViewModel.kt` — injected `ChapterAgentProgressDao`; `chapterCounts` (from the reactive flow, via `observeChapterCounts()`) + `completedChapterCounts` (`refreshCompletedChapterCounts()`). All local Room, **no Firebase**.
- `app .../ui/screens/home/GamifiedHomeMapper.kt` — `map(...)` gained `chapterCountsBySubject` + `completedChapterCountsBySubject`; `mapSubjectTiles` sets subtitle ("N chapters", localized via `HomeCopy.chapterCount`) + `progress = completed/total`. **Both real and `defaultSubjectTiles` (Math/Science fallback) now get counts** — the fallback missing them was the original "nothing shows" bug.
- `app .../ui/screens/home/GamifiedHomeRoute.kt` — collects both count maps, adds them to the `remember` keys, passes them into `map(...)`.
- `app .../utils/HomeCopy.kt` — `chapterCount(lang, n)` → "N chapters" / "1 chapter" (+ Kannada).

## Verify
1. **Clean build so `:ui-kit` recompiles:** `./gradlew clean :app:assembleDebug`. Install the APK you just built (not a previously installed one).
2. `adb logcat -s SubjectRowDBG` — expect:
   - `total chapters by subject = {…}`
   - `completed chapters by subject = {…}` (or `completed skipped — blank userId`)
   - `tiles(source=available|default) = Math[sub=…, prog=…], Science[…]`
3. Read it:
   - **No `SubjectRowDBG` lines** → app not running this code (stale build / wrong branch).
   - `tiles = …[sub=null, prog=null]` → local `chapters` table empty (content sync issue, not UI).
   - `tiles = …[sub=14 chapters, prog=0.2]` but no rows on screen → `:ui-kit` not recompiled.

## Cleanup after confirming
- Remove the temporary `SubjectRowDBG` logs in `GamifiedHomeMapper.mapSubjectTiles` and `HomeViewModel.observeChapterCounts()/refreshCompletedChapterCounts()`.
- Unused `ChapterDao.getChapterCountsBySubject()` (non-flow) can stay or be dropped.
