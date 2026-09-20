# FORGE — Project Changelog (CHANGELOG.md)

All notable changes to the FORGE platform will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.4.0] - Phase 2.1 & Phase 3: Popular Exercise Layer, Navigation Fix, Liquid Glass Redesign, Adaptive Progressive Overload & Coaching Extensions - 2026-09-20

### Added
- **Curated Popular Exercise Layer (Part A):**
  - Added `is_popular: Boolean`, `popularity_rank: Int`, and `is_favorite: Boolean` to `exercises` table via non-destructive migration `MIGRATION_3_4` (Database Version 4).
  - Seeded 44 curated core popular movements across Chest, Back, Shoulders, Biceps, Triceps, Legs, and Core.
  - Segmented tab bar in Exercise Library and Active Workout picker (`Popular`, `Recent`, `Favorites`, `Browse All`) so users are never confronted with 876 items by default.
  - Instant FTS search retains seamless access to the entire 876 canonical exercise database.
- **Root Navigation Architecture Fix & Automated Tests (Part B):**
  - Fixed bottom bar and back navigation using `popUpTo(navController.graph.findStartDestination().id) { saveState = true }`, `launchSingleTop = true`, and `restoreState = true`.
  - Added workout minimize "←" button returning safely to Command Center Home while keeping session active.
  - Comprehensive automated test suite `NavigationFlowTest` covering all 9 critical navigation routes and transitions.
- **Liquid Glass Visual Redesign (Part C):**
  - Obsidian (`0xFF090A0D`), titanium typography (`0xFFF8FAFC`), translucent glass surfaces (`0x14FFFFFF`), hairline translucent borders (`0x1FFFFFFF`), and emerald accents (`0xFF10B981`).
  - Reusable `Modifier.liquidGlass(...)` design system modifier.
  - Command Center Home with Today's session hero card, streak counter, weekly volume, PR cards, and quick actions.
  - Glass bottom navigation bar with active spring pill indicator.
- **Deterministic Adaptive Progressive Overload Engine (Part D):**
  - `ProgressionEngine`: Double progression (load vs reps vs RIR), equipment-aware step increments, fatigue & rep-collapse detection, multi-session deloads.
  - Epley formula 1RM calculations (`1RM = weight * (1 + reps / 30.0)`).
  - Real-time PR detection across max weight, max reps, estimated 1RM, and set volume.
  - `ProgressViewModel` and `ProgressScreen`: Connected to Room to surface lifetime workout count, total tonnage, training hours, PR badges, and session volume logs.
- **Gym Utility Extensions (Part E):**
  - **YouTube Form Reference:** "Watch Form" intent launcher with verified YouTube video IDs and web browser fallback.
  - **Timestamp Rest Timer Engine:** Epoch timestamp countdown (`restStartedAt`, `restDurationSeconds`, `restEndsAt`) surviving process death and screen lock, with dynamic `+30s` / `-15s` / `SKIP` controls.
  - **Voice Coach Engine:** Android TTS delivery and transient countdown beeps at 10s and 5-4-3-2-1s with polite transient audio focus ducking (`AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`).
  - **Health Connect Activity Integration:** `HealthConnectRepository` querying daily steps and active burn calories with graceful degradation if unavailable or permissions are denied.
  - **Nutrition & Macro Engine:** Mifflin-St Jeor TDEE calculation, target macro distributions, and rolling 7-14 day weight trend feedback loop.
- **Automated Verification:**
  - `ProgressionEngineTest`, `RestTimerEngineTest`, `NutritionEngineTest`, `NavigationFlowTest`, `DatabaseMigrationTest`, `ActiveWorkoutFlowTest`: 100% test pass rate.
  - Debug APK built and assembled (10MB).

---

## [0.3.0] - Phase 2: Exercise Ecosystem, Database Seeding, Search & Exercise Library - 2026-09-20


### Added
- **Normalized Relational Schema (Database Version 3):**
  - Expanded `exercises` entity with source provenance fields (`source`, `source_id`, `source_category`, `source_force`, `source_level`, `source_mechanic`, `source_equipment`, `forge_movement_pattern`, `forge_exercise_family_id`, `search_tokens`, `license`).
  - Added expandable `muscles` taxonomy table (22 muscle groups across Core, Legs, Arms, Chest, Back, Shoulders, Neck).
  - Added normalized `exercise_muscles` junction table with `role` (`PRIMARY`, `SECONDARY`, `STABILIZER`) and `is_forge_derived` provenance flags.
  - Added `equipment` taxonomy table (12 equipment categories).
  - Added `exercise_equipment` junction table with `is_primary` flag.
  - Added `exercise_attributes` table for flexible characteristics (`BILATERAL`, `UNILATERAL`, `INCLINE`, `DECLINE`, `DEFICIT`, `CLOSE_GRIP`, etc.).
  - Added `exercise_families` and `exercise_family_members` tables for multi-variant exercise groupings (e.g. Bench Press variations, Squat variations).
  - Normalized `exercise_aliases` table with explicit `is_forge_derived` flag and foreign key cascade deletion.
- **Architectural Separation of FTS and Relational Queries:**
  - Upgraded SQLite FTS4 virtual table `exercises_fts` over `(name, canonical_name, forge_movement_pattern, search_tokens, instructions)` with unicode61 tokenizer.
  - Separated FTS candidate lookup from relational SQL filtering (`searchAndFilterExercises`): text search candidate IDs are queried via FTS/aliases, and exact relational filters (`muscleId`, `equipmentId`, `movementPattern`, `experienceLevel`, `familyId`) are applied in SQL.
- **Asynchronous Seed Loader & Provenance Ingestion:**
  - Ingested 876 canonical exercises from Free Exercise DB under The Unlicense (zero third-party raster images bundled).
  - Biomechanical movement patterns derived with taxonomy reference from Kinetic (MIT) and explicitly marked `is_forge_derived`.
  - Non-blocking asynchronous ingestion on `Dispatchers.IO` via `DatabaseSeedLoader`. Measured actual seed time on physical Samsung Galaxy S21 FE (SM-G990B2): **1,732 ms** for 876 exercises and all relational mappings.
- **Safe, Non-Destructive Migration (`MIGRATION_2_3`):**
  - Preserves all existing Phase 1 exercises, workout sessions, logged sets, custom exercises, and active workout crash recovery state.
  - Legacy/unknown movement patterns default to `UNKNOWN` (never `ISOLATION`).
  - Explicit CREATE TABLE and indexes for `exercise_aliases`.
- **Athletic Exercise Ecosystem UI:**
  - `ExerciseLibraryScreen`: Instant FTS search bar, filter chips (Muscle, Equipment, Pattern, Level), responsive exercise cards with category and muscle pills, cyan `+ CUSTOM` FAB, and empty/loading states.
  - `ExerciseDetailScreen`: Title, movement pattern, level, mechanic, target muscles breakdown, equipment requirements, step-by-step numbered execution guide, and exercise family variations / siblings.
  - Provenance & License Card: Displays `"Source: Free Exercise DB · The Unlicense"` with full transparency.
  - No fabricated form cues or common mistakes: preserved only when provided by source, empty otherwise.
  - `CustomExerciseDialog`: Dialog allowing users to create custom exercises with taxonomy selectors, saved locally with `is_custom = true`.
  - Integrated into `WorkoutsScreen` (direct exploration) and `ActiveWorkoutScreen` (add exercise picker).
- **Automated Verification:**
  - `DatabaseMigrationTest`: Verified non-destructive migration v1 $\to$ v2 $\to$ v3 preserving active recovery state and sets.
  - `ExerciseRelationalTest`: Verified relational taxonomy, FTS candidate search, relational filtering, and custom exercise coexistence.
  - Physical device verification: Installed, tested, and measured on connected Samsung Galaxy S21 FE 5G (`SM-G990B2`).

---

## [0.2.1] - Quick Start Empty Workout Flow - 2026-09-20

### Fixed
- **Quick Start → Empty Workout Action:**
  - Resolved unhandled empty callback in `WorkoutsScreen` and `ForgeNavGraph`.
  - Tapping **Empty Workout** creates an active session in Room (`name = "Empty Workout"`, `status = "IN_PROGRESS"`) and immediately navigates to `ActiveWorkoutScreen`.
  - Bottom navigation bar is cleanly hidden while an active workout is in progress.

### Added
- **Active Workout Logging Interface (`ActiveWorkoutScreen`):**
  - Session header with title and live elapsed duration ticker.
  - Zero-exercise initial state with "+ Add Exercise" call-to-action.
  - Exercise cards displaying set headers, set rows (Set #, kg weight, reps count, RPE/RIR tracking, and completion checkmark button), and "+ Add Set" button.
  - Modal bottom sheet for exercise search (FTS4-backed) and instant custom exercise creation.
  - Finish Workout and Discard Workout confirmation dialogs.
- **State Management & Domain Logic (`ActiveWorkoutViewModel`):**
  - StateFlow-driven reactive UI state with atomic database updates via Room transaction (`logSetTransaction`).
  - Coroutine-based live elapsed ticker running on `Dispatchers.Default`.
  - Seamless fallback to `getActiveSessionDirect()` ensuring zero race conditions between flow emissions and UI events.
- **Database Schema & Seed Data (Version 2):**
  - Added `name` column to `WorkoutSessionEntity` with non-destructive SQLite migration `MIGRATION_1_2`.
  - Seeded 10 canonical compound/isolation exercises with aliases in `DefaultExercises` on first cold launch.
  - `DatabaseMigrationTest` verifying schema upgrade from v1 to v2.
- **Cold Launch Crash Recovery Integration:**
  - `MainActivity` detects in-progress sessions on cold startup and presents an athletic recovery dialog with "Resume" (navigating to `ActiveWorkoutScreen`) and "Discard" options.
- **Verification Suite:**
  - `ActiveWorkoutFlowTest`: 4 Robolectric unit tests verifying active workout creation with 0 exercises, subsequently adding exercises and sets, session recovery across process recreation, and transition to `COMPLETED` upon finishing. All 10 unit tests pass.

---

## [0.2.0] - Phase 1: Foundation Baseline - 2026-09-20

### Added
- **Build System Toolchain:**
  - Configured Gradle 8.7 with AGP 8.5.2, Kotlin 2.0.21, KSP 2.0.21-1.0.28, and Compose BOM 2024.09.02.
  - Set `minSdk = 26`, `targetSdk = 34`, `compileSdk = 34` with Java 17 compatibility.
- **Semantic Design Tokens & Theme:**
  - `ForgeColors`: Semantic tokens (`Background`, `Surface`, `SurfaceElevated`, `Primary`, `Secondary`, `Accent`, `TextPrimary`, `TextSecondary`, `Success`, `Warning`, `Error`, `Divider`).
  - `ForgeTheme`: CompositionLocal provider for semantic colors with Material 3 dark color scheme mapping and typography tokens.
- **Room Database Architecture (Version 1):**
  - Canonical `ExerciseEntity` and `ExerciseAliasEntity`.
  - SQLite FTS4 virtual table `ExerciseFtsEntity` with unicode61 tokenization and external content mapping.
  - Transactional `WorkoutSessionEntity` and `WorkoutSetEntity` supporting single-source-of-truth crash recovery.
  - `ExerciseDao` with instant FTS subquery prefix and alias search.
  - `WorkoutDao` with atomic transactional set logging and cold launch active session inspection.
  - `Migrations.kt` with explicit versioned migrations and zero destructive fallback.
- **Clean Architecture Repositories:**
  - `ExerciseRepository` and `ExerciseRepositoryImpl`.
  - `WorkoutRepository` and `WorkoutRepositoryImpl`.
- **Navigation & Structural Shells:**
  - `ForgeNavGraph` with bottom navigation connecting 5 root screens: `HomeScreen`, `WorkoutsScreen`, `ProgressScreen`, `JourneyScreen`, and `ProfileScreen`.
  - Interactive cold launch session recovery dialog in `MainActivity`.
  - Compose Previews for empty and active workout states across all shell screens.
- **Test Infrastructure:**
  - `ForgeDatabaseTest`: Database opening, entity insertion, and FTS prefix/alias search.
  - `DatabaseMigrationTest`: Explicit migration configuration validation without destructive fallback.
  - `TransactionalCrashRecoveryTest`: Atomic set logging, state reconstruction, and volume recalculation.

---

## [0.1.1] - Phase 0 Corrections - 2026-09-20

### Changed
- Refined toolchain versions, defensible minSdk 26 basis, FTS4 unicode61 search, transactional crash recovery model, semantic design tokens, and verified public domain exercise licensing.

---

## [0.1.0] - Phase 0: Research & Architecture Baseline - 2026-09-20

### Added
- Initial specification, research, architecture, dependencies, and changelog documents.
