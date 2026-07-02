# RepairManager

A cloud-synced Android application that runs a mobile phone repair shop — from intake to delivery, invoice printing, and customer SMS — across as many devices as the shop owns.

RepairManager replaces paper ledgers with a single real-time system backed by Firebase. Shop staff capture customer and device information on one tablet, print receipts on a Bluetooth POS printer, and a second device with a SIM can act as an automatic SMS gateway, all reading and writing the same data through Google Sign-In.

> See [`product.md`](./product.md) for the full product specification.

---

## Tech Stack

| Layer        | Technology                                                                     |
|--------------|--------------------------------------------------------------------------------|
| Language     | Kotlin 2.0.21                                                                  |
| UI           | Jetpack Compose (BOM `2026.04.01`) + Material 3                                |
| Navigation   | AndroidX Navigation Compose 2.8.9                                              |
| Architecture | MVVM + Repository pattern                                                      |
| DI           | Koin 4.1.1 (BOM-managed) — `koin-android` + `koin-androidx-compose`            |
| Async        | Kotlin Coroutines + `StateFlow` (collected with `collectAsStateWithLifecycle`) |
| Auth         | Firebase Authentication — Google Sign-In via Play Services Auth 21.3.0         |
| Backend      | Cloud Firestore (Firebase BOM 34.9.0) — real-time listeners + transactions     |
| Local media  | App-private storage (photos/videos, never synced)                              |
| Printing     | Bluetooth POS printers (ESC/POS)                                               |
| Build        | Android Gradle Plugin 8.13.2 · Kotlin Compose Compiler plugin · JVM 17         |
| SDK          | `minSdk` 24 · `targetSdk` / `compileSdk` 36                                    |

Dependency versions are pinned in `gradle/libs.versions.toml`; Compose, Firebase, and Koin are imported through their respective BOMs.

---

## Core Features

- **Repair intake form** — customer info, device details, security (password or pattern), accessories checklist, dead-phone consent, and local-only photo/video attachments.
- **Unique serial counter** — `RM-000001`, `RM-000002`, … generated through a Firestore transaction, so two devices creating records simultaneously never produce duplicates.
- **Real-time customer list** — searchable by name, phone, serial, or device model. Status moves through `Pending → In Progress → Completed → Delivered` (or `Cancelled`).
- **Bluetooth POS printing** — one-tap "Save & Print" produces a text receipt on GOOJPRT, Rongta, XPrinter, and other ESC/POS-compatible printers.
- **Two-device SMS gateway** — a workstation with no SIM creates repairs; a paired phone with a SIM listens to Firestore and sends per-status SMS automatically, deduplicated through an `smsLogs` collection.
- **Tali Khata (credit ledger) + Notes + Employee Notes** — lightweight internal tools for tracking customer balances, general notes, and per-job work & profit.

---

## Project Architecture

The app follows **MVVM** with a **Repository** layer, wired with **Koin**. The UI is 100% Jetpack Compose — no XML layouts, no Fragments, no View binding.

```
┌──────────────────────────────────────────────────────────┐
│                  Presentation (Compose)                    │
│   Screens  ←→  ViewModels  ←→  UiState (StateFlow)        │
└──────────────────────────┬───────────────────────────────┘
                           │ suspend / StateFlow
┌──────────────────────────▼───────────────────────────────┐
│                       Data Layer                          │
│   Repositories → FirestoreUserProvider → Firebase         │
│                  │                                        │
│                  └──→ MediaStorageManager (local-only)     │
└──────────────────────────────────────────────────────────┘
```

### Package layout

```
com.appriyo.repairmanager
├── data
│   ├── model         # Plain Kotlin data classes (Repair, Note, EmployeeNote, …)
│   ├── repository    # Firestore-backed repositories
│   ├── media         # Local photo / video storage
│   └── sms           # Auto-send manager, device id, templates
├── di                # Koin modules (appModule)
├── navigation        # NavGraph, Screen routes, bottom nav items
├── presentation
│   ├── components    # Reusable composables (SectionCard, StatusChip, …)
│   ├── screens       # Top-level screens (Dashboard, AddRepair, …)
│   ├── state         # UiState classes
│   ├── utils         # Formatting helpers
│   └── viewmodel     # One ViewModel per screen
├── printing          # InvoiceFormatter + POSPrinterHelper (Bluetooth ESC/POS)
├── ui/theme          # Material 3 theme (colors, type)
└── RepairManagerApp.kt, MainActivity.kt
```

**Architectural rules**

- Unidirectional data flow: composables observe `StateFlow<UiState>` and emit intents back to the ViewModel.
- One `FirestoreUserProvider` resolves the current user ID; every repository derives per-user paths from it.
- Photos and videos stay on-device — they never touch Firestore.

---

## Getting Started

### Prerequisites

- **Android Studio** (Hedgehog `2023.1.1` or newer recommended; required for AGP 8.13.x).
- **JDK 17** (the project compiles with `sourceCompatibility` / `targetCompatibility` / `kotlinOptions.jvmTarget` all set to 17).
- An Android device or emulator running **API 24 (Android 7.0)** or higher.

### Open the project

1. Clone the repository:

   ```bash
   git clone <your-repo-url> RepairManager
   cd RepairManager
   ```

2. **Open in Android Studio:** `File → Open…` and select the project root. Android Studio will run a Gradle sync automatically.

3. **Configure `local.properties`:**
   Copy `local.properties.example` to `local.properties` and set the path to your Android SDK:

   ```properties
   sdk.dir=/absolute/path/to/Android/sdk
   ```

4. **Firebase setup:**
   - The repository ships with a `google-services.json` already wired into `app/build.gradle.kts` via the `com.google.gms.google-services` plugin.
   - To use your own Firebase project, replace `app/google-services.json` with one downloaded from the [Firebase console](https://console.firebase.google.com/), enable **Authentication → Google**, and create a **Cloud Firestore** database in production or test mode.

5. **Build & run:**
   Select the `app` run configuration and an attached device or emulator, then press **Run ▶**. Sign in with the Google account associated with your Firebase project to start using the app.

### Useful Gradle commands

```bash
./gradlew assembleDebug          # build a debug APK
./gradlew installDebug           # build + install on the connected device
./gradlew lint                   # run Android lint
./gradlew test                   # run unit tests
```

---

## Documentation

- [`product.md`](./product.md) — full product specification (features, data model, success criteria).
- [`APP.md`](./APP.md) — feature checklist and MVP scope notes.
- [`docs/`](./docs) — additional design notes and references.

## License

This project is private/internal. Add a `LICENSE` file to publish under specific terms.
