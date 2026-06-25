# Repair Store Manager MVP

Version: 1.0

## Overview

Repair Store Manager MVP is a lightweight Android application designed for mobile phone repair shops to manage customer repair records, track repair status, print receipts, and send customer notifications.

This MVP is intended as a temporary production-ready solution while the full SaaS version is being developed.

The application must be simple, fast, reliable, and optimized for daily repair shop operations.

---

# Goals

## Primary Goals

* Add and manage repair customers.
* Generate unique repair IDs automatically.
* Track repair progress through statuses.
* Print customer receipts using Bluetooth POS printers.
* Send SMS updates to customers.
* Support multiple shop devices under the same account.
* Synchronize repair records through Firebase Firestore.

## Non-Goals (Future SaaS Features)

The following are NOT part of the MVP:

* Stock Management
* Expense Tracking
* Income Reports
* WhatsApp Integration
* Customer Portal
* Web Dashboard
* Analytics
* Multi-Store Management
* Cloud Photo Storage
* Subscription Billing

---

# User Roles

## Shop Owner

Can:

* Add customers
* Edit customers
* Update repair status
* Send SMS
* Print receipts
* Create notes
* Create employee notes

---

# Authentication

## Login

* Google Sign In
* Firebase Authentication

## Session

* Stay logged in
* Auto-login on app restart

---

# Navigation

Bottom Navigation contains four tabs:

## 1. Add Customer

Create new repair records.

## 2. Customers

View and manage repair records.

## 3. Notes

Store repair-related notes.

## 4. Employee Notes

Manual note-taking for employee activities with only 4 fields.

---

# Add Customer Module

## Required Fields

### Customer Information

* Customer Name (Required)
* Phone Number (Required)
* Device Model

Validation:

* Phone number must be exactly 11 digits.

Example:

01712345678

---

## Repair Information

* Problem Description
* Expected Delivery Date
* Payment Information
* Additional Details
* Box / Drawer Number

Payment Information is free text.

Examples:

Paid 1000
Due 500
Advance 2000
Paid 3000 via bKash

---

## Device Security

### Password

Text Input

### Pattern Lock

Pattern Input Component

Only one security type may be selected.

---

## Accessories Checklist

Checkboxes

* Battery
* SIM
* Memory Card
* SIM Tray
* Back Cover

---

## Customer Consent

Checkboxes

* Dead Phone Permission

Purpose:

Confirms customer understands risks when repairing dead devices.

---

## Media Attachment

### Photos

* Multiple images
* Saved locally only

### Videos

* Multiple videos
* Saved locally only

Not uploaded to Firestore.

---

## Customer Status

Default:

Pending

Available statuses:

* Pending
* In Progress
* Completed
* Delivered
* Cancelled

---

## Actions

### Save

Creates repair record.

### Save & Print

Creates repair record and opens invoice printing.

---

# Customer ID System

## Auto Generated Serial

Format:

RM-000001
RM-000002
RM-000003

Requirements:

* Sequential
* Unique
* Generated using Firestore transaction
* No duplicate IDs across devices

---

# Customers Module

## Customer List

Display:

* Serial Number
* Customer Name
* Phone Number
* Device Model
* Status
* Delivery Date

---

## Search

Search by:

* Customer Name
* Phone Number
* Serial Number
* Device Model

---

## Customer Details

Show all information stored during creation.

---

## Edit Customer

Editable:

* Name
* Phone Number
* Device Model
* Problem
* Payment Info
* Delivery Date
* Additional Details
* Accessories
* Consent
* Status

Not Editable:

* Repair ID
* Serial Number
* Created Date

---

## Customer Actions

### Print

Reprint invoice.

### Send SMS

Manual SMS trigger.

### Edit

Update customer information.

---

# SMS System

## Architecture

Two-device workflow:

### Device A

Main working device.

* Creates customers
* Updates statuses

May not have SIM card.

### Device B

SMS device.

* Logged into same account
* Has SIM card
* Receives Firestore updates
* Sends SMS automatically

---

## Automatic SMS Triggers

When first created and
When status changes:

Pending
In Progress
Completed
Delivered
Cancelled

SMS sent automatically.

---

## Duplicate Prevention

Requirements:

* SMS must be sent only once per status.
* Multiple devices must not send duplicate messages.
* Firestore tracking required.

Example:

Delivered SMS already sent.

Another device must not send Delivered SMS again.

---

## Manual SMS

Button:

Send SMS

Behavior:

Opens SMS intent with prefilled message.

Shop owner can:

* Review message
* Edit message
* Send manually

---

# Invoice Printing

## Printer Type

Bluetooth POS Printer

Examples:

* GOOJPRT
* Rongta
* XPrinter

---

## Invoice Contents

* Shop Name
* Repair ID
* Customer Name
* Phone Number
* Device Model
* Problem Description
* Delivery Date
* Status
* Payment Information
* Security Information
* Accessories
* Created Date

---

## Features

* Print after save
* Reprint anytime
* Bluetooth device selector
* Connection status indicator

---

# Notes Module

Purpose:

Store internal repair notes.

Each note contains:

* Title
* Description
* Created Date
* Created By

Features:

* Create
* Edit
* Delete

---

# Employee Notes Module

## Purpose

Simple manual note-taking for tracking employee activities.

## Note Fields

Each employee note contains exactly 4 fields:

1. **Title** - Brief description of the work done
2. **Description** - Detailed notes about the work
3. **Total Payment** - Amount charged to the customer for the device repair
4. **Profit** - Profit earned from this work

## Usage Examples

- "Fixed water damage on iPhone 12"
    - Total Payment: ৳3,000
    - Profit: ৳1,500

- "Replaced battery on Samsung S21"
    - Total Payment: ৳2,500
    - Profit: ৳800

- "Screen replacement for OnePlus 9"
    - Total Payment: ৳4,000
    - Profit: ৳1,200

## Features

- Create new notes
- View all notes
- Edit existing notes
- Delete notes
- Simple and straightforward interface
- No employee management or permissions needed

---

# Firestore Collections

## repairs

Stores repair records.

## repairCounter

Stores last serial number.

## notes

Stores general notes.

## employeeNotes

Stores employee work notes with 4 fields.

## smsLogs

Tracks sent SMS records.

Purpose:

Prevent duplicate SMS sending.

---

# MVP Success Criteria

The MVP is considered complete when:

* Customer creation works.
* Serial generation works.
* Firestore synchronization works.
* Customer editing works.
* Status updates work.
* Automatic SMS works.
* Manual SMS works.
* Bluetooth printing works.
* Search works.
* Notes work.
* Employee notes work with 4 fields.
* Multiple devices can use the same account without data conflicts.

---

# Technical Stack

## Frontend

* Kotlin
* Jetpack Compose
* Material 3

## Architecture

* MVVM
* Repository Pattern
* Koin Dependency Injection

## Backend

* Firebase Authentication
* Firebase Firestore

## Local Storage

* Room Database
* Media Storage (Photos & Videos)

## Background Tasks

* WorkManager

## Printing

* Bluetooth POS Printer

## Notifications

* Android SMS Manager
* Firestore Sync Events

---

# Future SaaS Upgrade Path

The MVP data structure must be designed so that it can later migrate into the full SaaS platform without requiring data loss or major schema changes.

Future modules:

* Stock Management
* Financial Reports
* WhatsApp Messaging
* Web Dashboard
* Customer Portal
* AI Repair Assistant
* Multi-Store Support
* Subscription System