I am building an Android application called "Repair Store Manager MVP".

Phase 1 has already been completed successfully.

The following features already exist and are working:

* Firebase Authentication
* Google Sign In
* Firestore Integration
* Koin Dependency Injection
* MVVM Architecture
* Repository Pattern
* Customer CRUD
* Repair Model
* Firestore Repair Collection
* Sequential Serial Number Generation
* Customer Search
* Customer Details
* Customer Status Updates
* Firestore Realtime Synchronization

I will provide all current source code files.

IMPORTANT:

Analyze my existing code first.

Reuse all existing architecture.

Do NOT rewrite any working functionality.

Do NOT modify Authentication.

Do NOT modify Customer CRUD.

Do NOT modify Repair Model unless absolutely necessary.

Do NOT modify Serial Number Generation.

Do NOT implement SMS.

Do NOT implement Media Storage.

Do NOT implement Bluetooth Printing.

Do NOT implement Room Database.

Do NOT implement WorkManager.

Do NOT implement Offline Support.

Only implement Notes and Employee Notes.

====================================================================
PHASE 2 GOAL
============

Implement two separate modules:

1. General Notes
2. Employee Notes

Both modules must use Firestore.

Both modules must support realtime synchronization across devices.

Keep implementation simple.

This application is temporary and does not require SaaS-level scalability.

====================================================================
UI REQUIREMENTS
===============

UI is NOT important.

Use basic Jetpack Compose Material 3 components.

No animations.

No custom design system.

No UI optimization.

No fancy layouts.

Only build enough UI to create, edit, delete, search, and view notes.

====================================================================
GENERAL NOTES MODULE
====================

Purpose:

Store business notes and reminders.

Examples:

* Buy Samsung display
* Order iPhone battery
* Call supplier tomorrow
* Customer requested charger
* Spare parts shopping list

Firestore Collection:

notes

Required Fields:

id

title

description

createdAt

updatedAt

createdBy

Requirements:

Create Note

Edit Note

Delete Note

View Notes

Search Notes

Realtime Firestore Sync

Validation:

Title required

Description optional

Use Firestore server timestamps.

====================================================================
EMPLOYEE NOTES MODULE
=====================

IMPORTANT:

This is NOT employee management.

No employee accounts.

No employee authentication.

No attendance tracking.

No salary tracking.

No permissions.

No role system.

This is simply a manual note book for recording repair jobs and profit.

Firestore Collection:

employeeNotes

Required Fields:

id

title

description

totalPayment

profit

createdAt

updatedAt

createdBy

Examples:

Title:
Samsung A54 Display Replacement

Description:
Customer requested original display.

Total Payment:
3500

Profit:
1200

Requirements:

Create Employee Note

Edit Employee Note

Delete Employee Note

View Employee Notes

Search Employee Notes

Realtime Firestore Sync

Validation:

Title required

Total Payment optional

Profit optional

Store payment and profit as numeric values.

Use Firestore server timestamps.

====================================================================
SEARCH REQUIREMENTS
===================

General Notes Search:

Search by:

* title
* description

Employee Notes Search:

Search by:

* title
* description

Client-side filtering is acceptable.

No complex Firestore search required.

====================================================================
FIRESTORE REQUIREMENTS
======================

Create only these collections:

notes

employeeNotes

Use server timestamps.

Minimize reads and writes.

Avoid unnecessary listeners.

Use snapshot listeners only where appropriate.

All changes must sync automatically between logged-in devices.

====================================================================
ARCHITECTURE REQUIREMENTS
=========================

Follow existing project architecture.

Use:

MVVM

Repository Pattern

Koin

StateFlow

Firestore

Compose Navigation

Material 3

Do NOT introduce:

UseCases

Clean Architecture layers

Feature modules

Complex abstractions

Keep code simple and maintainable.

====================================================================
NAVIGATION
==========

Current Bottom Navigation already exists.

Add:

Notes Screen

Employee Notes Screen

Implement navigation using existing navigation structure.

Keep navigation changes minimal.

====================================================================
OUTPUT REQUIREMENTS
===================

Generate:

1. Folder structure updates
2. Firestore schema
3. Notes model
4. Employee Notes model
5. Firestore repositories
6. ViewModels
7. UI State classes
8. Navigation updates
9. Koin dependency updates
10. Compose screens
11. Complete source code for every new file
12. Complete source code for every modified file

Return FULL FILES.

Do not return snippets.

Do not use placeholders.

Do not skip files.

All generated code must compile.

Analyze my existing code first and build on top of it instead of rewriting existing functionality.
