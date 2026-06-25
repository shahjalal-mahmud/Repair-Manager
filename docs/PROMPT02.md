I am building an Android application called "Repair Store Manager MVP".

I will provide my existing source code, project structure, Product.md, and current Firebase implementation.

Your task is to extend the existing project and implement ONLY Phase 1.

DO NOT implement SMS.

DO NOT implement Notes.

DO NOT implement Employee Notes.

DO NOT implement Room Database.

DO NOT implement Media Storage.

DO NOT implement Bluetooth Printing.

DO NOT implement WorkManager.

DO NOT implement Offline Support.

Those features will be implemented in later phases.

Focus only on Repair Model, Firestore Schema, and Customer Management.

====================================================================
IMPORTANT
=========

UI is NOT important.

Use the simplest possible Jetpack Compose UI.

Use basic Material 3 components.

No animations.

No fancy design.

No custom design system.

No UI optimization.

Focus entirely on functionality.

The UI only needs to be usable for testing.

====================================================================
CURRENT PROJECT
===============

Already implemented:

* Kotlin
* Jetpack Compose
* MVVM
* Repository Pattern
* Koin DI
* Firebase Authentication
* Google Sign In
* Firestore
* Sequential serial generation using Firestore transaction
* Basic Add Customer functionality

I will provide all current source code files.

Analyze the existing implementation before making changes.

Reuse all working code.

DO NOT rewrite authentication.

DO NOT rewrite serial generation.

DO NOT change existing architecture unnecessarily.

====================================================================
PROJECT REQUIREMENTS
====================

This application is temporary.

It is NOT intended to become a SaaS.

It does NOT need future enterprise scalability.

Keep the implementation simple.

Keep Firestore structure clean and maintainable.

Use Firebase Spark Plan only.

Minimize Firestore reads and writes.

Avoid unnecessary listeners.

Avoid unnecessary collections.

====================================================================
PHASE 1 GOAL
============

Implement a complete Customer Management System.

The following must work correctly:

* Create Customer
* Edit Customer
* Delete Customer
* Customer Details
* Customer List
* Search Customers
* Status Updates
* Firestore Synchronization Across Devices

====================================================================
REPAIR MODEL
============

Extend the current Repair model.

Required fields:

id

serialNumber

customerName

phoneNumber

deviceModel

problemDescription

expectedDeliveryDate

paymentInfo

additionalDetails

boxNumber

status

securityType

password

pattern

batteryIncluded

simIncluded

memoryCardIncluded

simTrayIncluded

backCoverIncluded

deadPhonePermission

photoCount

videoCount

createdAt

updatedAt

createdBy

Validation:

customerName:
required

phoneNumber:
required
exactly 11 digits

status default:
Pending

Status options:

Pending

In Progress

Completed

Delivered

Cancelled

====================================================================
FIRESTORE DESIGN
================

Only use:

repairs

repairCounter

collections.

Do NOT create notes collections.

Do NOT create employee collections.

Do NOT create sms collections.

Use Firestore server timestamps.

Keep Firestore schema simple.

Ensure synchronization works correctly between multiple logged-in devices.

When one device updates a repair:

all other devices should receive updated data automatically.

====================================================================
ADD CUSTOMER
============

Implement full customer creation.

Required:

Customer Name

Phone Number

Optional:

Device Model

Problem Description

Expected Delivery Date

Payment Information

Additional Details

Box Number

Security Type

Password

Pattern

Accessories

Consent

Status

Save all data to Firestore.

Keep serial generation using existing transaction logic.

====================================================================
CUSTOMER LIST
=============

Display:

Serial Number

Customer Name

Phone Number

Device Model

Status

Delivery Date

Implement realtime Firestore updates.

Implement search.

Search by:

Customer Name

Phone Number

Serial Number

Device Model

Search can be client-side filtering.

No need for complex Firestore search.

====================================================================
CUSTOMER DETAILS
================

Create a details screen.

Display all repair information.

Show complete customer record.

====================================================================
EDIT CUSTOMER
=============

Allow editing:

Customer Name

Phone Number

Device Model

Problem Description

Expected Delivery Date

Payment Information

Additional Details

Box Number

Status

Accessories

Consent

Security Information

Do NOT allow editing:

id

serialNumber

createdAt

createdBy

====================================================================
DELETE CUSTOMER
===============

Implement delete functionality.

Require confirmation before delete.

Delete Firestore document.

====================================================================
STATUS UPDATE
=============

Allow status update from:

Customer List

Customer Details

Status options:

Pending

In Progress

Completed

Delivered

Cancelled

Changes must sync across all logged-in devices.

====================================================================
ARCHITECTURE REQUIREMENTS
=========================

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

Keep code simple.

====================================================================
OUTPUT REQUIREMENTS
===================

Generate:

1. Updated folder structure
2. Updated Firestore schema
3. Updated Repair model
4. Repository changes
5. ViewModels
6. UI State classes
7. Navigation updates
8. DI updates
9. Firestore queries
10. Complete source code for all new files
11. Complete source code for all modified files

Return FULL FILES.

Do not return snippets.

Do not use placeholders.

Do not skip files.

All generated code must compile.

Analyze my existing code first and build on top of it rather than rewriting everything.
