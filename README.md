# 🚖 Cab Booking System — Java OOP Project

A fully-featured, console-based **Cab Booking System** built in Java that demonstrates **15 core Object-Oriented Programming concepts** through a real-world ride-hailing simulation. The system covers everything from basic booking and ride tracking to surge pricing, wallet payments, and persistent file I/O — all in a single, well-structured Java file.

---

## 📋 Table of Contents

- [Features](#-features)
- [OOP Concepts Demonstrated](#-oop-concepts-demonstrated)
- [System Architecture](#-system-architecture)
- [Design Patterns](#-design-patterns)
- [Project Structure](#-project-structure)
- [Getting Started](#-getting-started)
- [Demo Walkthrough](#-demo-walkthrough)
- [Exception Handling](#-exception-handling)
- [File I/O Persistence](#-file-io-persistence)
- [Sample Output](#-sample-output)

---

## ✨ Features

| Feature | Description |
|---|---|
| 🚗 **Multi-vehicle Booking** | Book rides across Bike, Auto, Sedan, and SUV vehicle types |
| 💰 **Dynamic Fare Calculation** | Per-vehicle fare strategies with distance-based pricing |
| ⚡ **Surge Pricing** | Toggle surge multiplier on/off at runtime using the Decorator Pattern |
| 👛 **Wallet Payments** | In-app wallet with recharge, deduction, and balance tracking |
| 🔔 **Real-time Notifications** | Observer-based SMS and push notifications for riders and drivers |
| 📍 **Ride Tracking** | Full ride lifecycle management (REQUESTED → ASSIGNED → IN_PROGRESS → COMPLETED) |
| 🧾 **Invoice Generation** | Detailed receipt showing base fare, surge premium, tax (5%), and platform fee |
| ❌ **Ride Cancellation** | Cancel bookings with driver availability restoration |
| 📁 **Persistent History** | Save and reload ride history to/from `ride_history.txt` |
| 🚨 **Robust Error Handling** | 5 custom checked exceptions covering all failure scenarios |

---

## 🧠 OOP Concepts Demonstrated

This project was built as a comprehensive demonstration of OOP principles. Every concept is intentionally wired into the application logic — not just present for show.

| # | Concept | Where Applied |
|---|---|---|
| 1 | **Inheritance** | `Rider` and `Driver` both extend the abstract `User` class |
| 2 | **Polymorphism** | `getRole()` is overridden in `Rider` (→ `"RIDER"`) and `Driver` (→ `"DRIVER"`) |
| 3 | **Abstract Class** | `User` is abstract; forces all subclasses to implement `getRole()` |
| 4 | **Encapsulation** | All fields are `private`; access is controlled via getters and setters |
| 5 | **Interfaces** | 5 interfaces: `Bookable`, `Trackable`, `Payable`, `FareStrategy`, `RideObserver` |
| 6 | **Generics** | `RideHistory<T extends Booking>` and `BookingQueue<T>` use bounded type parameters |
| 7 | **Collections** | `HashMap`, `ArrayList`, `LinkedList` used throughout the engine |
| 8 | **Exception Handling** | 5 custom checked exceptions with proper `try-catch` propagation |
| 9 | **Singleton Pattern** | `CabBookingEngine.getInstance()` ensures one engine instance |
| 10 | **Strategy Pattern** | Fare calculation is swappable via `BikeFareStrategy`, `AutoFareStrategy`, etc. |
| 11 | **Observer Pattern** | `Rider` and `Driver` both implement `RideObserver`; notified on every status change |
| 12 | **File I/O** | `RideHistoryFileManager` uses `BufferedWriter` / `BufferedReader` for persistence |
| 13 | **Builder Pattern** ⭐ | `Booking.Builder` provides a fluent API to construct complex `Booking` objects |
| 14 | **Decorator Pattern** ⭐ | `SurgePricingDecorator` wraps any `FareStrategy` to apply a surge multiplier at runtime |
| 15 | **Wallet Feature** ⭐ | `Wallet` class with `recharge()` / `deduct()` and `InsufficientWalletBalanceException` |

> ⭐ marks the three features added as extensions beyond the base requirements.

---

## 🏗 System Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        CabBookingSystem (Main)                      │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ uses
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│               CabBookingEngine  (Singleton)                         │
│   implements: Bookable · Trackable · Payable                        │
│                                                                     │
│   ┌─────────────┐  ┌──────────────┐  ┌──────────────────────────┐  │
│   │  drivers    │  │   riders     │  │  fareStrategies          │  │
│   │  HashMap    │  │  HashMap     │  │  (Strategy Pattern)      │  │
│   └──────┬──────┘  └──────┬───────┘  └──────────────────────────┘  │
│          │                │                                          │
│   ┌──────▼──────┐  ┌──────▼───────┐  ┌────────────────────────┐   │
│   │   Driver    │  │    Rider     │  │  RideHistory<Booking>  │   │
│   │ (User+Obs.) │  │ (User+Obs.)  │  │  (Generics)            │   │
│   └─────────────┘  └──────┬───────┘  └────────────────────────┘   │
│                            │                                         │
│                     ┌──────▼───────┐                                │
│                     │    Wallet    │                                 │
│                     │(Encapsulated)│                                 │
│                     └──────────────┘                                │
└─────────────────────────────────────────────────────────────────────┘

Fare Strategy Chain (Decorator Pattern):
  FareStrategy  ←── BikeFareStrategy
                ←── AutoFareStrategy
                ←── SedanFareStrategy
                ←── SUVFareStrategy
                ←── SurgePricingDecorator(wraps any FareStrategy, × multiplier)
```

---

## 🎨 Design Patterns

### 1. Singleton Pattern — `CabBookingEngine`
Only one booking engine exists throughout the application's lifetime. All state (drivers, riders, bookings) is centralized.

```java
CabBookingEngine engine = CabBookingEngine.getInstance();
```

### 2. Strategy Pattern — Fare Calculation
Each vehicle type has its own fare logic encapsulated in a strategy class. Switching strategies requires no changes to the engine.

```
Bike  : ₹20 base + ₹8/km
Auto  : ₹30 base + ₹12/km
Sedan : ₹50 base + ₹15/km
SUV   : ₹80 base + ₹20/km
```

### 3. Decorator Pattern — Surge Pricing ⭐
`SurgePricingDecorator` wraps any `FareStrategy` at runtime and multiplies the output fare. Enabling/disabling surge requires no changes to strategy classes.

```java
engine.enableSurge(1.5);   // all fares become 1.5×
engine.disableSurge();     // base strategies restored
```

### 4. Builder Pattern — `Booking` ⭐
`Booking` has 9+ fields. The Builder Pattern eliminates error-prone telescoping constructors and makes construction readable.

```java
Booking booking = new Booking.Builder(bookingId, riderId, driverId)
        .pickup("MG Road")
        .drop("Airport")
        .distanceKm(12.5)
        .fare(237.50)
        .vehicleType(VehicleType.SEDAN)
        .surge(true, 1.5)
        .build();
```

### 5. Observer Pattern — Notifications
Both `Rider` and `Driver` implement `RideObserver`. When a booking status changes, both parties are notified automatically.

```
[SMS → Pranav Sharma] Booking BK1001 | DRIVER_ASSIGNED — Driver Rajesh Kumar is on the way!
[App → Rajesh Kumar]  Booking BK1001 | DRIVER_ASSIGNED — New ride: MG Road → Airport
```

---

## 📁 Project Structure

```
OOP Project/
│
├── CabBookingSystem.java        ← Main source file (all classes in one file)
│
├── ── Compiled .class files ──
│
├── Enums
│   ├── VehicleType.class        (BIKE, AUTO, SEDAN, SUV)
│   ├── RideStatus.class         (REQUESTED, DRIVER_ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED)
│   └── PaymentMethod.class      (UPI, CASH, CARD, WALLET)
│
├── Exceptions
│   ├── InvalidLocationException.class
│   ├── NoDriverAvailableException.class
│   ├── BookingNotFoundException.class
│   ├── RideAlreadyCancelledException.class
│   └── InsufficientWalletBalanceException.class
│
├── Interfaces
│   ├── Bookable.class
│   ├── Trackable.class
│   ├── Payable.class
│   ├── FareStrategy.class
│   └── RideObserver.class
│
├── Domain Classes
│   ├── User.class               (abstract base)
│   ├── Rider.class              (extends User, implements RideObserver)
│   ├── Driver.class             (extends User, implements RideObserver)
│   ├── Booking.class            (Builder Pattern)
│   ├── Booking$Builder.class    (inner Builder class)
│   └── Wallet.class             (encapsulated balance)
│
├── Fare Strategies
│   ├── BikeFareStrategy.class
│   ├── AutoFareStrategy.class
│   ├── SedanFareStrategy.class
│   ├── SUVFareStrategy.class
│   └── SurgePricingDecorator.class
│
├── Engine & Utilities
│   ├── CabBookingEngine.class   (Singleton, core logic)
│   ├── CabBookingEngine$1.class (lambda captured class)
│   ├── RideHistory.class        (Generics)
│   ├── BookingQueue.class       (Generics)
│   └── RideHistoryFileManager.class
│
└── ride_history.txt             ← Persisted ride data (auto-generated on run)
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 17+** (uses Text Blocks and Switch Expressions)
- No external libraries required

### Compile

```bash
# Navigate to the project folder
cd "OOP Project"

# Compile the single source file
javac CabBookingSystem.java
```

### Run

```bash
java CabBookingSystem
```

The program runs a full demo automatically, covering all 15 OOP concepts sequentially. A `ride_history.txt` file is generated in the same directory after the run.

---

## 🎬 Demo Walkthrough

The `main()` method walks through 10 sequential demo sections:

| Section | What it demonstrates |
|---|---|
| `1. Registering Drivers` | 4 drivers across all vehicle types registered in the engine |
| `2. Registering Riders` | 2 riders with initial wallet balances |
| `3. Fare Estimates` | Strategy Pattern — per-vehicle fare for a 10 km trip |
| `4. Sedan Booking` | Full ride lifecycle + UPI payment + invoice generation |
| `5. Wallet Payment` | Wallet deduction on COMPLETED ride, balance display |
| `5a. Wallet Recharge` | `Wallet.recharge()` adds funds to rider balance |
| `5b. Insufficient Balance` | `InsufficientWalletBalanceException` caught and handled |
| `6. Surge Pricing` | Decorator wraps strategies → fares rise 1.5× → invoice shows surge line |
| `7. Cancellation` | Cancel an active booking; driver becomes available again |
| `8. Exception Scenarios` | All 5 exceptions triggered and caught individually |
| `9. Ride History` | Generics-backed history queried per rider |
| `10. File I/O` | History saved to `ride_history.txt` and reloaded |

---

## 🚨 Exception Handling

Five custom **checked exceptions** are defined, each extending `Exception` directly to force callers to handle them:

| Exception | Thrown When |
|---|---|
| `InvalidLocationException` | Pickup/drop is blank, too short, or both locations are identical |
| `NoDriverAvailableException` | No driver of the requested vehicle type is currently free |
| `BookingNotFoundException` | A booking ID lookup fails in the active bookings map |
| `RideAlreadyCancelledException` | Attempting to cancel a ride that is already CANCELLED or COMPLETED |
| `InsufficientWalletBalanceException` | Wallet balance is less than the fare when paying with WALLET method |

All exceptions are demonstrated live in **Section 8** of the demo runner.

---

## 📁 File I/O Persistence

`RideHistoryFileManager` uses `BufferedWriter` and `BufferedReader` to persist and reload ride data.

**Output format (`ride_history.txt`):**

```
===== RIDE HISTORY — 25-Mar-2026 10:30 =====

BookingID   : BK1001
Route       : MG Road → Airport
Distance    : 12.3 km
Vehicle     : SEDAN
Fare        : ₹234.50
Status      : COMPLETED
Payment     : UPI
Paid        : true
-------------------------------------------
BookingID   : BK1002
Route       : Koregaon Park → Viman Nagar
Distance    : 7.8 km
Vehicle     : BIKE
Fare        : ₹82.40
Status      : COMPLETED
Payment     : WALLET
Paid        : true
-------------------------------------------
```

---

## 📊 Sample Output (Excerpt)

```
══════════════════════════════════════════════
  6. SURGE PRICING  ★ NEW Feature (Decorator Pattern)
══════════════════════════════════════════════

[Surge Pricing] ENABLED — multiplier: x1.5  (Decorator Pattern applied to all fare strategies)

── Fare Estimates for 10.0 km ──
   Bike (Surge x1.5)           → ₹120.00
   Auto (Surge x1.5)           → ₹195.00
   Sedan (Surge x1.5)          → ₹262.50
   SUV (Surge x1.5)            → ₹420.00

╔══════════════════════════════════════╗
║           INVOICE / RECEIPT          ║
╠══════════════════════════════════════╣
║  Booking ID  : BK1003               ║
║  Route       : Hinjawadi→Baner      ║
║  Distance    : 9.4 km               ║
║  Vehicle     : AUTO                 ║
╠══════════════════════════════════════╣
║  Base Fare   : ₹142.80              ║
║  Surge (x1.5): ₹71.40               ║
║  Tax (5%)    : ₹10.71               ║
║  Platform Fee: ₹10.00               ║
╠══════════════════════════════════════╣
║  TOTAL       : ₹234.91              ║
║  Payment     : CARD                 ║
╚══════════════════════════════════════╝
```

---

## 👨‍💻 Author

**Pranav Bhattad**

Built as an **Object-Oriented Programming course project** to demonstrate design patterns, SOLID principles, and core Java features in a realistic, production-inspired application.
