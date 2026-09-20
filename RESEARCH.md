# FORGE — Technical Research & Verification Document (RESEARCH.md)

**Generated:** September 2026  
**Status:** Approved Architectural Baseline (Phase 0 Corrections Incorporated)  
**Scope:** Verified Toolchain Matrix, Defensible Min SDK, SQLite Search Strategy, Exercise DB Licensing Audit, Transactional Crash Recovery, and Mac Storage Safeguards.

---

## 1. Verified Toolchain Compatibility Matrix

To prevent build instability, version drift, and broken annotation processors, we establish a single cohesive, verified compatible version set:

| Component | Verified Version | Minimum Requirement | Primary Source |
| :--- | :--- | :--- | :--- |
| **Android Gradle Plugin (AGP)** | **8.5.2** | Minimum Gradle 8.7, JDK 17 | [Android Studio Release Notes](https://developer.android.com/studio/releases/gradle-plugin) |
| **Gradle** | **8.7** | JDK 17 | [Gradle Releases](https://docs.gradle.org/8.7/release-notes.html) |
| **Kotlin** | **2.0.21** | Compatible with AGP 8.5+ | [Kotlin Releases](https://github.com/JetBrains/kotlin/releases/tag/v2.0.21) |
| **Compose Compiler** | **2.0.21** | Built into Kotlin 2.0 (`org.jetbrains.kotlin.plugin.compose`) | [Jetpack Compose to Kotlin Mapping](https://developer.android.com/jetpack/androidx/releases/compose-kotlin) |
| **Kotlin Symbol Processing (KSP)** | **2.0.21-1.0.28** | Exact match for Kotlin 2.0.21 | [Google KSP Releases](https://github.com/google/ksp/releases/tag/2.0.21-1.0.28) |
| **Room Persistence Library** | **2.6.1** | KSP compatible, SQLite 3.9+ | [Android Room Releases](https://developer.android.com/jetpack/androidx/releases/room) |
| **Compose BOM** | **2024.09.02** | Compose UI 1.7.2, Material3 1.3.0 | [Compose BOM Mapping](https://developer.android.com/jetpack/compose/bom/bom-mapping) |

---

## 2. Defensible Minimum SDK Determination

Instead of arbitrary assumptions, the minimum SDK for FORGE is formally established at **API 26 (Android 8.0 Oreo)** based on the following technical evaluation:

1. **Native Date/Time API (`java.time.*`):**
   - API 26 introduced native `java.time.Instant`, `LocalDate`, `Duration`, and `ZoneId`.
   - Supporting API < 26 requires R8 core library desugaring (`desugar_jdk_libs`), which adds build overhead, increases APK size by ~250 KB, and complicates pure JVM testing.
2. **Notification Channels (`NotificationChannel`):**
   - Required for gym rest timer alarms, recovery alerts, and background timer notifications.
   - Introduced in API 26; eliminating pre-Oreo legacy notification logic reduces code paths and testing surface.
3. **Health Connect Client Support:**
   - Google Health Connect client (`androidx.health.connect:connect-client`) strictly requires **API 26 or higher**. It is distributed as an APK for Android 8–13 and integrated into the OS for Android 14+. Supporting API < 26 would prevent Health Connect entirely.
4. **Scoped Storage & File System:**
   - Predictable app-specific directory structures (`context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)`) without requiring dangerous legacy `WRITE_EXTERNAL_STORAGE` permissions.
5. **Real-World Device Distribution:**
   - Android distribution statistics confirm API 26+ covers ~95% of active global Android hardware. The primary physical target device (Samsung Galaxy S21 FE 5G) runs Android 13/14 (API 33/34).
   - **Conclusion:** `minSdk = 26`, `targetSdk = 34`, `compileSdk = 34` (or 35).

---

## 3. SQLite Search Evaluation: FTS4 vs FTS5 vs Alternatives

| Criteria | SQLite FTS4 | SQLite FTS5 | LIKE '%query%' (Standard SQL) | External Local Search (ObjectBox/Realm) |
| :--- | :--- | :--- | :--- | :--- |
| **Room Integration** | **First-class native** via `@Fts4` entity annotation | Requires custom raw SQL migration or external table mapping | First-class native | Requires separate proprietary C++ engine |
| **Android Availability** | API 11+ (100% of devices) | SQLite 3.9+ (API 24+) | All versions | Independent binary |
| **Tokenization** | `unicode61`, `simple`, `porter` | Advanced prefix/trigram, external tokenizers | None (substring scan) | Custom |
| **Performance (2K-10K rows)**| Instant (<3ms prefix match) | Instant (<2ms prefix match) | Sluggish ($O(N)$ full table scan, 25-50ms) | Instant |
| **Disk Overhead** | Low (~1.2x of content size) | Moderate (~1.5x of content size) | Zero additional index | High (Separate DB files) |
| **Maintenance Burden** | Minimal (Room handles index generation) | Moderate (Manual sync triggers required in Room) | None | High (Third-party C++ dependencies) |

### Selected Search Architecture:
We select **Room `@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61)`** with external content mapping:
- **Rationale:** Room provides compile-time verification for `@Fts4`, eliminating raw SQL boilerplate. Since FORGE's exercise database contains ~1,000–3,000 exercises and ~5,000 aliases, FTS4 provides instantaneous search response times while keeping database footprint and complexity minimal.
- **Prefix Matching Pattern:** `MATCH :query || '*'` handles partial words (e.g. `benc` $\to$ `Bench Press`).
- **Target Performance Standard:** *"Exercise search must feel instantaneous on a realistic database containing thousands of exercises and aliases."* (Replaces arbitrary sub-millisecond claims).

---

## 4. Exercise Data Licensing & Asset Provenance Audit

Under Section 1.1 and Phase 2 Requirements, we audit candidate datasets and assets against strict licensing, attribution, and redistribution criteria:

### 4.1 Dataset & Asset License Audit Table

| Dataset | Data License | Images License | Commercial Use | Attribution | Redistribution | Approved? |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Free Exercise DB** (`yuhonas/free-exercise-db`) | **The Unlicense** (Public Domain dedication in repository `LICENSE.md`) | **Unverified / Third-Party** (Maintainer explicitly states in Issues #2 & #12: origins unknown/scraped, likely bodybuilding.com) | Permitted for JSON metadata | Not legally required; credited in FORGE docs/credits | Permitted for JSON metadata | **APPROVED (Textual Metadata Only):** Ingest 876 validated canonical exercise records. **STRICTLY REJECT ALL BUNDLED RASTER IMAGES** due to unverified third-party copyright. |
| **Kinetic Exercises** (`kinetic-place/exercises-api`) | **MIT License** (Documented in repo root) | MIT | Permitted | Permitted with MIT copyright notice | Permitted | **APPROVED (Taxonomy & Biomechanical Reference):** Used for movement-pattern classification rules and anatomical taxonomy. Any FORGE-inferred field is tagged `source = "forge_derived"`. |
| **RepDB (Free Tier)** (`RepDB/exercise-dataset`) | **RepDB Free Tier License v1.0** | Included Free WebP (Non-animating) | Permitted in-app only | Mandatory visible link: *"Exercise data by RepDB (repdb.co)"* | **STRICTLY PROHIBITED** (Term 3: *"You may not republish, resell, or repackage the dataset — or a modified or derived dataset — as a dataset, dataset repository, or API"*) | **NOT APPROVED FOR BUNDLING:** Term 3 prohibits distributing derived seed datasets in an open codebase. Avoided to keep FORGE 100% legally unencumbered and open. |
| **wrkout/exercises.json** | **The Unlicense** (Public Domain dedication in repo `LICENSE.md`) | Web-scraped images flagged by maintainers | Permitted for JSON | Not legally required | Permitted | **APPROVED (Upstream Text Reference):** The upstream source of Free Exercise DB. Used for alias derivation. Zero images bundled. |

### 4.2 Critical Asset Safety Protocol:
1. **Zero Unverified Image Bundling:** The 2,600+ JPEG files in `free-exercise-db` will NOT be packaged or redistributed in the APK. Bundling them would both bloat the APK and risk copyright infringement.
2. **Textual Content Safety:** Only the step-by-step instructions, equipment, category, force, level, and muscle mappings released under The Unlicense are ingested.
3. **Media References:** Demonstration media is represented via YouTube Video IDs or clean vector anatomical diagrams. Video references are opened via Android system Intents or player components, requiring zero video rehosting.

### 4.3 Normalized Relational Architecture vs Comma-Separated Strings
Storing relational data (muscles, equipment, aliases, families) as comma-separated strings inside a single table limits query expressiveness, prevents foreign key constraints, and impairs relational joins. FORGE adopts a fully normalized relational schema:
- `exercises`: Core canonical exercise records, distinguishing raw `source_*` fields from `forge_*` classifications.
- `muscles`: Expandable muscle taxonomy supporting arbitrary muscle groups (e.g. adductors, abductors, middle back, neck, etc.).
- `exercise_muscles`: Relational mapping linking exercises to muscles with roles (`PRIMARY`, `SECONDARY`, `STABILIZER`) and `is_forge_derived` flags.
- `equipment`: Normalized equipment taxonomy (barbell, dumbbell, cable, machine, bodyweight, kettlebell, etc.).
- `exercise_equipment`: Normalized mapping of equipment requirements (`is_primary`).
- `exercise_aliases`: Normalized search aliases with `is_forge_derived`.
- `exercise_families` & `exercise_family_members`: Multi-variant groupings for progressions, substitutions, and volume analysis.
- `exercise_attributes`: Normalized flexible tags (`INCLINE`, `UNILATERAL`, `DEFICIT`, `CLOSE_GRIP`, etc.) replacing monolithic variation enums.
- `exercises_fts`: Virtual FTS4 table over `(name, canonical_name, forge_movement_pattern, search_tokens, instructions)` enabling instant prefix search.

---

## 5. Single-Source-of-Truth Transactional Crash Recovery Model

In accordance with Phase 0 Correction 8, `ActiveWorkoutSnapshotEntity` is **REJECTED** as a secondary source of truth.

### 5.1 Recovery Architecture

```text
               Atomic User Action (Check Set / Edit Weight / Add Exercise)
                                          │
                                          ▼
                     ┌──────────────────────────────────────────┐
                     │          Single Room Transaction         │
                     │  1. Insert / Update `workout_sets`       │
                     │  2. Update `workout_sessions` metadata:  │
                     │     - status = 'IN_PROGRESS'             │
                     │     - active_exercise_id                 │
                     │     - active_set_index                   │
                     │     - updated_timestamp                  │
                     └──────────────────────────────────────────┘
                                          │
                  ┌───────────────────────┴───────────────────────┐
                  ▼                                               ▼
          Normal Lifecycle                               Unscheduled Event
      (App Background / Resume)                   (Crash / Process Kill / Restart)
                  │                                               │
                  ▼                                               ▼
          Instant StateFlow                           Cold Launch Recovery Check:
        in Memory ViewModel                        SELECT * FROM workout_sessions 
                                                   WHERE status = 'IN_PROGRESS'
                                                   ORDER BY start_time DESC LIMIT 1
                                                                  │
                                                                  ▼
                                                      Session Found? Prompt User:
                                                      "Resume active workout?"
                                                      - Load transactional sets
                                                      - Active exercise highlighted
                                                      - Next uncompleted set queued
```

### 5.2 Verification Matrix
1. **Normal Close / Completion:** User taps "Finish Workout" $\to$ `status` updated to `'COMPLETED'`.
2. **App Backgrounding:** Session remains `'IN_PROGRESS'`; state retained in ViewModel.
3. **Force-Stop (`am force-stop`):** On relaunch, app detects `'IN_PROGRESS'` session and prompts recovery.
4. **Process Death (Low Memory Killer):** Same recovery check executes seamlessly.
5. **Device Restart:** Database state survives device reboot.
6. **Interrupted Write:** SQLite WAL mode guarantees ACID transaction: either the set write and session update commit together, or neither commits, preventing corruption.

---

## 6. Mac Storage Envelope & Android SDK Strategy

### 6.1 Host Storage Profile
- Available disk space on `/System/Volumes/Data`: ~20 GiB.
- Full Android Studio + Emulator + AVD system images: ~15–25 GB (Dangerously high).

### 6.2 Storage-Optimized Installation Plan
| Component | Estimated Disk Footprint | Installation Method |
| :--- | :--- | :--- |
| `android-commandlinetools` | ~150 MB | Homebrew Cask |
| Android SDK Platform 34 | ~85 MB | `sdkmanager "platforms;android-34"` |
| Android Build Tools 34.0.0 | ~65 MB | `sdkmanager "build-tools;34.0.0"` |
| Gradle Wrapper & Cache | ~250 MB | Auto-download via `./gradlew` |
| **Total Build Toolchain Footprint** | **~550 MB** | **< 3% of available Mac storage** |

All emulation is eliminated. Validation uses JVM tests, Compose Previews, and direct deployment to the physical **Samsung Galaxy S21 FE 5G** via wireless ADB.
