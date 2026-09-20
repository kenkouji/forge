# FORGE — Full Functionality Audit

**Audit Date**: September 20, 2026  
**Target Hardware**: Samsung Galaxy S21 FE (Android 14 / One UI 6, Exynos 2100 / Snapdragon 888)  
**Standard**: Full End-to-End Architectural Integrity (UI ← ViewModel ← Domain Engine ← Repository ← Persistent Data / Android System). Strict Zero Mock Data.

---

## Executive Summary

The visual design system of FORGE (Liquid Glass / Proto aesthetic) is solid and responsive. However, testing on the physical Samsung Galaxy S21 FE revealed that several core flows behaved as visual prototypes or disconnected features rather than an interconnected fitness operating system:
1. **Initialization Hard Gate**: A fresh install jumped directly into Home without enforcing initialization or collecting body measurements (Height, Weight, Age, Sex).
2. **Profile & Settings**: Profile had no mechanism to edit body parameters or training preferences, and settings lacked export, import, and reset wiring.
3. **Home Layout & Navigation**: Unwanted vertical gap existed between Training Snapshot and Activity; Quick Actions pushed child destinations without popping back stack to true Home root upon bottom tab re-selection.
4. **Upcoming Workouts**: Editing upcoming sessions hardcoded target reps (8–12) and dropped user-specified weights, notes, warmups, and drop sets.
5. **Transformation Vault**: Displayed a locked placeholder that blocked access without providing clear Add/Take photo flows for Front, Side, and Back, nor a slideshow for verified milestones.
6. **AI Assistant**: Only supported a few keyword triggers rather than executing typed application tools against Room database state with confirmation prompts for state mutations.
7. **Zero Mock Data & Single Source of Truth**: Progress and History needed explicit expansion of logged sets (`WorkoutSetEntity`), and PR calculations needed to be strictly derived from completed workouts.

---

## Detailed Feature Audits

### 1. Initialization System & Fresh Install Hard Gate
* **FEATURE**: First-Launch Initialization Wizard
* **CURRENT IMPLEMENTATION**: `InitializationWizard.kt`, `InitializationViewModel.kt`, `UserProfileEntity.kt`, `ForgeNavGraph.kt`.
* **DATA SOURCE**: `user_profile` table in Room database.
* **UI SOURCE**: `InitializationWizard.kt`.
* **EXPECTED BEHAVIOR**:
  - Fresh install must hard-gate on `!userProfile.isInitialized`.
  - Collect Identity (Name, Photo URI), Body metrics (Height, Weight, Age, Sex), Goal (10 canonical options), Experience (Beginner, Intermediate, Advanced), Frequency (1–7 days), Training days (with explicit rest days), Session duration (30, 45, 60, 75, 90+ min), Equipment (Gym, Home, Barbell, Dumbbells, Machines, Cables, Bands, Bodyweight, Custom), Existing program text import, Nutrition targets, Notifications setup, Health Connect offer, Transformation password.
  - If user leaves and re-launches app before finishing, resumes at the exact step.
* **ACTUAL BEHAVIOR**:
  - `UserProfileEntity` was missing `heightCm`, `weightKg`, `age`, and `sex` columns.
  - Step 1 only captured name; body stats were skipped.
  - Equipment was single-choice rather than multi-select equipment types.
  - Existing program import didn't offer an explicit "Already have a workout program? YES/NO" branch with edit/apply/discard preview.
* **BUG**: Fresh install lacked complete identity & body telemetry collection, and schema lacked physical body fields.
* **FIX**:
  - Add `height_cm`, `weight_kg`, `age`, `sex` to `user_profile` in Room schema and entity.
  - Rebuild wizard steps: Body Information step (Height, Weight, Age, Sex), Equipment multi-select, Program Import branch with Edit/Apply/Discard, dedicated Permissions Setup step.

---

### 2. Permissions Setup Flow & Graceful Fallback
* **FEATURE**: Dedicated Permissions Setup Screen & Runtime Handlers
* **CURRENT IMPLEMENTATION**: Ad-hoc system permission dialogs during runtime in `HealthConnectRepositoryImpl.kt` and `MainActivity.kt`.
* **DATA SOURCE**: Android System (`ActivityCompat.checkSelfPermission`, `HealthConnectClient.permissionController`).
* **UI SOURCE**: `InitializationWizard.kt` & `PermissionsSetupScreen.kt`.
* **EXPECTED BEHAVIOR**:
  - Dedicated screen during initialization explaining exactly what FORGE requires and why: Health Connect (Steps, Calories, Weight), Notifications (Workout alerts, Streak reminders, Pre/Post workout), Camera (Transformation photos).
  - Just-in-time requests for Camera/Audio when feature is tapped.
  - If denied: FORGE functions completely; shows feature as "Disconnected / Permission Needed" without crashing or fabricating fake values.
* **ACTUAL BEHAVIOR**:
  - Health Connect and Notification permissions were requested without prior contextual rationale.
  - Denials resulted in empty activity cards with no clear indication of permission state.
* **BUG**: Missing dedicated permission rationale onboarding screen and graceful fallback state indicator in UI.
* **FIX**:
  - Implement `PermissionsSetupScreen` step in initialization.
  - Add permission check helpers and disconnected status chips in Activity & Settings.

---

### 3. Profile Management & Athletic Passport
* **FEATURE**: Edit Profile & Reactive Telemetry Propagation
* **CURRENT IMPLEMENTATION**: `ProfileScreen.kt`, `ProfileViewModel.kt`, `UserProfileDao.kt`.
* **DATA SOURCE**: `UserProfileEntity`, `UserNutritionProfileEntity`, `WeightLogEntity`.
* **UI SOURCE**: `ProfileScreen.kt`.
* **EXPECTED BEHAVIOR**:
  - Obvious "EDIT PROFILE" button.
  - Modal sheet or screen allowing edits to Name, Photo, Height, Weight, Age, Goal, Experience, Frequency, Training days, Duration, Equipment.
  - Saving persists immediately to Room and updates dependent calculations (BMR, TDEE, target calories/macros, weight history).
* **ACTUAL BEHAVIOR**:
  - Profile card was read-only with no edit button or sheet.
  - Weight could not be updated from Profile.
* **BUG**: No UI or ViewModel function to modify user profile details.
* **FIX**:
  - Create `EditProfileSheet` in `com.forge.presentation.profile`.
  - Bind updates to `UserProfileDao.insertOrUpdate()` and log new weight to `weight_logs` when weight changes.
  - Recalculate target calories and macros reactively.

---

### 4. Profile Settings Full Suite
* **FEATURE**: Functional Application & Privacy Settings
* **CURRENT IMPLEMENTATION**: `ProfileScreen.kt`, `AppSettingsEntity.kt`, `AppSettingsDao.kt`.
* **DATA SOURCE**: `app_settings` Room table.
* **UI SOURCE**: `ProfileScreen.kt`.
* **EXPECTED BEHAVIOR**:
  - Training preferences: view current program, edit schedule, equipment preferences.
  - Nutrition preferences: target calories, protein, carbs, fat.
  - Health Connect: connection status, sync button, permission status.
  - Notifications: toggles for workout reminders, pre-workout, post-workout, streaks, motivation; each toggle cancels or schedules alarms in `NotificationScheduler`.
  - Privacy: change transformation password, auto-lock toggle.
  - Appearance: reduce motion, particles toggle, haptics toggle.
  - Data: Export JSON, Import JSON, Reset All Data (double confirmation).
* **ACTUAL BEHAVIOR**:
  - Only 4 basic toggles existed (Reminders, Reduce Motion, Particles, Auto-lock).
  - Password change, Export, Import, and Data Reset were completely absent.
* **BUG**: Dead/incomplete settings surface.
* **FIX**:
  - Build out full settings sections in `ProfileScreen` and `ProfileViewModel`.
  - Wire JSON export/import in `ProfileViewModel` and implement two-step destructive `resetAllData()` that deletes Room database contents and clears encrypted vault storage.

---

### 5. Home Screen Visual Hierarchy & Dead Space Bug
* **FEATURE**: Home Dashboard Layout
* **CURRENT IMPLEMENTATION**: `HomeScreen.kt`, `HomeViewModel.kt`.
* **DATA SOURCE**: `TrainingScheduleDao`, `WorkoutDao`, `DailyActivityDao`, `UserProfileDao`.
* **UI SOURCE**: `HomeScreen.kt`.
* **EXPECTED BEHAVIOR**:
  - Exact order:
    1. TODAY (Active workout or scheduled training/rest)
    2. PERSONALIZED ASSISTANT (`[ ✦ Personalized Assistant ]` context card)
    3. TRAINING SNAPSHOT (Streak, weekly sessions, volume, goal)
    4. ACTIVITY (Steps, active kcal, measured vs estimated badge, sync)
    5. PROGRESS / RECENT PR (Latest PR card if one exists; collapses naturally if none)
    6. CONTEXTUAL SECTIONS (Quick actions / Milestones)
  - Zero unexplained blank gaps between sections.
  - Sections collapse naturally when empty.
* **ACTUAL BEHAVIOR**:
  - Assistant was a tiny pill in the top header.
  - Excessive spacing existed between Training Snapshot and Activity due to fixed paddings and uncollapsed layout rows.
  - Recent PR was absent from Home.
* **BUG**: Layout order deviated from specification; excessive vertical space between Training Snapshot and Activity.
* **FIX**:
  - Reorganize `HomeScreen.kt` into the exact specified order.
  - Insert contextual `Personalized Assistant` card between Today and Training Snapshot.
  - Embed dynamic `Recent PR` card that displays the latest PR from `exercise_personal_records` or cleanly collapses if empty.
  - Adjust vertical spacers to 14–16.dp standard.

---

### 6. Home Quick Action Navigation Bug
* **FEATURE**: Bottom Tab & Child Navigation Architecture
* **CURRENT IMPLEMENTATION**: `ForgeNavGraph.kt`, `ForgeBottomBar.kt`, `HomeScreen.kt`.
* **DATA SOURCE**: Android `NavController` back stack.
* **UI SOURCE**: `ForgeBottomBar.kt`.
* **EXPECTED BEHAVIOR**:
  - Sequence: `Home -> Quick Action (e.g. Exercises) -> Workouts -> Home` MUST land at Home ROOT.
  - Quick Actions must not leave stray destinations on the Home back stack.
  - Pressing Home tab always produces Home root.
* **ACTUAL BEHAVIOR**:
  - Navigating to `ExerciseLibrary` from Home pushed `ExerciseLibrary` onto the back stack. When switching to Workouts and then tapping Home, `popUpTo(startDestination)` did not pop intermediate child screens when `restoreState` was used or when navigating without popping up to `Home`.
* **BUG**: Child navigation state retained across tab switches causing user to get trapped on previous screen.
* **FIX**:
  - In `ForgeNavGraph.kt`, configure bottom bar tab navigation with:
    ```kotlin
    popUpTo(ForgeNavDestination.Home.route) {
        saveState = false
        inclusive = false
    }
    launchSingleTop = true
    restoreState = false
    ```
  - Convert Quick Actions on Home to modal bottom sheets or ensure direct navigation pops up to destination cleanly.

---

### 7. Upcoming Workouts Editor & Versioned Templates
* **FEATURE**: Interactive Upcoming Workout Session Editor
* **CURRENT IMPLEMENTATION**: `SessionEditorSheet.kt`, `WorkoutsViewModel.kt`, `WorkoutTemplateDao.kt`.
* **DATA SOURCE**: `workout_templates` and `template_exercises` tables.
* **UI SOURCE**: `WorkoutsScreen.kt` & `SessionEditorSheet.kt`.
* **EXPECTED BEHAVIOR**:
  - Tapping "Edit" on any day in the weekly cycle opens the session editor.
  - User can: Add exercise, Delete exercise, Replace exercise, Reorder exercises, Modify target sets, Target reps (single or range e.g. 10–15), Target weight, Target RIR/RPE, Rest seconds, Warmup flag, Drop-set flag, Notes.
  - Saving persists to `workout_templates` and `template_exercises`, incrementing `version`.
  - Next time user opens that day, the edited version appears.
  - Historical completed workouts are NEVER altered by template edits.
* **ACTUAL BEHAVIOR**:
  - `SessionEditorSheet` only supported sets, reps, and simple delete/reorder.
  - Saving hardcoded `targetRepsMin = 8`, `targetRepsMax = 12` and dropped weights, notes, warmup, and drop-set flags.
* **BUG**: Session editor dropped detailed exercise parameters and hardcoded rep ranges.
* **FIX**:
  - Expand `EditableExerciseItem` and `SessionEditorSheet` to support weight, RIR, warmup, dropset, notes, and exact rep range parsing.
  - Save full entity fields in `WorkoutsViewModel.saveTemplateChanges()`.

---

### 8. Program / Schedule / History Data Model Separation
* **FEATURE**: Historical Immutability vs Upcoming Program
* **CURRENT IMPLEMENTATION**: `WorkoutSessionEntity`, `WorkoutSetEntity`, `WorkoutTemplateEntity`, `TrainingScheduleEntity`.
* **DATA SOURCE**: Room tables `workout_sessions`, `workout_sets`, `workout_templates`, `training_schedule`.
* **UI SOURCE**: `WorkoutsScreen.kt`, `ActiveWorkoutScreen.kt`, `ProgressScreen.kt`.
* **EXPECTED BEHAVIOR**:
  - `TrainingScheduleEntity` links day of week to a `template_id`.
  - Active workout instantiates a new `WorkoutSessionEntity` with independent `WorkoutSetEntity` rows.
  - Editing upcoming template creates or modifies template vN, leaving all past `WorkoutSessionEntity` and `WorkoutSetEntity` rows completely untouched.
* **ACTUAL BEHAVIOR**:
  - Separation is architecturally respected in schema, but template editing needed validation to verify that historical sessions remain immutable.
* **STATUS**: Verified intact; automated test `historicalImmutability_editingTemplateDoesNotAffectPastSessions` passes.

---

### 9. Workout Screen Tabs: Schedule, Recent, and Templates
* **FEATURE**: Functional Recent Sessions & Routine Templates
* **CURRENT IMPLEMENTATION**: `WorkoutsScreen.kt`, `WorkoutsViewModel.kt`.
* **DATA SOURCE**: `workout_sessions` (completed) and `workout_templates`.
* **UI SOURCE**: `WorkoutsScreen.kt`.
* **EXPECTED BEHAVIOR**:
  - Tab selector: `[ Schedule | Recent | Templates ]`.
  - **Recent**: lists actual completed workout sessions with date, volume, exercises. Supports: View, Edit as new template, Duplicate, Start again.
  - **Templates**: lists saved templates. Supports: View, Edit, Duplicate, Start now.
  - No decorative or dead buttons.
* **ACTUAL BEHAVIOR**:
  - Segmented control only had `["Schedule", "Templates"]`.
  - Templates tab was a basic list without Duplicate or View options.
  - "Recent" workouts option was missing.
* **BUG**: Missing Recent tab and missing actions (Duplicate, View) on Templates.
* **FIX**:
  - Update `WorkoutsScreen.kt` segmented control to 3 tabs: Schedule, Recent, Templates.
  - Implement full card actions (Start, View/Edit, Duplicate) for both Recent and Templates.

---

### 10. Workout Text Parser & Multi-Day Program Rebuilding
* **FEATURE**: Multi-Day Routine Text Parser
* **CURRENT IMPLEMENTATION**: `WorkoutTextParser.kt`.
* **DATA SOURCE**: Pure deterministic Kotlin engine (`WorkoutTextParser.kt`).
* **UI SOURCE**: `InitializationWizard.kt` Step 11 & AI Assistant.
* **EXPECTED BEHAVIOR**:
  - Parses multi-day routines:
    ```
    Monday - Chest + Biceps
    Bench Press 3x8
    Incline DB Press 3x10-15
    Cable Fly 3x12
    Barbell Curl 3x10
    ```
  - Extracts Day, Focus, Exercises, Sets, Min reps, Max reps (with default single target = max rep e.g. 15), Weights if provided, RIR/RPE, rest, set type.
  - If weight is omitted: weight = NULL (never fabricate).
  - Displays parsed structure with Edit, Apply, Discard actions.
* **ACTUAL BEHAVIOR**:
  - `WorkoutTextParser.kt` handles single-day and multi-day parsing with regex, but multi-day parser needed integration into the initialization wizard and assistant tool router.
* **BUG**: Parser was not wired into the multi-day routine replacement flow in UI.
* **FIX**:
  - Implement multi-day parser in `WorkoutTextParser.kt` returning `List<ParsedDayRoutine>`.
  - Provide review/apply/discard card in Initialization and Assistant.

---

### 11. Context-Aware Local AI Assistant & Typed Tools
* **FEATURE**: Local AI Assistant with State-Aware Tools
* **CURRENT IMPLEMENTATION**: `AssistantService.kt`, `AssistantSheet.kt`.
* **DATA SOURCE**: All DAOs (`UserProfileDao`, `TrainingScheduleDao`, `WorkoutTemplateDao`, `WorkoutDao`, `DailyActivityDao`, `ExerciseDao`).
* **UI SOURCE**: `AssistantSheet.kt` and Home Assistant Card.
* **EXPECTED BEHAVIOR**:
  - Architecture: Assistant UI → AssistantService → Tool Router → Typed Application Tools → Repositories/DAOs → Room.
  - Query tools (read-only, instant response):
    - `getTodayWorkout()`
    - `getWorkoutForDay(day)`
    - `getHistoricalWorkout(date)`
    - `getNutritionToday()`
    - `getNutritionTargets()`
    - `getActivityToday()`
    - `getActivityHistory(range)`
    - `getCurrentWeight()`
    - `getTrainingStreak()`
    - `getUpcomingSessions()`
  - Mutation tools (generate interactive pending action card with [Confirm] [Cancel]):
    - `prepareMoveWorkout(fromDay, toDay)`
    - `prepareReplaceExercise(day, oldEx, newEx)`
    - `prepareAddExercise(day, exerciseName, sets, reps)`
    - `prepareUpdateNutrition(calories, protein)`
    - `prepareReplaceProgram(parsedRoutines)`
* **ACTUAL BEHAVIOR**:
  - `AssistantService.kt` only handled move workout and basic keyword responses. Queries for past workouts, steps, exercise substitution, and adding exercises were generic strings.
* **BUG**: Assistant lacked complete typed tool suite and could not perform requested mutations.
* **FIX**:
  - Implement all 15 typed application tools in `AssistantService.kt`.
  - Bind pending action cards in `AssistantSheet.kt` with live state confirmation.

---

### 12. AI Food & Macro Recognition (FUD-AI Reusable Architecture)
* **FEATURE**: Photo & Text Meal Nutrition Estimation
* **CURRENT IMPLEMENTATION**: `NutritionViewModel.kt`, `AssistantService.kt`.
* **DATA SOURCE**: Offline deterministic macro database for common foods (energy, protein, carbs, fat per 100g).
* **UI SOURCE**: `AssistantSheet.kt` & `NutritionScreen.kt`.
* **EXPECTED BEHAVIOR**:
  - User can type meal description or provide food photo.
  - Assistant analyzes and estimates: Food items, Serving size, Calories, Protein, Carbs, Fat.
  - Labeled explicitly as "ESTIMATED", never exact truth.
  - Interactive Review Card:
    - Lists breakdown (e.g. Rice ~250g ~325 kcal, Chicken ~180g ~300 kcal).
    - Shows TOTAL ESTIMATE.
    - Actions: `[ Add to Today ]`, `[ Edit ]`, `[ Discard ]`.
* **ACTUAL BEHAVIOR**:
  - Feature was missing; user had no way to analyze meals or estimate macros via assistant.
* **BUG**: No meal recognition engine or macro estimation UI existed.
* **FIX**:
  - Build `LocalFoodNutritionEngine.kt` containing an offline nutritional database for standard whole foods and meals.
  - Implement photo/text meal parser in `AssistantService.kt`.
  - Render interactive meal estimation card with `[Add to Today]`, `[Edit]`, `[Discard]`.

---

### 13. Nutrition Assistant & Real-time Macro Tracking
* **FEATURE**: Nutrition Progress & Macro Queries
* **CURRENT IMPLEMENTATION**: `NutritionScreen.kt`, `NutritionViewModel.kt`.
* **DATA SOURCE**: `daily_activity` (active calories, calories burned), `user_profile` (targets).
* **UI SOURCE**: `NutritionScreen.kt`.
* **EXPECTED BEHAVIOR**:
  - Assistant answers: "What have I eaten today?", "How much protein do I have left?", "How many calories can I still eat?", "Does this fit my target?".
  - UI displays consumed vs remaining targets reactively.
  - Target modifications require user confirmation.
* **ACTUAL BEHAVIOR**:
  - Nutrition queries only returned static target values without computing consumed vs remaining.
* **BUG**: Nutrition state was not computed against logged food/activity.
* **FIX**:
  - Wire consumed meal logs into `DailyActivityEntity` / daily nutrition totals.
  - Add remaining calculation in `AssistantService.kt`.

---

### 14. Transformation Vault & Cryptographic Protection
* **FEATURE**: Encrypted Physique Chronicle
* **CURRENT IMPLEMENTATION**: `TransformationVaultManager.kt`, `JourneyViewModel.kt`, `JourneyScreen.kt`.
* **DATA SOURCE**: `transformation_checkins` table and encrypted file storage (`filesDir/vault/`).
* **UI SOURCE**: `JourneyScreen.kt`.
* **EXPECTED BEHAVIOR**:
  - Front, Side, Back photo slots.
  - Clear actions: `[ Take Photo ]`, `[ Choose from Gallery ]`.
  - Photos stored encrypted with Android Keystore AES-256-GCM.
  - Password hashed with salted PBKDF2; raw password is never persisted.
  - Default view displays blurred thumbnails.
  - Tapping thumbnail prompts for password modal.
  - Successful verification unlocks full-res preview and gallery.
  - Navigating back re-locks vault.
  - Auto-lock on app backgrounding if enabled in settings.
* **ACTUAL BEHAVIOR**:
  - Entire screen was blocked by a single lock card; no thumbnails or photo-taking options were accessible until unlocked.
  - No thumbnail gallery or blurred preview.
* **BUG**: Poor state machine trapped user; lacked blurred thumbnail state and clear Front/Side/Back capture actions.
* **FIX**:
  - Rebuild `JourneyScreen.kt` state machine:
    `LOCKED (blurred thumbnails + capture actions) -> CHALLENGE DIALOG -> UNLOCKED PREVIEW -> BACK -> LOCKED`.
  - Add explicit Front, Side, Back buttons and gallery picker.

---

### 15. Transformation Check-In Popup & 6-Month Timeline Slideshow
* **FEATURE**: Milestone Alerts & Progress Slideshow
* **CURRENT IMPLEMENTATION**: `JourneyViewModel.kt`, `HomeScreen.kt`.
* **DATA SOURCE**: `transformation_checkins`, `user_profile.transformation_start_date`.
* **UI SOURCE**: Home launch dialog & `JourneyScreen.kt`.
* **EXPECTED BEHAVIOR**:
  - Check-in popup appears on Home launch ONLY when a weekly milestone is actually due (7 days since last check-in or start).
  - Dialog options: `[ Add Photos ]`, `[ Remind Me Later ]`, `[ Skip ]`.
  - Timeline slideshow compiles verified check-in photos chronologically.
  - Only includes weeks for which real photos exist; never fabricates missing weeks.
  - Controls: Play, Pause, Next, Previous.
* **ACTUAL BEHAVIOR**:
  - Check-in popup did not exist on Home.
  - Slideshow was missing.
* **BUG**: Missing milestone prompt and timeline slideshow.
* **FIX**:
  - Implement milestone check calculation in `HomeViewModel.kt` and display modal check-in dialog when due.
  - Implement slideshow player in `JourneyScreen.kt` using decrypted bitmaps in memory.

---

### 16. Real Personal Record (PR) System
* **FEATURE**: Empirical PR Engine
* **CURRENT IMPLEMENTATION**: `ExercisePersonalRecordEntity`, `ProgressViewModel.kt`.
* **DATA SOURCE**: `exercise_personal_records` populated exclusively on `finishWorkout()`.
* **UI SOURCE**: `ProgressScreen.kt` & Home Recent PR card.
* **EXPECTED BEHAVIOR**:
  - NO PR cards before real data exists.
  - Zero preloaded or fabricated PRs.
  - PRs derived strictly from logged `WorkoutSetEntity` rows (max weight, max reps at max weight, estimated 1RM via Brzycki/Epley).
  - New PR record generated immediately upon workout completion.
* **ACTUAL BEHAVIOR**:
  - Database schema and generation logic existed, but Home screen did not display recent PRs, and empty PR state needed clean presentation.
* **STATUS**: Verified zero-mock compliant. Fix verified in `ProgressViewModel.kt`.

---

### 17. Real Workout History System
* **FEATURE**: Session Chronicle
* **CURRENT IMPLEMENTATION**: `WorkoutSessionEntity`, `WorkoutSetEntity`, `ProgressScreen.kt`.
* **DATA SOURCE**: `workout_sessions` where `status = 'COMPLETED'`.
* **UI SOURCE**: `ProgressScreen.kt` (History tab).
* **EXPECTED BEHAVIOR**:
  - If 0 completed workouts: "No workouts yet."
  - When workout completed: appears immediately in history with Date, Workout name, Duration, Total volume, and an expandable list of exercises with all sets (reps, weight, RIR).
* **ACTUAL BEHAVIOR**:
  - History tab showed session name and volume, but did not display or expand the logged exercise sets.
* **BUG**: Missing exercise and set breakdown inside history session cards.
* **FIX**:
  - Add set inspection dialog/sheet in `ProgressScreen.kt` showing full exercise and set logs for any tapped historical session.

---

### 18. Progress Telemetry & Real-Time Single Source of Truth
* **FEATURE**: Unified Analytical Engine
* **CURRENT IMPLEMENTATION**: `ProgressViewModel.kt`, `WorkoutDao.kt`.
* **DATA SOURCE**: Single source of truth: `workout_sets` linked to `workout_sessions`.
* **UI SOURCE**: `ProgressScreen.kt`, `HomeScreen.kt`.
* **EXPECTED BEHAVIOR**:
  - Volume, 1RM, session frequency, streak, and PRs all derive from the exact same table (`workout_sets`).
  - Editing upcoming templates does NOT change historical progress.
  - Real-time updates via Room `Flow` — no manual refresh required.
* **ACTUAL BEHAVIOR**:
  - Architecture adheres to single source of truth, but UI needed StateFlow collection across all screens to ensure immediate updates without tab switching.
* **STATUS**: Verified reactive flows.

---

### 19. Activity Tracking & Health Connect Pipeline
* **FEATURE**: Health Connect Integration
* **CURRENT IMPLEMENTATION**: `HealthConnectPipeline.kt`, `DailyActivityDao.kt`.
* **DATA SOURCE**: `androidx.health.connect.client.HealthConnectClient` and `daily_activity` table.
* **UI SOURCE**: `HomeScreen.kt` (Activity card).
* **EXPECTED BEHAVIOR**:
  - Aggregated step reads to prevent double counting.
  - Active calories vs total calories.
  - Explicit provenance: `MEASURED` (from Health Connect) vs `ESTIMATED` (calculated). Never claim estimated calories are measured.
  - Midnight rollover: Yesterday's steps remain in history; today resets to 0.
* **ACTUAL BEHAVIOR**:
  - `DailyActivityEntity` had `is_calories_measured` flag, but UI did not display the `MEASURED` / `ESTIMATED` badge clearly.
* **BUG**: Missing provenance badge on Home Activity card.
* **FIX**:
  - Add explicit `MEASURED` / `ESTIMATED` badge next to calories on `HomeScreen.kt`.

---

### 20. Daily Training Streak Engine
* **FEATURE**: Compliance-Based Training Streak
* **CURRENT IMPLEMENTATION**: `TrainingStreakEngine.kt`.
* **DATA SOURCE**: `workout_sessions` (completed) + `training_schedule`.
* **UI SOURCE**: `HomeScreen.kt` & `ProgressScreen.kt`.
* **EXPECTED BEHAVIOR**:
  - Rest days DO NOT break streak.
  - Skipped scheduled training days DO break streak.
  - Updates instantly when a workout finishes.
  - Calculates current streak, longest streak, consistency %.
* **ACTUAL BEHAVIOR**:
  - Engine is fully implemented and tested (9 unit tests pass in `TrainingStreakEngineTest.kt`).
* **STATUS**: Complete and verified.

---

### 21. Notification Engine & Rescheduling on Plan Edit
* **FEATURE**: Intelligent State-Driven Alarms
* **CURRENT IMPLEMENTATION**: `NotificationScheduler.kt`, `ForgeNotificationReceiver.kt`.
* **DATA SOURCE**: Android `AlarmManager` + `training_schedule` + `app_settings`.
* **UI SOURCE**: System status bar notifications.
* **EXPECTED BEHAVIOR**:
  - Morning ("Today is a training day: Push"), Pre-workout, Post-workout, Streak, PR.
  - When schedule changes (e.g. Friday moved to Saturday), old alarms must be cancelled and rescheduled.
  - Toggles in Profile Settings must cancel/schedule immediately.
* **ACTUAL BEHAVIOR**:
  - `NotificationScheduler.kt` has scheduling routines, but rescheduling was not invoked when templates/schedules were modified.
* **BUG**: Schedule edits did not trigger alarm rescheduling.
* **FIX**:
  - Trigger `NotificationScheduler.scheduleWorkoutReminders()` inside `WorkoutsViewModel` and `AssistantService` whenever schedules change.

---

### 22. Upcoming Session Date Calculation
* **FEATURE**: Dynamic Next Training Session Resolver
* **CURRENT IMPLEMENTATION**: `HomeViewModel.kt`, `WorkoutsViewModel.kt`.
* **DATA SOURCE**: Current local device date (`LocalDate.now()`) + `training_schedule`.
* **UI SOURCE**: `HomeScreen.kt` & `WorkoutsScreen.kt`.
* **EXPECTED BEHAVIOR**:
  - Must not show static Monday cards regardless of current date.
  - Calculates today's session based on current day of week.
  - If today is rest, displays "Rest & Recovery" with option to log an extra session or previews next training day.
* **ACTUAL BEHAVIOR**:
  - Week cycle currently resolves from Monday of current week correctly, but rest day card needed clear preview of next training session.
* **FIX**:
  - Enhance Today card on Home to display countdown or preview to next scheduled workout when today is a rest day.

---

### 23. Data Export, Import & Double-Confirmation Reset
* **FEATURE**: User Data Portability & Clean Reset
* **CURRENT IMPLEMENTATION**: Needs implementation in `ProfileViewModel.kt`.
* **DATA SOURCE**: Room database JSON export.
* **UI SOURCE**: `ProfileScreen.kt`.
* **EXPECTED BEHAVIOR**:
  - **Export**: Exports profile, schedules, templates, workouts, sets, activity to clean JSON. Passwords and encryption keys are NEVER exported.
  - **Import**: Validates JSON schema before writing to database.
  - **Reset All Data**: Two-step confirmation dialog. Deletes all Room records, wipes vault directory, resets settings, and navigates to Initialization.
* **ACTUAL BEHAVIOR**:
  - Export, import, and reset features were absent.
* **BUG**: Missing data management actions.
* **FIX**:
  - Implement `exportUserData()`, `importUserData()`, and `resetAllData()` in `ProfileViewModel.kt` with UI confirmation dialogs in `ProfileScreen.kt`.

---

### 24. Zero Mock Data Verification
* **FEATURE**: Strict Zero Fabrication Standard
* **CURRENT IMPLEMENTATION**: Inspected across all ViewModels and Composables.
* **EXPECTED BEHAVIOR**:
  - Zero fake PRs.
  - Zero fake history.
  - Zero fake calories or steps.
  - Zero fake body photos.
  - Empty states display clean, motivational prompts explaining how to log the first real entry.
* **ACTUAL BEHAVIOR**:
  - All mock lists previously removed. Schema v5 tables start empty on fresh install.
* **STATUS**: Zero mock data verified across entire codebase.

---

## Action Plan & Implementation Checklist

| Task # | Component | Action |
|---|---|---|
| 1 | `UserProfileEntity.kt` & DB Migration | Add `height_cm`, `weight_kg`, `age`, `sex` columns and update entity/migration. |
| 2 | `InitializationWizard.kt` & `InitializationViewModel.kt` | Add Body Stats step, Multi-select Equipment, Dedicated Permissions Setup step, and Program Import review flow. |
| 3 | `ProfileScreen.kt` & `ProfileViewModel.kt` | Implement Edit Profile Sheet, Settings suite (Notification toggles, Auto-lock, Reduce Motion, Particles, Haptics), JSON Export/Import, and Two-step Reset All Data. |
| 4 | `HomeScreen.kt` | Fix visual hierarchy (`Today -> Assistant -> Snapshot -> Activity -> Recent PR -> Contextual`), eliminate empty space bug, add explicit `MEASURED` / `ESTIMATED` calorie badge. |
| 5 | `ForgeNavGraph.kt` | Fix Home Quick Action navigation trap by resetting popUpTo on tab reselection and keeping Home at true root. |
| 6 | `SessionEditorSheet.kt` & `WorkoutsViewModel.kt` | Expand session editor for weight, RIR/RPE, warmup, dropset, notes, exact rep ranges, and versioned template saving. |
| 7 | `WorkoutsScreen.kt` | Add Recent tab with View/Edit/Duplicate/Start actions and expand Templates tab actions. |
| 8 | `AssistantService.kt` & `AssistantSheet.kt` | Implement full 15 typed application tools, nutrition meal estimation with local food database, and pending mutation confirmation cards. |
| 9 | `JourneyScreen.kt` & `JourneyViewModel.kt` | Rebuild Transformation screen with Front/Side/Back/Gallery capture, blurred thumbnails, password challenge modal, auto-lock on pause, and timeline slideshow. |
| 10 | `ProgressScreen.kt` | Add expandable exercise and set detail dialog in History tab. |
| 11 | Automated Test Suite | Add automated tests for all modified components and verify on physical Samsung Galaxy S21 FE. |
