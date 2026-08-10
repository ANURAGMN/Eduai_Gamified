# App data migrations

Derived app data (trial queues, progress backfills, computed schedules) lives outside Room schema versioning. This doc describes how to ship **migratable** changes the same way we ship Room `Migration` objects.

## Two migration layers

| Layer | What it covers | Where |
|-------|----------------|--------|
| **Room schema** | Tables, columns, indexes | `DatabaseMigrations.kt`, bump DB version in `EduAiDatabase` |
| **App data** | Rebuilt queues, backfills, one-off fixes | `domain/migration/`, `AppDataMigrationRunner` |

Room handles structure; app-data migrations handle **content** that is computed from syllabus + plan rules.

## Architecture

```
AppDataMigrationVersions.CURRENT   ← single source of truth for latest version
AppDataMigration (interface)       ← one step: toVersion + migrate()
AppDataMigrationRunner             ← runs pending steps sequentially (like Room)
AppDataMigrationModule             ← registers all steps via Dagger @IntoSet
```

Stored version: `SharedPreferenceUtils.getAppDataMigrationVersion()` (`app_data_migration_version`).

Legacy backfill: old `trial_materializer_version` and `legacy_progress_migration_v1` flags are mapped into the unified version on first run.

## When to add a migration

Add a new migration when a release changes:

- Trial item ordering or materializer rules (`PlanTrialMaterializer`)
- Progress normalization that must run on upgrade
- Exam-plan derived data that cannot be fixed lazily per screen

Do **not** add a migration for pure UI changes or server-only data that re-syncs automatically.

## How to add migration v5 (checklist)

1. **Implement the feature** (e.g. new trial pacing rule in `PlanTrialMaterializer`).

2. **Add a version constant** in `AppDataMigrationVersions.kt`:
   ```kotlin
   const val TRIAL_SCHEDULE_MY_FEATURE = 5
   const val CURRENT = TRIAL_SCHEDULE_MY_FEATURE
   ```

3. **Create a migration class** in `domain/migration/migrations/`:
   - Trial queue rebuilds → reuse `RematerializeTrialScheduleMigration(5)` or call `planTrialRepository.materializeAllPlanDays` from a dedicated class.
   - Other backfills → new class implementing `AppDataMigration`.

4. **Register in `AppDataMigrationModule`** with `@Provides @IntoSet`.

5. **Verify chain**: `AppDataMigrationRunner` init requires contiguous versions `1..CURRENT` with no gaps.

6. **Test**: unit test the migration logic; manually upgrade from a DB dump at v4.

7. **Document** the version row in the table below.

## Version history

| Version | Constant | What it does |
|--------:|----------|--------------|
| 1 | `LEGACY_PROGRESS_LANGUAGE` | Normalize legacy progress language codes |
| 2 | `TRIAL_SCHEDULE_PER_CONCEPT` | Rebuild trial queues (per-concept SIM → STUDY → agent) |
| 3 | `TRIAL_SCHEDULE_BATCHED` | Rebuild trial queues (batched sims before study) |
| 4 | `TRIAL_SCHEDULE_INTERLEAVED` | Rebuild trial queues (interleaved partitions + revise agents) |
| 5 | `TRIAL_SCHEDULE_THREE_SIMS_PER_STUDY` | Three sims per study slot across days; leftover studies stacked at end |
| 6 | `TRIAL_SCHEDULE_TWO_SIMS_PER_STUDY` | Two sims per study/math slot; chapter trials: interleaved → revision → sim agents last |

## When migrations run

1. **App cold start** — `EduAiApplication.runAppDataMigrations()` (logged-in user)
2. **Home / plan refresh** — `ExamPlanRepository.refreshDayStatuses()` → `PlanTrialRepository.ensureTrialScheduleCurrent()`
3. **Trial screen open** — `PlanTrialRepository.ensureTrialItemsForDay()`

Failed migrations **do not** advance the version; they retry on next launch.

## Progress preservation

Trial rematerialization merges existing progress by stable key:

```
kind | conceptId | sourceId
```

New items start `PENDING`; removed items are dropped.

## Related code

   - `PlanTrialRepository.materializeAllPlanDays()` — rebuild all plan days for a student
   - `PlanTrialRepository.mergeTrialProgress()` — progress merge during sync
   - `AppDataMigrationRunner` — orchestrates all steps
   - `docs/exam-plan/EXAM_TRIAL_SPEC.md` — trial product rules
