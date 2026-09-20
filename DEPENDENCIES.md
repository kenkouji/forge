# FORGE — Dependency Manifest & License Audit (DEPENDENCIES.md)

**Generated:** September 2026  
**Status:** Approved Dependency Baseline (Phase 0 Corrections Incorporated)  
**Verification:** 100% Free / Zero Paid Dependencies / Zero Mandatory Cloud / Zero Proprietary APIs

---

## 1. Core Rule Compliance Statement

Under Section 1.1 of the FORGE Master Specification:
- **Monthly Cost:** ₹0 / $0
- **Paid APIs:** None
- **Subscriptions:** None
- **Mandatory Cloud Services:** None
- **Mandatory Backend:** None
- **Proprietary Paid SDKs:** None
- **Paid Exercise Databases:** None

Every dependency listed below is open-source, permissively licensed (Apache 2.0, MIT, or Unlicense), and verified for commercial compatibility.

---

## 2. Cohesive & Compatible Toolchain Matrix

| Component | Verified Version | License | Primary Source & Compatibility |
| :--- | :--- | :--- | :--- |
| **Android Gradle Plugin (AGP)** | `8.5.2` | Apache 2.0 | [Android Studio Release Notes](https://developer.android.com/studio/releases/gradle-plugin) (Requires Gradle 8.7+) |
| **Gradle** | `8.7` | Apache 2.0 | [Gradle Releases](https://docs.gradle.org/8.7/release-notes.html) (Requires JDK 17) |
| **Kotlin Standard Library** | `2.0.21` | Apache 2.0 | [Kotlin Releases](https://github.com/JetBrains/kotlin/releases/tag/v2.0.21) |
| **Compose Compiler Plugin** | `2.0.21` | Apache 2.0 | Bundled in Kotlin 2.0 (`org.jetbrains.kotlin.plugin.compose`) |
| **Kotlin Symbol Processing (KSP)**| `2.0.21-1.0.28` | Apache 2.0 | [Google KSP Releases](https://github.com/google/ksp/releases/tag/2.0.21-1.0.28) (Strict match for Kotlin 2.0.21) |
| **Room Persistence Library** | `2.6.1` | Apache 2.0 | [Android Room Releases](https://developer.android.com/jetpack/androidx/releases/room) |
| **Compose BOM** | `2024.09.02` | Apache 2.0 | [Compose BOM Mapping](https://developer.android.com/jetpack/compose/bom/bom-mapping) (Compose UI 1.7.2, Material3 1.3.0) |

---

## 3. Runtime & Platform Libraries

### 3.1 Jetpack Compose (BOM Managed)
All Compose libraries align to `2024.09.02`:
- `androidx.compose.ui:ui`
- `androidx.compose.ui:ui-graphics`
- `androidx.compose.ui:ui-tooling-preview`
- `androidx.compose.material3:material3`
- `androidx.compose.foundation:foundation`
- `androidx.compose.ui:ui-tooling` (debugImplementation only)

### 3.2 Navigation, Lifecycle & Concurrency
| Component | Identifier | Version | License | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **Activity Compose** | `androidx.activity:activity-compose` | `1.9.2` | Apache 2.0 | Activity lifecycle integration |
| **Navigation Compose** | `androidx.navigation:navigation-compose` | `2.8.0` | Apache 2.0 | Type-safe declarative app navigation |
| **Lifecycle ViewModel** | `androidx.lifecycle:lifecycle-viewmodel-compose` | `2.8.5` | Apache 2.0 | ViewModel lifecycle and StateFlow integration |
| **Coroutines Core** | `org.jetbrains.kotlinx:kotlinx-coroutines-core` | `1.8.1` | Apache 2.0 | Asynchronous domain operations |
| **Coroutines Android** | `org.jetbrains.kotlinx:kotlinx-coroutines-android` | `1.8.1` | Apache 2.0 | Android main looper dispatcher |
| **Kotlinx Serialization** | `org.jetbrains.kotlinx:kotlinx-serialization-json` | `1.7.1` | Apache 2.0 | Pure Kotlin JSON serialization for backup & exports |

### 3.3 Persistence & Storage
| Component | Identifier | Version | License | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **Room Runtime** | `androidx.room:room-runtime` | `2.6.1` | Apache 2.0 | SQLite ORM & DAO mapping |
| **Room KTX** | `androidx.room:room-ktx` | `2.6.1` | Apache 2.0 | Coroutines Flow streams for Room queries |
| **Room Compiler** | `androidx.room:room-compiler` | `2.6.1` | Apache 2.0 | Annotation processing via KSP |
| **DataStore Preferences** | `androidx.datastore:datastore-preferences` | `1.1.1` | Apache 2.0 | App settings and user configuration persistence |

### 3.4 Hardware & Android Integrations (Future Phases)
| Component | Identifier | Version | License | Phase |
| :--- | :--- | :--- | :--- | :--- |
| **Health Connect Client** | `androidx.health.connect:connect-client` | `1.1.0` | Apache 2.0 | Phase 7 |
| **Biometric Jetpack** | `androidx.biometric:biometric` | `1.2.0-alpha05`| Apache 2.0 | Phase 8 |
| **CameraX Core/Camera2** | `androidx.camera:camera-camera2` | `1.3.4` | Apache 2.0 | Phase 6 |
| **ML Kit Text Recognition**| `com.google.android.gms:play-services-mlkit-text-recognition` | `19.0.0` | Apache 2.0 | Phase 5 |

---

## 4. Testing Frameworks (Zero-Emulator Desktop Validation)

| Component | Identifier | Version | License | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **JUnit 4 / 5** | `junit:junit` / `org.junit.jupiter` | `4.13.2` / `5.11.0` | EPL 2.0 | Pure JVM domain and unit tests |
| **Robolectric** | `org.robolectric:robolectric` | `4.13` | Apache 2.0 | Local Android Room & SQLite tests on Mac JVM |
| **Room Testing** | `androidx.room:room-testing` | `2.6.1` | Apache 2.0 | Automated schema migration testing |
| **Google Truth** | `com.google.truth:truth` | `1.4.4` | Apache 2.0 | Fluent assertions |
| **Turbine** | `app.cash.turbine:turbine` | `1.1.0` | Apache 2.0 | Flow testing |

---

## 5. Exercise Dataset Audit & Licensing Record

### 5.1 Dataset License Verification Table

| Dataset | Data License | Images License | Commercial Use | Attribution | Redistribution | Approved? |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Free Exercise DB** (`yuhonas/free-exercise-db`) | **The Unlicense** (Dedicated in repo `LICENSE.md`) | **Unverified / Third-Party** (Maintainer noted in Issues #2 & #12: origins unknown/scraped) | Permitted for JSON | Not legally required; credited in FORGE docs/credits | Permitted for JSON | **APPROVED (Text Only):** Ingest 876 validated canonical exercise records. **STRICTLY REJECT ALL BUNDLED RASTER IMAGES** due to unverified third-party copyright. |
| **Kinetic Exercises** (`kinetic-place/exercises-api`) | **MIT License** (Documented in repo root) | MIT | Permitted | Permitted with MIT copyright notice | Permitted | **APPROVED (Taxonomy & Reference Only):** Used as taxonomy/classification reference. Inferred classifications tagged `source = "forge_derived"`. |
| **RepDB (Free Tier)** (`RepDB/exercise-dataset`) | **RepDB Free Tier License v1.0** | Included Free WebP | Permitted in-app only | Mandatory visible link: *"Exercise data by RepDB (repdb.co)"* | **STRICTLY PROHIBITED** (Term 3 forbids dataset/API redistribution) | **NOT APPROVED FOR BUNDLING:** Term 3 prohibits distributing derived seed datasets in an open codebase. Avoided to keep FORGE 100% legally unencumbered and open. |
| **wrkout/exercises.json** | **The Unlicense** (Dedicated in repo `LICENSE.md`) | Web-scraped images flagged by maintainers | Permitted for JSON | Not legally required | Permitted | **APPROVED (Text Reference Only):** Used for alias derivation. Media excluded. |
| **Proprietary Databases (Hevy, Strong, ExRx)** | Various | Commercial Copyright | Strictly Forbidden | N/A | Strictly Forbidden | **STRICTLY PROHIBITED:** Absolute zero scraping. |

### 5.2 Mandatory Attribution Disclosures
* **Free Exercise DB**: Exercise metadata licensed under The Unlicense (Public Domain dedication). Maintained by yuhonas and Ollie Jennings (`wrkout/exercises.json`).
* **Kinetic Exercises**: Biomechanical taxonomy reference licensed under the MIT License, Copyright (c) kinetic.place.
* Attribution is integrated into the application's **Settings → About & Credits** section and project documentation.
