# Repair Manager — Product Specification

Version: 2.0
Platform: Android (Kotlin)
Package: `com.appriyo.repairmanager`

---

## 1. Overview

Repair Manager is an Android application for mobile phone repair shops. It centralizes the entire repair workflow — from intake to delivery — into a single cloud-synced system accessible from any device logged into the shop's Google account.

The app is a production-grade operational tool used daily in real shops. It replaces paper ledgers, eliminates duplicate invoices, and keeps multiple shop devices in lockstep through Firebase.

### Goals

- Capture complete repair intake information in a single form.
- Issue unique, sequential repair serial numbers across all devices.
- Track device status from intake through delivery.
- Print repair receipts on Bluetooth POS printers.
- Notify customers via SMS when their device status changes.
- Synchronize all data in real time across multiple devices.
- Run entirely on the Firebase free tier.

### Non-Goals

The following are intentionally excluded and will live in a separate future SaaS product:

- Stock / inventory management
- Expense tracking and accounting
- Multi-store tenancy
- Web dashboard and customer portal
- WhatsApp integration
- Subscription billing

---

## 2. User Roles

There is a single role for this MVP: **Shop Owner / Staff**. All authenticated users have full read/write access to the shop's data. Role-based permissions are out of scope.

---

## 3. Authentication

- **Method**: Google Sign-In via Firebase Authentication.
- **Session**: Persistent. The user stays logged in across app restarts.
- **Multi-device**: Multiple devices may sign in with the same Google account. All devices see and write the same Firestore data.
- **Identity scoping**: Every Firestore document is scoped under `users/{uid}/...` via a `FirestoreUserProvider`, so each Google account sees only its own data.

---

## 4. Application Architecture

### High-Level

The app follows **MVVM with a Repository pattern**, wired together with **Koin** for dependency injection. UI is 100% Jetpack Compose with Material 3.

```
┌────────────────────────────────────────────────────────┐
│                    Presentation Layer                   │
│   Compose Screens  ←→  ViewModels  ←→  UiState          │
└──────────────────────────┬─────────────────────────────┘
                           │ StateFlow / suspend
┌──────────────────────────▼─────────────────────────────┐
│                      Data Layer                         │
│   Repositories  →  FirestoreUserProvider  →  Firebase   │
│                      │                                  │
│                      └──→ MediaStorageManager (local)   │
└────────────────────────────────────────────────────────┘
```

### Module Layout (source tree)

```
com.appriyo.repairmanager
├── data
│   ├── model         # Plain Kotlin data classes
│   ├── repository    # Firestore-backed repositories
│   ├── media         # Local photo/video storage
│   └── sms           # SMS auto-send + device-id + templates
├── di                # Koin modules
├── navigation        # NavGraph, Screen routes, bottom nav
├── presentation
│   ├── components    # Reusable composables
│   ├── screens       # Top-level screens
│   ├── state         # UiState classes
│   ├── utils         # Formatting helpers
│   └── viewmodel     # One ViewModel per screen
├── printing          # Bluetooth POS printer + invoice layout
├── ui/theme          # Material 3 theme
└── RepairManagerApp.kt, MainActivity.kt
```

### Architectural Rules

- **Unidirectional data flow**: Composables observe `StateFlow<UiState>` from ViewModels and emit user intents back.
- **Single source of truth**: `FirestoreUserProvider` exposes the current user ID; repositories use it to build per-user paths. No repository reads from a global user singleton.
- **Local-only media**: Photos and videos are stored in the app's private storage and never uploaded to Firestore.

---

## 5. Navigation

Implemented with **Jetpack Navigation Compose**. A single `NavGraph` declares every route and a `BottomNavigationBar` exposes the primary tabs.

Bottom navigation tabs:

| Tab        | Route          | Purpose                          |
|------------|----------------|----------------------------------|
| Home       | Dashboard      | At-a-glance shop summary         |
| Add        | AddRepair      | Create a new repair record       |
| Customers  | CustomerList   | Browse / search repair records   |
| Notes      | Notes          | Internal repair notes            |
| Employee   | EmployeeNotes  | Per-job work & profit log        |

Tali Khata (credit ledger) and SMS Settings are reachable from the dashboard, not the bottom bar.

---

## 6. Core Modules

### 6.1 Dashboard

Entry point after login. Shows summary counts (total, pending, in progress, completed, delivered) and quick links into Add Customer, Customer List, Tali Khata, and SMS Settings.

### 6.2 Add Customer / Repair

A single, structured intake form with the following sections:

**Customer Information**
- Customer name (required)
- Phone number (required, exactly 11 digits, e.g. `01712345678`)

**Device Information**
- Device model
- Problem description
- Expected delivery date
- Payment information (free text — `Paid 1000`, `Due 500`, `Advance 2000`, etc.)
- Additional details
- Box / drawer number

**Device Security** (one of)
- Password (text)
- Pattern lock (custom pattern input)

**Accessories Checklist** — Battery, SIM, Memory Card, SIM Tray, Back Cover.

**Customer Consent** — Dead Phone Permission acknowledgment.

**Media Attachments** — Multiple photos and videos captured from the device. Stored **locally only**, never synced to Firestore.

**Status** — Defaults to `Pending`. Selectable from the full status set.

**Actions**
- **Save** — Creates the record in Firestore.
- **Save & Print** — Creates the record and immediately opens the print flow.

### 6.3 Customer ID System

- Format: `RM-000001`, `RM-000002`, …
- Generated via a **Firestore transaction** against a counter document (`users/{uid}/counters/repairCounter`).
- Guarantees uniqueness across concurrent writes from multiple devices.
- The repair ID, serial number, and creation timestamp are immutable after creation.

### 6.4 Customers Module

**List view** shows serial, name, phone, device, status, and delivery date for every repair.

**Search** filters locally on cached data by:
- Customer name
- Phone number
- Serial number
- Device model

**Details view** shows the full record and exposes:
- **Edit** — opens the edit screen
- **Print** — reprints the receipt on the connected Bluetooth printer
- **Send SMS** — opens the system SMS intent with a pre-filled message
- **Status change** — triggers automatic SMS on supported transitions

**Edit screen** allows updating every field except `Repair ID`, `Serial Number`, and `Created Date`.

### 6.5 Status State Machine

```
Pending  →  In Progress  →  Completed  →  Delivered
   │             │              │
   └─────────────┴──────────────┴──────→  Cancelled
```

Any status transition into `In Progress`, `Completed`, `Delivered`, or `Cancelled` from a record that did not already have that status fires the SMS pipeline.

---

## 7. SMS System

A two-device architecture optimized for shops where the primary working tablet has no SIM.

**Device A — Workstation** (no SIM required)
- Creates customers, updates statuses, prints receipts.
- Writes all changes to Firestore.

**Device B — SMS Gateway** (has SIM, logged into same account)
- Listens to Firestore updates via `SmsAutoSendManager`.
- Sends the appropriate SMS for each status change.
- A persistent `smsLogs` collection records every sent message.

### Templates

`SmsTemplateProvider` centralizes the message text per status. Each template is editable from the in-app **SMS Settings** screen, so shop owners can tune wording without rebuilding the app.

### Duplicate Prevention

Each `smsLog` entry is keyed by `(repairId, status)`. Before sending, the SMS device checks the log; if an entry already exists for that pair, the message is skipped. This makes it safe to run multiple SMS devices concurrently.

### Manual SMS

From the customer details screen, **Send SMS** opens the platform SMS intent with a pre-filled message. The shop owner can review and edit before tapping send. This path bypasses the auto-send log.

---

## 8. Invoice Printing

### Hardware

Bluetooth POS printers are supported (GOOJPRT, Rongta, XPrinter, and other ESC/POS-compatible models). Pairing and selection happen through the Android Bluetooth settings and the in-app printer flow.

### Invoice Contents

- Shop name
- Repair ID and serial number
- Customer name and phone
- Device model
- Problem description
- Delivery date
- Status
- Payment information
- Security information
- Accessories checklist
- Created date

### Flow

- `InvoiceFormatter` produces a plain-text ESC/POS payload.
- `POSPrinterHelper` handles Bluetooth socket connection, write, and disconnect.
- `PrintViewModel` exposes `PrintUiState` (idle / connecting / printing / success / error) for the UI to observe.
- A connection-status indicator is shown in the print UI.

---

## 9. Notes Module

Internal notes for the shop. Each note has a title, description, creation date, and author. Full CRUD via `NotesRepository`. Notes are scoped per Google account.

---

## 10. Employee Notes Module

Lightweight per-job log for tracking work and profit. Each note has exactly **four fields**:

1. **Title** — brief description of the work
2. **Description** — detailed notes
3. **Total Payment** — amount charged to the customer
4. **Profit** — profit earned from the work

Example entries:

- *Fixed water damage on iPhone 12* — Total ৳3,000 / Profit ৳1,500
- *Replaced battery on Samsung S21* — Total ৳2,500 / Profit ৳800
- *Screen replacement for OnePlus 9* — Total ৳4,000 / Profit ৳1,200

Full CRUD via `EmployeeNotesRepository`. No employee management, roles, or permissions.

---

## 11. Tali Khata (Credit Ledger)

A simple credit ledger for tracking amounts owed to or by customers, surfaced from the dashboard. Built on `TaliKhataRepository` with dedicated summary cards, history list, search, and an add/edit dialog. Scoped per Google account.

---

## 12. Settings

- **SMS Settings** — per-status message templates, edited at runtime.
- **App Settings** — shop name and other preferences used by the invoice formatter, persisted via `AppSettingsRepository`.

---

## 13. Jetpack Compose Integration

The UI layer is **100% Compose**. There is no XML layout, no Fragment, no View binding.

Key Compose choices:

- **Material 3** theming (`ui/theme/`) with light/dark support.
- **Navigation Compose** (`navigation/`) — single `NavGraph` declares all routes; `Screen` is a sealed object of route constants.
- **State hoisting** — every screen takes a `UiState` and lambdas for events; ViewModels own the truth.
- **Lifecycle-aware state** — `collectAsStateWithLifecycle()` is used for `StateFlow` consumption in all screens.
- **Reusable components** — `SectionCard`, `StatusChip`, `RepairFormComponents`, `MediaCaptureSection`, `SendSmsButton`, `ConfirmationDialog`, `TopToast`, plus the full Tali Khata component set live in `presentation/components/`.
- **Koin Compose** — `koinViewModel()` resolves ViewModels inside composables without boilerplate.
- **Splash screen** — `androidx.core.splashscreen` for the cold-start experience.
- **Compose BOM** — versions are pinned through a single Compose BOM, with material-icons-extended for the icon set.

---

## 14. Firebase Integration

### Products used

- **Firebase Authentication** — Google Sign-In, persistent session.
- **Cloud Firestore** — sole cloud store. Real-time listeners feed the UI.
- **Play Services Auth** — drives the Google Sign-In UI.

### Data model

All documents are scoped under the signed-in user:

```
users/{uid}/
  ├── counters/
  │   └── repairCounter          # { lastSerial: N }
  ├── repairs/{repairId}         # full repair record
  ├── notes/{noteId}             # general notes
  ├── employeeNotes/{noteId}     # 4-field work log
  ├── taliKhata/{entryId}        # ledger entries
  ├── taliKhataHistory/{id}      # ledger history
  ├── smsLogs/{logId}            # { repairId, status, sentAt, sentByDeviceId }
  └── settings/
      ├── smsTemplates           # per-status message bodies
      └── appSettings            # shop name, etc.
```

### Multi-device consistency

- A single `FirestoreUserProvider` is injected into every repository, so per-user paths are derived from one source of truth and never duplicated.
- Real-time `snapshotFlow` listeners in the customer list and other screens react to writes from any device.
- Serial-number allocation uses a Firestore transaction, so concurrent intake on two devices cannot produce duplicates.
- `SmsLogRepository` ensures at-most-once SMS delivery for any (repair, status) pair.

### Security rules

For the MVP, any authenticated user can read and write their own data tree. Role-based rules are deferred to the future SaaS product.

---

## 15. Dependency Injection (Koin)

A single `appModule` (`di/AppModule.kt`) wires the entire object graph:

- **Singletons** — Firebase clients, `FirestoreUserProvider`, every repository, media managers, SMS infrastructure.
- **ViewModel factories** — one `viewModel { … }` block per screen.

Koin is started from `RepairManagerApp.onCreate()`. Screens resolve their ViewModels via `koinViewModel()`.

---

## 16. Build & Tooling

- **compileSdk / targetSdk**: 36
- **minSdk**: 24
- **Kotlin JVM target**: 17
- **Release builds**: R8 with resource shrinking enabled.
- **Dependency catalogs**: `libs.versions.toml` pins Compose BOM, Firebase BOM, Koin BOM, Navigation, and Play Services Auth.

---

## 17. Success Criteria

The product is considered complete when:

- A shop owner can sign in with Google and stay signed in across restarts.
- New repair records can be created and saved in under 2 seconds.
- Serial numbers are unique across all devices, with no duplicates possible.
- Status changes appear on every signed-in device in real time.
- Receipts can be printed to a paired Bluetooth POS printer.
- Automatic SMS fires on supported status transitions, and manual SMS opens the system composer with a pre-filled message.
- Duplicate SMS sending is impossible, even with multiple SMS devices online.
- The app runs comfortably on the Firebase free tier for a single shop.
