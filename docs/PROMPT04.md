I am building an Android application called "Repair Store Manager MVP".

Phase 1 and Phase 2 have already been completed successfully.

Already implemented:

* Firebase Authentication
* Google Sign In
* Firestore
* Customer CRUD
* Repair Model
* Customer Search
* Customer Status Updates
* Notes Module
* Employee Notes Module
* Realtime Firestore Synchronization
* Koin DI
* MVVM
* Repository Pattern

I will provide all current source code.

Analyze the existing implementation first.

Reuse existing architecture.

Do NOT rewrite working features.

Do NOT modify Customer CRUD unless required for SMS integration.

Do NOT implement Media Storage.

Do NOT implement Bluetooth Printing.

Do NOT implement Room Database.

Do NOT implement Offline Support.

Focus ONLY on SMS functionality.

====================================================================
IMPORTANT SMS ARCHITECTURE
==========================

This application uses two devices.

Device A:

Main shop device.

Used for:

* Add customer
* Edit customer
* Change status
* Create notes

This device may NOT have a SIM card.

Device B:

Personal phone of shop owner.

Logged into same account.

Has SIM card.

Receives Firestore updates.

Responsible for sending SMS.

Only Device B should send SMS.

====================================================================
SMS SENDER DEVICE SYSTEM
========================

Implement a Firestore-based SMS sender device selection system.

Create collection:

appSettings

Document:

global

Fields:

smsSenderDeviceId

smsSenderDeviceName

updatedAt

Each device must generate and store a unique device ID.

Add a setting:

"Use this device for SMS sending"

When enabled:

Save current device ID to Firestore.

Only the selected device may send SMS.

All other devices must ignore SMS events.

====================================================================
SMS LOG SYSTEM
==============

Create Firestore collection:

smsLogs

Purpose:

Prevent duplicate SMS sending.

Document ID format:

{repairId}_{status}

Examples:

repair123_Pending

repair123_Delivered

Fields:

id

repairId

status

phoneNumber

message

sentAt

sentByDeviceId

success

Before sending SMS:

Check if smsLogs document already exists.

If exists:

Do not send.

If not exists:

Create log and send SMS.

Use Firestore transaction where necessary.

====================================================================
AUTOMATIC SMS TRIGGERS
======================

SMS should automatically send:

1. When a customer is created

Status:

Pending

2. When repair status changes to:

Pending

In Progress

Completed

Delivered

Cancelled

Each status should send only once.

====================================================================
SMS MESSAGE TEMPLATES
=====================

Create centralized SMS template provider.

Example:

Pending:

"Dear {customerName}, your device has been received successfully. Repair ID: {serialNumber}."

In Progress:

"Dear {customerName}, your repair is currently in progress. Repair ID: {serialNumber}."

Completed:

"Dear {customerName}, your device repair has been completed and is ready for delivery. Repair ID: {serialNumber}."

Delivered:

"Dear {customerName}, thank you for choosing our service. Repair ID: {serialNumber}."

Cancelled:

"Dear {customerName}, your repair request has been cancelled. Repair ID: {serialNumber}."

Templates should be easy to modify later.

====================================================================
SMS PERMISSIONS
===============

Implement:

SEND_SMS

READ_PHONE_STATE

READ_PHONE_NUMBERS if required

Runtime permission handling.

Graceful error handling.

====================================================================
SIM SELECTION
=============

Support dual SIM devices.

Allow shop owner to select SIM slot.

Store selected SIM in Firestore settings.

SMS sender device must use configured SIM.

====================================================================
AUTOMATIC SMS LISTENER
======================

Implement Firestore listener on repairs collection.

Only SMS sender device listens for SMS events.

When status changes:

Check smsLogs.

Send SMS if not already sent.

Create smsLogs record.

Prevent duplicates.

====================================================================
MANUAL SMS
==========

Add button:

Send SMS

Behavior:

Open SMS Intent

Prefill:

Customer name

Repair ID

Current status

Phone number

User may edit before sending.

====================================================================
ARCHITECTURE REQUIREMENTS
=========================

Use:

MVVM

Repository Pattern

Koin

StateFlow

Firestore

Keep implementation simple.

No WorkManager.

No Room.

No Offline Mode.

No unnecessary abstractions.

====================================================================
OUTPUT REQUIREMENTS
===================

Generate:

1. Firestore schema
2. SMS models
3. SMS repositories
4. Device ID management
5. SMS sender device system
6. SMS logs implementation
7. SMS templates
8. SIM selection implementation
9. Permission handling
10. Firestore listeners
11. ViewModels
12. DI updates
13. Navigation updates
14. Complete source code for all new files
15. Complete source code for all modified files

Return FULL FILES.

Do not return snippets.

Do not use placeholders.

All generated code must compile.
