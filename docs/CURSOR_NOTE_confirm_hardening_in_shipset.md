# Confirm: two hardening edits are in the vc14 ship set (`b66f5c0`)

**Date:** 2026-10-08 · **Status:** ✅ Closed — both edits confirmed in `b66f5c0`, nothing uncommitted.

| File | Change | In `b66f5c0` |
|------|--------|--------------|
| `app/src/main/java/.../ui/screens/chatbotscreen/utility/GraphRenderLogic.kt` | `graphData.nodes.first()` → `rootNodes.ifEmpty { graphData.nodes.take(1) }` (empty-graph `NoSuchElementException` guard) | Yes |
| `app/proguard-rules.pro` | `-keepclassmembers enum` for `TutorCharacter` and `NotificationEvalTrigger` | Yes |

## Verification

- `git show --stat b66f5c0` lists both files; `git diff HEAD` on both is empty.
- `GraphRenderLogic` compiled in the `testDebugUnitTest` run on 2026-10-08.
- ProGuard rules only take effect in release — covered by the `bundleRelease` + release smoke gate.

## Note on the enum keeps

Defense-in-depth rather than a confirmed bug fix. R8's default rules keep enum `values()` / `valueOf`, and
`valueOf` matches on the constant name string, which R8 does not normally rewrite. Both call sites already
use `runCatching`. Treat a release-only fallback as unproven unless observed on the release build.

Tracked in `docs/PENDING_BEFORE_NEXT_RELEASE.md` §9.
