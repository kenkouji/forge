# FORGE — Product Specification & Master Requirements (FORGE_SPEC.md)

**Version:** 1.0.1  
**Status:** Canonical Source of Truth (Phase 0 Corrections Incorporated)  
**Product Creed:** *Train. Adapt. Transform.*

---

## 1. Absolute Development Rules & Non-Negotiables

### 1.1 Zero Paid Dependencies
The core application must operate at **₹0 / month**:
- No paid third-party APIs (OpenAI, RapidAPI, AWS, Twilio, etc.).
- No mandatory subscriptions, paywalls, or feature tiers.
- No mandatory cloud hosting, servers, or cloud AI services.
- No proprietary paid SDKs.
- No proprietary or scraped exercise databases.
- Every capability must be implemented using Android native APIs, local algorithmic engines, or verified public-domain datasets.

### 1.2 Data Sovereignty & Privacy
- Zero mandatory accounts or social logins.
- All personal data, workout logs, physique photos, and biometrics remain strictly on-device.
- App must function completely in Airplane Mode (offline-first).
- Export and import must be available at all times via standard formats (JSON, CSV, ZIP).
- Every schema change must have an explicit, tested migration; never destroy user data to resolve a migration issue.

### 1.3 Mac Host Development Constraint
- Host Mac has ~20 GB free disk space.
- **Never install or require the Android Emulator or AVD system images.**
- Development uses minimal SDK command-line tools, pure Kotlin JVM unit tests, Robolectric for local SQLite testing, and Compose Previews.
- Physical testing targets the **Samsung Galaxy S21 FE 5G** via wireless ADB.

### 1.4 Single-Source-of-Truth Transactional Crash Recovery
- Active workout sessions must be recoverable after crashes, process kills, phone calls, or battery depletion.
- Persistence relies on transactional relational updates (`workout_sessions` + `workout_sets`).
- No independent secondary snapshot files that could cause state divergence.
- On reopening, the app detects any `IN_PROGRESS` session and prompts the user to resume.

---

## 2. Onboarding & Profiling Requirements

The onboarding wizard captures essential profile attributes:
1. **Primary Fitness Goal:** Aesthetic/V-Taper, Hypertrophy, Strength/Powerlifting, Fat Loss, Recomposition, Athletic Performance, General Longevity.
2. **Training Experience Level:** Beginner, Intermediate, Advanced.
3. **Weekly Training Frequency:** 2 to 6 days per week.
4. **Session Duration Target:** 30, 45, 60, 75, or 90+ minutes.
5. **Equipment Profile:** Commercial Gym, Home Gym, Dumbbells Only, Resistance Bands, Bodyweight, Custom Equipment increments.
6. **Optional Biometrics:** Bodyweight, height, body fat estimate (optional), body measurements.

---

## 3. Physique & Program Generation Engine

All program and progression algorithms reside in pure, testable Kotlin domain modules with zero UI or Android framework dependencies:
- **V-Taper:** Prioritizes lat width, lateral deltoids, upper chest, and core stability, while sustaining lower body compound strength.
- **Hypertrophy:** Balances volume across muscle groups within the optimal 10–20 weekly sets bracket at 1–3 RIR.
- **Strength:** Concentrates loading on major compound movements (Squat, Bench, Deadlift, Overhead Press) with low-to-moderate rep ranges (3–6 reps) and extended rest.
- **Time-Adaptive Sessions:** Dynamically compresses workouts when time is constrained (e.g. 40 minutes) using supersets without dropping primary compound work.
- **Travel / Home Mode:** Temporarily substitutes unavailable equipment without permanently altering the master training split.

---

## 4. Exercise Ecosystem & Knowledge Graph

- **Extensible Schema:** Supports canonical exercises, aliases, variations, primary/secondary muscles, equipment, movement patterns, form cues, and custom exercises.
- **Dataset Licensing Standard:** Ingest only text metadata from verified public-domain sources (`The Unlicense`). Zero third-party scraped images.
- **Instant Search Standard:** Exercise search must feel instantaneous on a realistic database containing thousands of exercises and aliases, backed by Room `@Fts4(tokenizer = unicode61)` with prefix matching.

---

## 5. Progressive Overload & Recovery Engine

- **Double Progression:** Ceiling reps must be achieved across all working sets with target RIR before incrementing weight.
- **Exercise Specificity:** Barbell compound micro-increments, dumbbell/isolation rep progression first, bodyweight leverage/volume progression.
- **Explainable Rationale:** Every recommendation states the exact reason for the load or rep target.
- **Fatigue & Deload Monitoring:** Identifies multi-session stagnation and suggests structured deloads.

---

## 6. Input Modalities: Parser, Voice & OCR

- **Deterministic Text Parser:** Converts arbitrary pasted text splits (`Bench 3x8`, `DB Incline 3x10-12`) into structured routines.
- **Native Voice Logging:** Uses Android native on-device `SpeechRecognizer` for gym utterances without cloud APIs.
- **On-Device OCR:** Uses on-device ML Kit Text Recognition for screenshot imports without cloud processing.

---

## 7. Progress Tracking & Transformation Passport

- **Quantitative Metrics:** Volume progression, estimated 1RM formulas (Brzycki, Epley, Wathan), PR records.
- **Weekly Standardized Physique Photos:** Local camera guides for front, side, and back poses stored in app-private storage.
- **6-Month Transformation Timeline:** Visual comparison between Week 0 and Week 26.

---

## 8. Semantic Design System & Gym Mode UX

- **Semantic Design Tokens:** All colors derive from tokens (`Background`, `Surface`, `SurfaceElevated`, `Primary`, `Secondary`, `Accent`, `TextPrimary`, `TextSecondary`, `Success`, `Warning`, `Error`, `Divider`).
- **High-Contrast Athletic Dark Theme:** Optimized for gym environments.
- **Gym Mode:** Large touch targets, screen wake-lock, automated rest timer with vibration/sound cues, plate loading calculator.

---

## 9. Phased Implementation Roadmap

- **Phase 0:** Research & Architecture Baseline (Approved).
- **Phase 1:** Foundation Only (Build setup, semantic tokens, shell navigation, Room DB, explicit migrations, repository interfaces, JVM tests, Compose previews).
- **Phase 2:** Exercise Ecosystem (800+ public-domain text dataset normalization, FTS search, substitutions).
- **Phase 3:** Workout Engine (Active gym session, transactional sets, rest timer, crash recovery).
- **Phase 4:** Intelligence Engine (Progressive overload, physique engine, program generation).
- **Phase 5:** Input Modalities (Text workout parser, voice logging, ML Kit OCR).
- **Phase 6:** Progress & Analytics (PR tracker, physique photo capture, 6-month transformation timeline).
- **Phase 7:** Integrations (Health Connect, biometric lock, notifications).
- **Phase 8:** Backup & Data Ownership (JSON, CSV, full ZIP export/restore).
- **Phase 9:** Gym Polish & Haptics.
- **Phase 10:** Release Audit & S21 FE On-Device Verification.
