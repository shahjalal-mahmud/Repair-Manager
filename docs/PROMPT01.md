I am building an Android application called "Repair Store Manager MVP".

I will provide my existing source code, project structure, Product.md, and current Firebase implementation.

Your task is NOT to redesign the UI.

Your task is to implement a production-ready Firebase/Firestore architecture first.

IMPORTANT:

* UI is not important right now.
* Use the simplest possible Jetpack Compose UI.
* Use basic Material 3 components only.
* No animations.
* No fancy design.
* No complex reusable UI systems.
* No UI optimization unless required for functionality.
* Focus 95% on functionality and architecture.
* Every feature must work correctly before improving UI.

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

I will provide all existing project files.

You must analyze the existing implementation first and extend it.

DO NOT rewrite working authentication logic.

DO NOT rewrite working serial generation logic unless absolutely necessary.

Reuse existing architecture.

====================================================================
MAIN GOAL
=========

Build the complete MVP backend, Firestore integration, repositories, models, ViewModels, synchronization logic, local storage, and functionality.

UI should only be sufficient for testing.

====================================================================
ARCHITECTURE REQUIREMENTS
=========================

Follow:

MVVM

Repository Pattern

Koin Dependency Injection

StateFlow

Firestore

WorkManager

Room Database

Compose Navigation

Material 3

No Clean Architecture layers.

No UseCases.

No unnecessary abstractions.

Keep code simple and maintainable.

====================================================================
FREE FIREBASE TIER REQUIREMENTS
===============================

The application MUST be optimized for Firebase Spark Plan.

Avoid:

* excessive listeners
* unnecessary document reads
* duplicate writes
* polling
* redundant collections

Use snapshot listeners only where necessary.

Use Firestore transactions where consistency matters.

Design all Firestore operations to minimize reads and writes.

====================================================================
DATABASE DESIGN
===============

Create proper Firestore models for:

repairs

repairCounter

notes

employeeNotes

smsLogs

The structure must be scalable enough for future SaaS migration.

Every document should contain:

id
createdAt
updatedAt

Use server timestamps.

====================================================================
REPAIR MODEL
============

Extend current repair model.

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

SMS tracking fields

The repair must support future SaaS migration.

====================================================================
LOCAL MEDIA STORAGE
===================

Photos and videos MUST NOT be stored in Firestore.

Use local device storage only.

Store:

repairId

local file path

media type

Create local Room tables for media.

When another device logs in:

repair data syncs

media does not sync

This is expected behavior.

====================================================================
CUSTOMER MODULE
===============

Implement:

Create Customer

Edit Customer

Delete Customer

Customer Details

Customer Search

Customer Status Update

Customer List

Realtime Firestore Sync

Search by:

customer name

phone number

serial number

device model

Status options:

Pending

In Progress

Completed

Delivered

Cancelled

====================================================================
NOTES MODULE
============

Firestore collection:

notes

Fields:

id

title

description

createdAt

updatedAt

createdBy

Implement:

Create

Edit

Delete

List

Search

Realtime Sync

====================================================================
EMPLOYEE NOTES MODULE
=====================

This is NOT employee management.

No permissions.

No roles.

No authentication.

Only note taking.

Firestore collection:

employeeNotes

Fields:

id

title

description

totalPayment

profit

createdAt

updatedAt

createdBy

Implement:

Create

Edit

Delete

List

Search

Realtime Sync

====================================================================
SMS SYSTEM
==========

This is the MOST IMPORTANT PART.

Implement carefully.

Architecture:

Device A

No SIM required

Creates and updates repairs

Device B

Has SIM

Logged into same account

Receives Firestore updates

Automatically sends SMS

Requirements:

SMS must send only once.

No duplicate SMS.

Multiple devices may be logged in.

Only one successful SMS should exist.

Create smsLogs collection.

Store:

repairId

status

phoneNumber

message

sentAt

sentByDevice

success

Before sending:

check if log already exists

if exists

do not send again

Use Firestore transaction where necessary.

Implement:

Automatic SMS

Manual SMS Intent

Prefilled messages

Status-based templates

====================================================================
ROOM DATABASE
=============

Use Room only for:

media paths

SMS queue cache

offline support

Never duplicate all Firestore data locally.

Firestore remains source of truth.

====================================================================
OFFLINE STRATEGY
================

Enable Firestore offline persistence.

When internet returns:

sync automatically.

Prevent duplicate writes.

Prevent duplicate SMS.

====================================================================
PRINTING SYSTEM
===============

Prepare architecture for Bluetooth POS printing.

Implement:

printer selection

printer connection manager

invoice formatter

reprint support

Keep implementation modular.

Do not focus on UI.

====================================================================
WORKMANAGER
===========

Use WorkManager for:

SMS retry

Background synchronization

Pending queue processing

Avoid battery-heavy operations.

====================================================================
OUTPUT FORMAT
=============

Generate:

1. Folder structure
2. Firestore collections design
3. Room entities
4. Data models
5. Repository implementations
6. ViewModels
7. Navigation updates
8. Dependency Injection updates
9. Firestore security rules
10. Required permissions
11. Gradle dependency changes
12. Complete source code for every new file
13. Complete modifications for existing files

VERY IMPORTANT:

Return FULL FILES.

Do not return snippets.

Do not use placeholders.

Do not skip files.

Generate production-ready code that compiles.

If the response becomes too large:

continue automatically in the next response until all files are completed.
