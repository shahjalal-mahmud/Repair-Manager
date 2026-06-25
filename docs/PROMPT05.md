# Phase 4 – Bluetooth POS Printing Integration

I am building an Android application called **Repair Store Manager MVP**.

All major features are already implemented:

* Firebase Authentication
* Google Sign In
* Firestore
* Customer CRUD
* Customer Status Management
* Notes Module
* Employee Notes Module
* SMS System
* Realtime Synchronization
* MVVM
* Repository Pattern
* Koin DI

I will provide my current source code.

I will also provide my existing printer implementation from another project.

## IMPORTANT

My existing printer code already works with:

* PT-210
* GOOJPRT
* ESC/POS printers

DO NOT rewrite the existing printer connection logic unless absolutely necessary.

DO NOT replace my existing InvoiceFormatter logic.

DO NOT introduce third-party printing libraries.

Reuse my current implementation and adapt it to the new Repair Store Manager MVP architecture.

---

## GOAL

Implement complete Bluetooth POS printing support.

The printing system must support:

* Print after save
* Reprint later
* Printer selection
* Printer persistence
* Bluetooth permission handling
* Connection status
* Repair invoice formatting

UI is not important.

Functionality is the priority.

---

## EXISTING CODE

I will provide:

### POSPrinterHelper

Responsible for:

* Bluetooth connection
* Bluetooth socket management
* ESC/POS text printing

### InvoiceFormatter

Responsible for:

* Invoice text formatting

Reuse both implementations.

---

## PRINTER REQUIREMENTS

Supported printers:

* PT-210
* GOOJPRT
* Rongta
* XPrinter
* Generic ESC/POS Bluetooth printers

Connection method:

SPP UUID

```kotlin
00001101-0000-1000-8000-00805F9B34FB
```

Reuse existing connection approach.

---

## PRINTER SETTINGS

Create Firestore collection:

```text
appSettings
```

Document:

```text
global
```

Fields:

```kotlin
selectedPrinterName

selectedPrinterAddress

updatedAt
```

Purpose:

When shop owner changes phones:

Printer settings sync automatically.

---

## BLUETOOTH DEVICE DISCOVERY

Implement:

### Scan Paired Devices

Show all bonded Bluetooth printers.

Display:

* Device Name
* MAC Address

Allow:

Select Printer

Save Selection

---

## BLUETOOTH PERMISSIONS

Handle:

Android 12+

```kotlin
BLUETOOTH_CONNECT
BLUETOOTH_SCAN
```

Android 10-11

```kotlin
ACCESS_FINE_LOCATION
```

Runtime permission flow required.

Graceful error handling required.

---

## REPAIR INVOICE FORMAT

IMPORTANT

I want the EXACT SAME invoice style as my existing application.

Reuse the formatting structure.

Only map fields from my new Repair model.

Invoice must contain:

Store Name

Store Address

Store Phone

Repair Serial Number

Created Date

Status

Customer Name

Phone Number

Device Model

Problem Description

Expected Delivery Date

Payment Information

Security Information

Accessories

Dead Phone Permission

Additional Details

Box Number

Thank You Message

Keep the invoice width optimized for 48mm POS printers.

---

## INVOICE FORMATTER

Create:

```kotlin
InvoiceFormatter.kt
```

The formatter must remain independent from UI.

Input:

```kotlin
Repair
StoreInfo
```

Output:

```kotlin
String
```

The returned string must be printable directly using:

```kotlin
POSPrinterHelper.printText()
```

---

## PRINT WORKFLOW

### Save

Save customer only.

### Save & Print

Save customer

↓

Fetch created repair

↓

Connect printer

↓

Generate invoice

↓

Print invoice

↓

Show success or failure

---

## REPRINT WORKFLOW

Customer List

↓

Customer Details

↓

Print Button

↓

Reconnect Printer

↓

Generate Invoice

↓

Print Again

---

## STORE INFORMATION

Create Firestore collection:

```text
storeInfo
```

Document:

```text
main
```

Fields:

```kotlin
storeName

address

phone

updatedAt
```

Purpose:

Invoice header data. (In the firestore i will manually input all the store details)

---

## ARCHITECTURE REQUIREMENTS

Use existing architecture:

* MVVM
* Repository Pattern
* Koin
* StateFlow
* Firestore

Do NOT introduce:

* Clean Architecture
* UseCases
* Printing SDKs
* Complex abstractions

Keep implementation simple.

---

## OUTPUT REQUIREMENTS

Generate:

1. Printer Settings Firestore Schema
2. StoreInfo Model
3. Printer Models
4. Printer Repository
5. Bluetooth Device Discovery
6. Permission Handling
7. InvoiceFormatter adapted to Repair model
8. Print ViewModel
9. Koin Updates
10. Navigation Updates
11. Save & Print Flow
12. Reprint Flow
13. Complete New Files
14. Complete Modified Files

IMPORTANT:

Reuse my existing POSPrinterHelper.

Reuse my existing InvoiceFormatter style.

Do not redesign working code.

Return FULL FILES.

Do not return snippets.

All code must compile.

---
