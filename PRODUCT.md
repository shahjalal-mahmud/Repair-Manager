# Repair Manager

## Product Overview

Repair Manager is a lightweight Android application designed for mobile phone repair shops to track customer devices during the repair process.

The application serves as a temporary production solution while the full SaaS version is under development.

The primary goal is to provide a simple, reliable, and real-time system for recording customer repair information, generating repair invoices, printing receipts, and tracking device delivery status.

The application is intentionally designed to be simple and focused. It is not intended to become the foundation of the future SaaS platform and will be developed separately from scratch.

---

# Problem Statement

Many small repair shops currently track repair devices using paper notebooks or manual records.

Common issues include:

* Lost repair records
* Difficulty finding customer information
* Duplicate invoices
* No synchronization between multiple devices
* Difficulty tracking delivered and pending devices

Repair Manager solves these issues by providing a centralized cloud-based tracking system using Firebase.

---

# Goals

## Primary Goals

* Store customer repair records
* Generate unique repair serial numbers
* Print repair invoices using Bluetooth POS printers
* Search customer records quickly
* Track repair status
* Sync data across multiple devices in real time

## Secondary Goals

* Support multiple devices using the same account
* Reduce paperwork
* Maintain historical repair records
* Work entirely on Firebase Free Tier

---

# Target Users

### Current Target

Single repair shop owner and employees.

### Future Possibility

Additional repair shops may use the application in the future, but this is not a current requirement.

---

# Technology Stack

## Android

* Kotlin
* Jetpack Compose
* Material 3

## Architecture

* MVVM
* Koin Dependency Injection

## Backend Services

* Firebase Authentication
* Cloud Firestore

## Printing

* Bluetooth POS Printer
* ESC/POS Text Printing

---

# Authentication

## Sign In Method

Google Sign-In only.

No email/password authentication.

## Requirements

* User signs in using Google Account.
* Same Google account can be used on multiple devices.
* All devices access the same data.
* Data synchronization handled through Firestore.

---

# Core Features

## 1. Add Customer Repair Record

Shop owner can create a repair entry.

### Required Fields

* Serial Number (Auto Generated)
* Customer Name
* Phone Number
* Device Name / Model
* Problem Description
* Expected Delivery Date
* Payment Information
* Repair Status

### Default Status

Pending

---

## 2. Unique Serial Number Generation

Every repair entry receives a unique serial number.

### Format

RM-000001

Examples:

* RM-000001
* RM-000002
* RM-000003

### Requirements

* Must remain unique across all devices.
* Multiple devices may create records simultaneously.
* Firestore Transaction must be used.
* Latest serial number stored centrally in Firestore.
* Prevent duplicate serial generation.

### Firestore Counter Example

/counters/repairCounter

Fields:

lastSerial: 152

When a new repair is created:

1. Start Firestore Transaction.
2. Read current counter.
3. Increment counter.
4. Save counter.
5. Generate serial.
6. Create repair record.

This guarantees uniqueness.

---

## 3. Invoice Printing

After saving a repair record:

* Generate printable invoice.
* Connect to Bluetooth POS printer.
* Print repair receipt.

### Invoice Information

* Shop Name
* Invoice Serial Number
* Customer Name
* Phone Number
* Device Name
* Problem Description
* Delivery Date
* Payment Information
* Current Status
* Created Date

### Notes

Text-only invoice.

No images or logos required.

---

## 4. Customer Search

Search customer records using:

* Customer Name
* Phone Number
* Device Name
* Serial Number

### Requirements

Search should be fast and simple.

Firestore data may be cached locally for filtering.

---

## 5. Repair Status Management

### Status Types

* Pending
* Repaired
* Delivered
* Cancelled

### Requirements

Status can be changed from customer details screen.

---

## 6. Customer Details Screen

Displays complete repair information.

### Information

* Serial Number
* Customer Name
* Phone Number
* Device Name
* Problem
* Delivery Date
* Payment Information
* Status
* Created Date

### Actions

* Edit Information
* Update Status
* Reprint Invoice

---

# Application Screens

## Splash Screen

Responsibilities:

* Check authentication state.
* Navigate accordingly.

---

## Login Screen

Features:

* Google Sign In Button

---

## Dashboard Screen

Summary information:

* Total Repairs
* Pending Repairs
* Repaired Devices
* Delivered Devices

Quick Actions:

* Add Repair
* Search Customer

---

## Add Repair Screen

Create new repair record.

Contains:

* Customer Information Form
* Device Information Form
* Save Button

After Save:

* Generate Serial
* Save to Firestore
* Print Invoice

---

## Customer List Screen

Displays all repair records.

Features:

* Search
* Filter by Status

---

## Customer Details Screen

Displays complete repair details.

Actions:

* Edit
* Update Status
* Reprint Invoice

---

# Firestore Database Design

## Collection

repairs

### Document Structure

```json
{
  "id": "auto_generated",
  "serialNumber": "RM-000001",
  "customerName": "John Doe",
  "phoneNumber": "+8801XXXXXXXXX",
  "deviceName": "Samsung A34",
  "problem": "Display Issue",
  "expectedDeliveryDate": "2026-06-20",
  "paymentInfo": "Advance 500",
  "status": "Pending",
  "createdAt": "timestamp",
  "updatedAt": "timestamp",
  "createdBy": "uid"
}
```

---

## Counter Collection

counters

### Document

repairCounter

```json
{
  "lastSerial": 152
}
```

---

# Offline Support

Not required.

### Reason

Serial number generation depends on Firestore transactions.

The application must have internet access.

Users should be informed when there is no internet connection.

---

# Security Rules

Basic Firestore Rules

Current version:

Authenticated users can read and write.

Since this application is intended for a single shop initially, complex role management is unnecessary.

Example:

```javascript
rules_version = '2';

service cloud.firestore {
 match /databases/{database}/documents {

   match /{document=**} {
     allow read, write: if request.auth != null;
   }
 }
}
```

---

# Non-Functional Requirements

## Performance

* Record creation under 2 seconds
* Search results instantly filtered
* Invoice printing under 5 seconds

## Reliability

* No duplicate serial numbers
* Real-time synchronization
* Automatic Firestore updates

## Cost

Must operate entirely within Firebase Free Tier limits.

---

# Out of Scope

The following features are intentionally excluded:

* Inventory Management
* Stock Tracking
* Employee Management
* Customer SMS
* WhatsApp Integration
* Expense Tracking
* Accounting
* Payment Gateway
* Multi-tenant SaaS Architecture
* Analytics Dashboard
* Image Uploads
* Device Photos
* Backend Server
* PostgreSQL
* Spring Boot

These features will be part of the future SaaS product and are not required for this temporary solution.

---

# Future SaaS Version

The future version will be developed separately and independently.

Potential technologies:

* Spring Boot
* PostgreSQL
* REST API
* Multi-Tenant Architecture
* Inventory Module
* Employee Module
* Billing Module
* Customer Notifications
* Web Dashboard
* Subscription Management

The current Repair Manager application is only intended as a temporary operational tool during development of the full SaaS platform.

---

# Success Criteria

The project will be considered successful when:

* Shop owner can sign in using Google.
* New repair records can be created.
* Unique serial numbers are generated without duplication.
* Repair invoices can be printed.
* Records sync across multiple devices.
* Customers can be searched easily.
* Devices can be marked as delivered.
* Application runs entirely on Firebase Free Tier.
* Shop owner can use it daily without manual record keeping.

```
```
