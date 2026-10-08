# Cursor verify — crash fix: NumberFormatException on class dropdown (Play v13 rejection)

**Date:** 2026-08-26 · **Play rejection:** v13 (1.0.11) "crashes after opening" · **Android Vitals crash:**
`UserDetailEntryScreenKt.UserDetailEntryScreen$lambda$... java.lang.NumberFormatException` (seen on v10/1.0.8;
same code shipped in v13).

## Root cause
The class-picker mapped the **selected label** back to a number by stripping an English prefix and calling
`.toInt()`:

```kotlin
// shipped v13 (f7de743), UserDetailEntryScreen.kt
selectedClass = selectedString.removePrefix("Class ").trim().toInt()
```

The options are localized: `class_format` = `Class %d` (en) / `ತರಗತಿ %d` (kn). In **Kannada** the label is
`ತರಗತಿ 7`, so `removePrefix("Class ")` matches nothing and `.toInt()` runs on `"ತರಗತಿ 7"` →
**NumberFormatException → crash**. English works, Kannada crashes — exactly the Vitals signature. A reviewer
in Kannada (or any KN user) hits it right after opening, on the user-detail entry screen.

## Fix (locale-proof, cannot throw)
Map the label to its class number by **option index**, never by parsing text. Digit-scrape kept only as a
fallback. Also fixed `selectedValue` to use the localized option instead of a hardcoded `"Class N"`.

Two files:
- `ui/screens/login/UserDetailEntryScreen.kt` — the actual crash. (HEAD had already softened `.toInt()` to
  `Regex("\\d+")...toIntOrNull()`, which stops the crash but silently fails if `%d` renders as Kannada
  digits; the index approach fixes both.)
- `ui/screens/setting/components/EditProfileSection.kt` — **same `.removePrefix("Class ").toInt()` pattern,
  still live at HEAD.** Its options are currently hardcoded English so it isn't crashing today, but it's the
  same latent bug (and would crash the moment those options are localized). Hardened identically.

```kotlin
val idx = classOptions.indexOf(selectedString)
selectedClass = if (idx >= 0) idx + 1
    else Regex("\\d+").find(selectedString)?.value?.toIntOrNull() ?: selectedClass
```

## Verify
- Simulated: EN `"Class 7"` → 7, KN `"ತರಗತಿ 7"` → 7, KN-digit label (in options) → correct via index; old
  `.toInt()` on `"ತರಗತಿ 7"` raises NumberFormatException (reproduced).
- App-wide grep for `(removePrefix|substring|replace|trim)(...).toInt()/.toFloat()` → **no other instances**.
- Both files brace/paren balanced. (Could not run Gradle here — please `./gradlew assembleDebug` / lint on
  your machine.)
- Other `.toInt()` hits in UI (VoiceWaveAnimation, SimulationIntroTtsSanitizer, ExamPlanSetupPanel slider,
  InAppUpdate progress) are on numeric types (Float/Long/Double), not string parsing → not NFE sources.

## Ships in
v14 / 1.0.12 (the resubmission). This is the fix for the v13 crash rejection.

## Note on the Vitals data
Vitals shows the crash on **v10 (1.0.8)**; v13 shows "data unavailable" (few installs). The rejection likely
came from Google's review/pre-launch device hitting the same KN path. Root cause and fix are the same either
way. If you have the Pre-launch report stack trace for v13, worth a glance to confirm it's this and not a
second issue.
