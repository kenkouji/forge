# FORGE — PRODUCT STATE SPECIFICATION

This document formalizes the core state machine, state transitions, and boundary rules for FORGE.
Every UI screen, domain engine, and database entity must conform to these states without ambiguity.

---

## 1. APPLICATION LIFECYCLE STATE MACHINE

```text
               ┌───────────────────────────────┐
               │         APPLICATION           │
               │         COLD LAUNCH           │
               └───────────────┬───────────────┘
                               │
                Check AppSettings & UserProfile
                               │
               ┌───────────────┴───────────────┐
       isInitialized = false           isInitialized = true
               │                               │
               ▼                               ▼
    ┌─────────────────────┐         ┌─────────────────────┐
    │   INITIALIZATION    │         │        READY        │
    │     HARD GATE       │         │    (Dashboard)      │
    └──────────┬──────────┘         └──────────┬──────────┘
               │                               │
        12-Step Wizard                  Active workout
          Completed                        detected?
               │                               │
               ▼                     YES ──────┴────── NO
    ┌─────────────────────┐           │                │
    │  GENERATE SCHEDULE  │           ▼                ▼
    │   & REAL TEMPLATES  │    ┌──────────────┐ ┌─────────────┐
    └──────────┬──────────┘    │ RESUME POPUP │ │ HOME ROOT   │
               │               └──────────────┘ └─────────────┘
               ▼
    ┌─────────────────────┐
    │        READY        │
    └─────────────────────┘
```

### State Definitions:
1. **`FIRST_LAUNCH`**: Fresh installation. No `user_profile` exists, or `isInitialized == false`.
2. **`INITIALIZATION_IN_PROGRESS(step: Int)`**: User has progressed through step `1..12`. If app is killed or abandoned, next launch restores the exact step. No fake or default programs are written to the database until step 12 is finalized.
3. **`READY`**: User profile, training schedule, and real templates exist. Dashboard renders real state or true empty states.
4. **`ACTIVE_WORKOUT`**: A workout session entity with status `IN_PROGRESS` exists. Sticky glass header is pinned, floating bottom pill is hidden.
5. **`WORKOUT_COMPLETE`**: A session has been finalized. Disintegration particle burst transitions into summary modal, sets written to history, progressive overload engine recalculated.
6. **`HISTORY`**: Immutable historical record of completed workout sessions.

---

## 2. DAILY TRAINING STATE MACHINE

For any calendar date (using device-local midnight rollover):

```text
                  ┌──────────────────────────────┐
                  │      EVALUATE LOCAL DATE     │
                  │   AGAINST TRAINING_SCHEDULE  │
                  └──────────────┬───────────────┘
                                 │
                 Is today marked as a training day?
                                 │
                 YES ────────────┴──────────── NO
                  │                             │
                  ▼                             ▼
        ┌──────────────────┐           ┌──────────────────┐
        │ TRAINING PENDING │           │     REST DAY     │
        └─────────┬────────┘           └─────────┬────────┘
                  │                              │
         Logged a session?              Logged a session?
                  │                              │
          YES ────┴──── NO               YES ────┴──── NO
           │             │                │             │
           ▼             ▼                ▼             ▼
     ┌───────────┐ ┌────────────┐   ┌───────────┐ ┌───────────┐
     │ COMPLETED │ │   MISSED   │   │ BONUS     │ │ PLANNED │
     │  SESSION  │ │  (Streak   │   │ COMPLETED │ │ REST    │
     │  (Streak  │ │   Broken)  │   │ (Streak   │ │ (Streak │
     │  +1)      │ │            │   │  Intact)  │ │  Intact)│
     └───────────┘ └────────────┘   └───────────┘ └───────────┘
```

### Streak Rules:
- **`PLANNED_REST` does NOT break streak**: Rest is an essential component of progressive overload and recovery.
- **`MISSED_SESSION` breaks streak**: Occurs only when a scheduled training day passes midnight local time without any logged workout session.
- **`COMPLETED_SESSION` increments streak**: A scheduled session completed on that day increments the training session streak count.

---

## 3. PROGRAM VERSIONING & HISTORICAL SNAPSHOTS

To prevent retroactive alteration of historical workouts:

```text
┌─────────────────────────────────────────────────────────────┐
│                 PROGRAM VERSIONING ARCHITECTURE             │
│                                                             │
│   ┌──────────────────────────┐                              │
│   │     PROGRAM (v1, v2)     │ Current active program       │
│   └────────────┬─────────────┘                              │
│                ▼                                            │
│   ┌──────────────────────────┐                              │
│   │     SCHEDULED SESSION    │ e.g., Monday: Chest + Biceps │
│   └────────────┬─────────────┘                              │
│                ▼                                            │
│   ┌──────────────────────────┐                              │
│   │     WORKOUT TEMPLATE     │ Editable template            │
│   └────────────┬─────────────┘                              │
│                │ User modifies exercises/sets               │
│                ▼                                            │
│   ┌──────────────────────────┐                              │
│   │   ACTIVE WORKOUT SESSION │ Instantiated session copy    │
│   └────────────┬─────────────┘                              │
│                │ User logs weights & reps                   │
│                ▼                                            │
│   ┌──────────────────────────┐                              │
│   │    COMPLETED WORKOUT     │ IMMUTABLE HISTORICAL         │
│   │         SNAPSHOT         │ RECORD (Never modified)      │
│   └──────────────────────────┘                              │
└─────────────────────────────────────────────────────────────┘
```

Editing an upcoming session or template creates a new revision. Completed sessions in `workout_sessions` and `workout_sets` are frozen and immutable.

---

## 4. ASSISTANT ARCHITECTURE & TOOL CONTRACTS

The Assistant does NOT directly execute arbitrary Room mutations. It operates via deterministic, type-safe tools with user confirmations:

```text
┌─────────────────┐       ┌──────────────────────┐       ┌────────────────────────┐
│  Assistant UI   │ ────► │   AssistantService   │ ────► │ Deterministic Tools    │
│  (Glass Sheet)  │ ◄──── │   (Local Intent)     │ ◄──── │ (Read/Write Interfaces)│
└─────────────────┘       └──────────────────────┘       └───────────┬────────────┘
                                                                     │
                                                           Requires User Action?
                                                                     │
                                                           YES ──────┴────── NO
                                                            │                 │
                                                            ▼                 ▼
                                                   ┌─────────────────┐  ┌─────────────┐
                                                   │ [CONFIRMATION]  │  │ Immediate   │
                                                   │ Dialog in Sheet │  │ Read/Answer │
                                                   └────────┬────────┘  └─────────────┘
                                                            │ User taps Confirm
                                                            ▼
                                                   ┌─────────────────┐
                                                   │ Mutate Database │
                                                   └─────────────────┘
```

### Deterministic Tool Interfaces:
1. `getTodayWorkout(): TodayWorkoutResult` (Read)
2. `getNutritionStatus(): NutritionStatusResult` (Read)
3. `getExerciseHistory(exerciseId): ExerciseHistoryResult` (Read)
4. `modifyUpcomingWorkout(dayOfWeek, changes): ActionRequest` (Write -> Confirmed)
5. `addExercise(templateId, exerciseId): ActionRequest` (Write -> Confirmed)
6. `replaceExercise(templateId, oldId, newId): ActionRequest` (Write -> Confirmed)
7. `moveWorkout(fromDay, toDay): ActionRequest` (Write -> Confirmed)

---

## 5. TRANSFORMATION PHOTO SECURITY BOUNDARY

```text
┌─────────────────────────────────────────────────────────────┐
│                 TRANSFORMATION PHOTO SECURITY               │
│                                                             │
│  ┌─────────────────────────┐                                │
│  │ Encrypted File Storage  │ AES-256 encrypted on disk      │
│  │ (Android Keystore / GCM)│ in app-private directory       │
│  └────────────┬────────────┘                                │
│               ▼                                             │
│  ┌─────────────────────────┐                                │
│  │  Salted PBKDF2/SHA-256  │ Never store raw password.      │
│  │     Password Hash       │ Password verification only.    │
│  └────────────┬────────────┘                                │
│               ▼                                             │
│  ┌─────────────────────────┐                                │
│  │  UI Exposure Boundary   │ Blurred thumbnails everywhere  │
│  │                         │ until password explicitly      │
│  │                         │ verified for current session.  │
│  └────────────┬────────────┘                                │
│               ▼                                             │
│  ┌─────────────────────────┐                                │
│  │  Lifecycle Auto-Lock    │ Auto-locks when app goes to    │
│  │                         │ background.                    │
│  └─────────────────────────┘                                │
└─────────────────────────────────────────────────────────────┘
```
