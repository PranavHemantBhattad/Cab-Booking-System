import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.*;

// ============================================================
// SECTION 1: ENUMS
// ============================================================

enum VehicleType { BIKE, AUTO, SEDAN, SUV }

enum RideStatus { REQUESTED, DRIVER_ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED }

enum PaymentMethod { UPI, CASH, CARD, WALLET }   // WALLET added for extension

// ============================================================
// SECTION 2: CUSTOM CHECKED EXCEPTIONS
// ============================================================

class InvalidLocationException extends Exception {
    public InvalidLocationException(String msg) { super(msg); }
}

class NoDriverAvailableException extends Exception {
    public NoDriverAvailableException(String msg) { super(msg); }
}

class BookingNotFoundException extends Exception {
    public BookingNotFoundException(String msg) { super(msg); }
}

class RideAlreadyCancelledException extends Exception {
    public RideAlreadyCancelledException(String msg) { super(msg); }
}

class InsufficientWalletBalanceException extends Exception {
    public InsufficientWalletBalanceException(String msg) { super(msg); }
}

// ============================================================
// SECTION 3: INTERFACES
// ============================================================

interface Bookable {
    String book(String pickup, String drop, VehicleType type, String riderId)
        throws InvalidLocationException, NoDriverAvailableException;

    void cancel(String bookingId)
        throws BookingNotFoundException, RideAlreadyCancelledException;
}

interface Trackable {
    RideStatus getStatus(String bookingId) throws BookingNotFoundException;
    void updateStatus(String bookingId, RideStatus status) throws BookingNotFoundException;
}

interface Payable {
    void processPayment(String bookingId, PaymentMethod method)
        throws BookingNotFoundException, InsufficientWalletBalanceException;
    void generateInvoice(String bookingId) throws BookingNotFoundException;
}

interface FareStrategy {
    double calculateFare(double distanceKm);
    String getVehicleTypeName();
}

interface RideObserver {
    void onRideUpdate(String bookingId, RideStatus status, String message);
}

// ============================================================
// SECTION 4: FARE STRATEGIES  (Strategy Pattern)
// ============================================================

class BikeFareStrategy implements FareStrategy {
    @Override public double calculateFare(double km) { return 20.0 + (km * 8.0); }
    @Override public String getVehicleTypeName()     { return "Bike"; }
}

class AutoFareStrategy implements FareStrategy {
    @Override public double calculateFare(double km) { return 30.0 + (km * 12.0); }
    @Override public String getVehicleTypeName()     { return "Auto"; }
}

class SedanFareStrategy implements FareStrategy {
    @Override public double calculateFare(double km) { return 50.0 + (km * 15.0); }
    @Override public String getVehicleTypeName()     { return "Sedan"; }
}

class SUVFareStrategy implements FareStrategy {
    @Override public double calculateFare(double km) { return 80.0 + (km * 20.0); }
    @Override public String getVehicleTypeName()     { return "SUV"; }
}

// ============================================================
// SECTION 5: DECORATOR PATTERN — Surge Pricing  ★ NEW
// ============================================================

/**
 * SurgePricingDecorator wraps any FareStrategy and multiplies the
 * calculated fare by a surge multiplier (e.g. 1.5x during peak hours).
 *
 * OOP Concept: Decorator Pattern
 *   - Adds behaviour to an object at runtime WITHOUT changing its class
 *   - SurgePricingDecorator IS-A FareStrategy (implements the same interface)
 *   - HAS-A FareStrategy (wraps the real strategy)
 *   - Open/Closed: engine never changes; just wrap the strategy
 */
class SurgePricingDecorator implements FareStrategy {
    private final FareStrategy wrapped;   // the real strategy being decorated
    private final double       multiplier; // surge factor, e.g. 1.5

    public SurgePricingDecorator(FareStrategy wrapped, double multiplier) {
        this.wrapped    = wrapped;
        this.multiplier = multiplier;
    }

    @Override
    public double calculateFare(double distanceKm) {
        double baseFare = wrapped.calculateFare(distanceKm);
        return baseFare * multiplier;               // surge applied here
    }

    @Override
    public String getVehicleTypeName() {
        return wrapped.getVehicleTypeName() + " (Surge x" + multiplier + ")";
    }

    public double getMultiplier()    { return multiplier; }
    public FareStrategy getWrapped() { return wrapped; }
}

// ============================================================
// SECTION 6: WALLET  ★ NEW
// ============================================================

/**
 * Wallet — encapsulated value object held by every Rider.
 *
 * OOP Concepts:
 *   - Encapsulation: balance is private; recharge/deduct are the only
 *     mutators; getBalance() is read-only
 *   - Exception Handling: deduct() throws InsufficientWalletBalanceException
 *     (checked) so callers are forced to handle it
 */
class Wallet {
    private double balance;

    public Wallet(double initialBalance) {
        this.balance = initialBalance;
    }

    /** Add money to wallet */
    public void recharge(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Recharge amount must be > 0");
        balance += amount;
        System.out.println("   [Wallet] Recharged ₹" + String.format("%.2f", amount)
                           + "  |  New balance: ₹" + String.format("%.2f", balance));
    }

    /** Deduct fare — throws checked exception if funds are insufficient */
    public void deduct(double amount) throws InsufficientWalletBalanceException {
        if (amount > balance) {
            throw new InsufficientWalletBalanceException(
                "Insufficient wallet balance. Required: ₹" + String.format("%.2f", amount)
                + "  Available: ₹" + String.format("%.2f", balance));
        }
        balance -= amount;
    }

    public double getBalance() { return balance; }

    @Override
    public String toString() {
        return "Wallet{balance=₹" + String.format("%.2f", balance) + "}";
    }
}

// ============================================================
// SECTION 7: ABSTRACT USER + CONCRETE RIDER / DRIVER
// ============================================================

/**
 * User — abstract base class (OOP: Abstract Class, Encapsulation)
 * Defines common state (name, phone, email) and forces subclasses
 * to implement getRole().
 */
abstract class User {
    private final String userId;
    private final String name;
    private final String phone;
    private final String email;

    public User(String userId, String name, String phone, String email) {
        this.userId = userId;
        this.name   = name;
        this.phone  = phone;
        this.email  = email;
    }

    // Polymorphism: each subclass returns a different role string
    public abstract String getRole();

    public String getUserId() { return userId; }
    public String getName()   { return name; }
    public String getPhone()  { return phone; }
    public String getEmail()  { return email; }

    @Override
    public String toString() {
        return getRole() + "[" + userId + "] " + name + " (" + phone + ")";
    }
}

/**
 * Rider — extends User, implements RideObserver
 * Now ALSO owns a Wallet (★ new).
 */
class Rider extends User implements RideObserver {
    private double         walletBalance;
    private final Wallet   wallet;                        // ★ NEW
    private final List<String> bookingHistory = new ArrayList<>();

    public Rider(String userId, String name, String phone, String email,
                 double initialWalletBalance) {
        super(userId, name, phone, email);
        this.walletBalance = initialWalletBalance;
        this.wallet        = new Wallet(initialWalletBalance); // ★ NEW
    }

    @Override public String getRole() { return "RIDER"; }

    // Observer callback — simulates SMS notification
    @Override
    public void onRideUpdate(String bookingId, RideStatus status, String msg) {
        System.out.println("   [SMS → " + getName() + "] Booking " + bookingId
                           + " | " + status + " — " + msg);
    }

    public void addBookingToHistory(String bookingId) {
        bookingHistory.add(bookingId);
    }

    public List<String> getBookingHistory() {
        return Collections.unmodifiableList(bookingHistory);
    }

    // ★ NEW — wallet accessors
    public Wallet getWallet() { return wallet; }
}

/**
 * Driver — extends User, implements RideObserver
 */
class Driver extends User implements RideObserver {
    private final VehicleType vehicleType;
    private final String      vehicleNumber;
    private boolean           available = true;
    private double            rating    = 4.5;
    private int               totalRides = 0;

    public Driver(String userId, String name, String phone, String email,
                  VehicleType vehicleType, String vehicleNumber) {
        super(userId, name, phone, email);
        this.vehicleType   = vehicleType;
        this.vehicleNumber = vehicleNumber;
    }

    @Override public String getRole() { return "DRIVER"; }

    // Observer callback — simulates app push notification
    @Override
    public void onRideUpdate(String bookingId, RideStatus status, String msg) {
        System.out.println("   [App  → " + getName() + "] Booking " + bookingId
                           + " | " + status + " — " + msg);
    }

    public VehicleType getVehicleType()  { return vehicleType; }
    public String      getVehicleNumber(){ return vehicleNumber; }
    public boolean     isAvailable()     { return available; }
    public double      getRating()       { return rating; }
    public int         getTotalRides()   { return totalRides; }

    public void setAvailable(boolean available) { this.available = available; }
    public void incrementRides()                { totalRides++; }

    @Override
    public String toString() {
        return super.toString() + " | " + vehicleType + " [" + vehicleNumber
               + "] | Rating: " + rating + " | Rides: " + totalRides
               + " | " + (available ? "AVAILABLE" : "BUSY");
    }
}

// ============================================================
// SECTION 8: BOOKING (Builder Pattern)  ★ EXTENDED
// ============================================================

/**
 * Booking — core domain object using the Builder Pattern.
 *
 * OOP Concept: Builder Pattern
 *   - Problem: Booking has 9+ fields; telescoping constructors are ugly
 *     and error-prone (wrong order of Strings is a silent bug)
 *   - Solution: Booking.Builder collects all parameters fluently and
 *     calls build() to produce an immutable-enough Booking
 *   - Before: new Booking(id, rider, driver, pickup, drop, km, fare, type, time)
 *   - After : new Booking.Builder(id, rider, driver)
 *                 .pickup("MG Road").drop("Airport")
 *                 .distanceKm(12.5).fare(237.50)
 *                 .vehicleType(VehicleType.SEDAN)
 *                 .build();
 *
 * Also implements Serializable for File I/O persistence.
 */
class Booking implements Serializable {
    private static final long serialVersionUID = 1L;

    // Core fields (set via Builder)
    private final String        bookingId;
    private final String        riderId;
    private final String        driverId;
    private final String        pickupLocation;
    private final String        dropLocation;
    private final double        distanceKm;
    private       double        fare;
    private final VehicleType   vehicleType;
    private final LocalDateTime bookingTime;
    private       RideStatus    status;
    private       PaymentMethod paymentMethod;
    private       boolean       isPaid = false;
    private       boolean       isSurge;      // ★ NEW — tracks surge flag
    private       double        surgeMultiplier = 1.0; // ★ NEW

    // ── Private constructor — only Builder can call this ──
    private Booking(Builder b) {
        this.bookingId       = b.bookingId;
        this.riderId         = b.riderId;
        this.driverId        = b.driverId;
        this.pickupLocation  = b.pickupLocation;
        this.dropLocation    = b.dropLocation;
        this.distanceKm      = b.distanceKm;
        this.fare            = b.fare;
        this.vehicleType     = b.vehicleType;
        this.bookingTime     = b.bookingTime;
        this.status          = RideStatus.REQUESTED;
        this.isSurge         = b.isSurge;
        this.surgeMultiplier = b.surgeMultiplier;
    }

    // ── Builder static inner class ──
    static class Builder {
        // Required
        private final String      bookingId;
        private final String      riderId;
        private final String      driverId;
        // Optional (have defaults)
        private String            pickupLocation  = "";
        private String            dropLocation    = "";
        private double            distanceKm      = 0.0;
        private double            fare            = 0.0;
        private VehicleType       vehicleType     = VehicleType.SEDAN;
        private LocalDateTime     bookingTime     = LocalDateTime.now();
        private boolean           isSurge         = false;
        private double            surgeMultiplier = 1.0;

        public Builder(String bookingId, String riderId, String driverId) {
            this.bookingId = bookingId;
            this.riderId   = riderId;
            this.driverId  = driverId;
        }

        public Builder pickup(String pickup)             { this.pickupLocation  = pickup;    return this; }
        public Builder drop(String drop)                 { this.dropLocation    = drop;      return this; }
        public Builder distanceKm(double km)             { this.distanceKm      = km;        return this; }
        public Builder fare(double fare)                 { this.fare            = fare;      return this; }
        public Builder vehicleType(VehicleType vt)       { this.vehicleType     = vt;        return this; }
        public Builder bookingTime(LocalDateTime time)   { this.bookingTime     = time;      return this; }
        public Builder surge(boolean isSurge, double m)  {
            this.isSurge         = isSurge;
            this.surgeMultiplier = m;
            return this;
        }

        /** Terminal method — produces the Booking */
        public Booking build() {
            if (pickupLocation.isBlank()) throw new IllegalStateException("Pickup must be set");
            if (dropLocation.isBlank())   throw new IllegalStateException("Drop must be set");
            return new Booking(this);
        }
    }

    // ── Getters ──
    public String        getBookingId()      { return bookingId; }
    public String        getRiderId()        { return riderId; }
    public String        getDriverId()       { return driverId; }
    public String        getPickupLocation() { return pickupLocation; }
    public String        getDropLocation()   { return dropLocation; }
    public double        getDistanceKm()     { return distanceKm; }
    public double        getFare()           { return fare; }
    public VehicleType   getVehicleType()    { return vehicleType; }
    public LocalDateTime getBookingTime()    { return bookingTime; }
    public RideStatus    getStatus()         { return status; }
    public PaymentMethod getPaymentMethod()  { return paymentMethod; }
    public boolean       isPaid()            { return isPaid; }
    public boolean       isSurge()           { return isSurge; }
    public double        getSurgeMultiplier(){ return surgeMultiplier; }

    // ── Setters (mutable fields only) ──
    public void setStatus(RideStatus status)             { this.status        = status; }
    public void setPaymentMethod(PaymentMethod pm)       { this.paymentMethod = pm; }
    public void setPaid(boolean paid)                    { this.isPaid        = paid; }
    public void setFare(double fare)                     { this.fare          = fare; }

    @Override
    public String toString() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");
        return String.format(
            "Booking{id=%s, route=%s→%s, %.1fkm, ₹%.2f%s, %s, %s, paid=%s}",
            bookingId, pickupLocation, dropLocation, distanceKm, fare,
            isSurge ? "(surge x" + surgeMultiplier + ")" : "",
            vehicleType, status, isPaid);
    }
}

// ============================================================
// SECTION 9: GENERICS — RideHistory<T> and BookingQueue<T>
// ============================================================

class RideHistory<T extends Booking> {
    private final List<T> history = new ArrayList<>();

    public void addBooking(T booking) { history.add(booking); }

    public List<T> getAll() { return Collections.unmodifiableList(history); }

    public Optional<T> findById(String id) {
        return history.stream()
                      .filter(b -> b.getBookingId().equals(id))
                      .findFirst();
    }

    public List<T> getByRider(String riderId) {
        return history.stream()
                      .filter(b -> b.getRiderId().equals(riderId))
                      .collect(Collectors.toList());
    }

    public int size() { return history.size(); }
}

class BookingQueue<T> {
    private final Queue<T> queue = new LinkedList<>();
    public void enqueue(T item) { queue.offer(item); }
    public T    dequeue()       { return queue.poll(); }
    public int  size()          { return queue.size(); }
    public boolean isEmpty()    { return queue.isEmpty(); }
}

// ============================================================
// SECTION 10: FILE I/O — RideHistoryFileManager
// ============================================================

class RideHistoryFileManager {
    private static final String FILE_PATH = "ride_history.txt";

    public void saveToFile(RideHistory<Booking> rideHistory) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH))) {
            writer.write("===== RIDE HISTORY — " + LocalDateTime.now()
                         .format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm")) + " =====\n\n");
            for (Booking b : rideHistory.getAll()) {
                writer.write("BookingID   : " + b.getBookingId() + "\n");
                writer.write("Route       : " + b.getPickupLocation()
                             + " → " + b.getDropLocation() + "\n");
                writer.write("Distance    : " + b.getDistanceKm() + " km\n");
                writer.write("Vehicle     : " + b.getVehicleType() + "\n");
                writer.write("Fare        : ₹" + String.format("%.2f", b.getFare())
                             + (b.isSurge() ? "  [SURGE x" + b.getSurgeMultiplier() + "]" : "") + "\n");
                writer.write("Status      : " + b.getStatus() + "\n");
                writer.write("Payment     : " + (b.getPaymentMethod() != null
                             ? b.getPaymentMethod() : "PENDING") + "\n");
                writer.write("Paid        : " + b.isPaid() + "\n");
                writer.write("-------------------------------------------\n");
            }
            System.out.println("\n[File I/O] Ride history saved → " + FILE_PATH);
        } catch (IOException e) {
            System.out.println("[File I/O] Save failed: " + e.getMessage());
        }
    }

    public void loadFromFile() {
        System.out.println("\n[File I/O] Loading ride history from → " + FILE_PATH);
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("   " + line);
            }
        } catch (IOException e) {
            System.out.println("[File I/O] Load failed: " + e.getMessage());
        }
    }
}

// ============================================================
// SECTION 11: CAB BOOKING ENGINE  (Singleton + Bookable + Trackable + Payable)
// ============================================================

class CabBookingEngine implements Bookable, Trackable, Payable {

    // ── Singleton ──
    private static CabBookingEngine instance;

    private CabBookingEngine() {
        initFareStrategies();
    }

    public static CabBookingEngine getInstance() {
        if (instance == null) {
            instance = new CabBookingEngine();
        }
        return instance;
    }

    // ── Internal state ──
    private final Map<String, Driver>  drivers        = new HashMap<>();
    private final Map<String, Rider>   riders         = new HashMap<>();
    private final Map<String, Booking> activeBookings = new HashMap<>();
    private final RideHistory<Booking> rideHistory    = new RideHistory<>();
    private final BookingQueue<Booking> pendingQueue  = new BookingQueue<>();

    private Map<VehicleType, FareStrategy> fareStrategies = new HashMap<>();
    private int  bookingCounter = 1000;
    private boolean surgeActive = false;      // ★ NEW — surge toggle
    private double  surgeMultiplier = 1.5;    // ★ NEW — surge factor

    private void initFareStrategies() {
        fareStrategies.put(VehicleType.BIKE,  new BikeFareStrategy());
        fareStrategies.put(VehicleType.AUTO,  new AutoFareStrategy());
        fareStrategies.put(VehicleType.SEDAN, new SedanFareStrategy());
        fareStrategies.put(VehicleType.SUV,   new SUVFareStrategy());
    }

    // ── Registration ──
    public void registerDriver(Driver d) { drivers.put(d.getUserId(), d); }
    public void registerRider(Rider r)   { riders.put(r.getUserId(), r); }

    // ── Surge control  ★ NEW ──
    public void enableSurge(double multiplier) {
        surgeActive     = true;
        surgeMultiplier = multiplier;
        // Wrap each strategy with SurgePricingDecorator
        fareStrategies.replaceAll((type, strategy) ->
            new SurgePricingDecorator(strategy, multiplier));
        System.out.println("\n[Surge Pricing] ENABLED — multiplier: x" + multiplier
                           + "  (Decorator Pattern applied to all fare strategies)");
    }

    public void disableSurge() {
        surgeActive = false;
        initFareStrategies();   // restore original strategies
        System.out.println("[Surge Pricing] DISABLED — base fares restored.");
    }

    // ── Fare estimate ──
    public void showFareEstimates(double distanceKm) {
        System.out.println("\n── Fare Estimates for " + distanceKm + " km ──");
        for (Map.Entry<VehicleType, FareStrategy> e : fareStrategies.entrySet()) {
            System.out.printf("   %-28s → ₹%.2f%n",
                e.getValue().getVehicleTypeName(),
                e.getValue().calculateFare(distanceKm));
        }
    }

    // ── Driver availability ──
    private Driver findAvailableDriver(VehicleType type) throws NoDriverAvailableException {
        return drivers.values().stream()
                      .filter(d -> d.getVehicleType() == type && d.isAvailable())
                      .findFirst()
                      .orElseThrow(() -> new NoDriverAvailableException(
                          "No " + type + " driver available right now."));
    }

    // ── Location validation ──
    private void validateLocation(String pickup, String drop) throws InvalidLocationException {
        if (pickup == null || pickup.isBlank() || pickup.length() < 3)
            throw new InvalidLocationException("Invalid pickup: '" + pickup + "'");
        if (drop == null || drop.isBlank() || drop.length() < 3)
            throw new InvalidLocationException("Invalid drop: '" + drop + "'");
        if (pickup.equalsIgnoreCase(drop))
            throw new InvalidLocationException("Pickup and drop cannot be the same location.");
    }

    // ── BOOKABLE: book() ──
    @Override
    public String book(String pickup, String drop, VehicleType type, String riderId)
            throws InvalidLocationException, NoDriverAvailableException {

        validateLocation(pickup, drop);

        Rider rider = riders.get(riderId);
        if (rider == null) throw new NoDriverAvailableException("Rider ID not found: " + riderId);

        Driver driver = findAvailableDriver(type);
        FareStrategy strategy = fareStrategies.get(type);

        double distanceKm = 5 + Math.random() * 15;   // simulated distance
        distanceKm        = Math.round(distanceKm * 10.0) / 10.0;
        double fare       = strategy.calculateFare(distanceKm);

        String bookingId = "BK" + (++bookingCounter);

        // ★ Builder Pattern — replaces old telescoping constructor
        Booking booking = new Booking.Builder(bookingId, riderId, driver.getUserId())
                .pickup(pickup)
                .drop(drop)
                .distanceKm(distanceKm)
                .fare(fare)
                .vehicleType(type)
                .bookingTime(LocalDateTime.now())
                .surge(surgeActive, surgeActive ? surgeMultiplier : 1.0)
                .build();

        driver.setAvailable(false);
        activeBookings.put(bookingId, booking);
        rideHistory.addBooking(booking);
        pendingQueue.enqueue(booking);
        rider.addBookingToHistory(bookingId);

        System.out.println("\n[Booking Created] " + bookingId
                           + " | " + pickup + " → " + drop
                           + " | " + distanceKm + " km"
                           + " | ₹" + String.format("%.2f", fare)
                           + (surgeActive ? "  [SURGE x" + surgeMultiplier + "]" : "")
                           + " | Driver: " + driver.getName());

        // ── Observer: notify both rider and driver ──
        rider.onRideUpdate(bookingId, RideStatus.DRIVER_ASSIGNED,
                "Driver " + driver.getName() + " is on the way! (" + driver.getVehicleNumber() + ")");
        driver.onRideUpdate(bookingId, RideStatus.DRIVER_ASSIGNED,
                "New ride: " + pickup + " → " + drop + " | Fare ₹" + String.format("%.2f", fare));

        booking.setStatus(RideStatus.DRIVER_ASSIGNED);
        return bookingId;
    }

    // ── BOOKABLE: cancel() ──
    @Override
    public void cancel(String bookingId)
            throws BookingNotFoundException, RideAlreadyCancelledException {

        Booking booking = getBookingOrThrow(bookingId);

        if (booking.getStatus() == RideStatus.CANCELLED)
            throw new RideAlreadyCancelledException("Booking " + bookingId + " is already cancelled.");
        if (booking.getStatus() == RideStatus.COMPLETED)
            throw new RideAlreadyCancelledException("Cannot cancel a completed ride: " + bookingId);

        booking.setStatus(RideStatus.CANCELLED);
        Driver driver = drivers.get(booking.getDriverId());
        if (driver != null) driver.setAvailable(true);

        Rider rider = riders.get(booking.getRiderId());
        if (rider != null)
            rider.onRideUpdate(bookingId, RideStatus.CANCELLED, "Your ride has been cancelled.");

        System.out.println("[Cancel] Booking " + bookingId + " cancelled. Driver freed.");
    }

    // ── TRACKABLE: getStatus() ──
    @Override
    public RideStatus getStatus(String bookingId) throws BookingNotFoundException {
        return getBookingOrThrow(bookingId).getStatus();
    }

    // ── TRACKABLE: updateStatus() ──
    @Override
    public void updateStatus(String bookingId, RideStatus status) throws BookingNotFoundException {
        Booking booking = getBookingOrThrow(bookingId);
        booking.setStatus(status);

        String msg = switch (status) {
            case IN_PROGRESS -> "Your ride has started! Enjoy the trip.";
            case COMPLETED   -> "Ride completed! Fare ₹" + String.format("%.2f", booking.getFare());
            case CANCELLED   -> "Ride cancelled.";
            default          -> "Status updated to " + status;
        };

        Rider  rider  = riders.get(booking.getRiderId());
        Driver driver = drivers.get(booking.getDriverId());

        if (rider  != null) rider.onRideUpdate(bookingId, status, msg);
        if (driver != null) driver.onRideUpdate(bookingId, status, "Ride " + bookingId + " → " + status);

        if (status == RideStatus.COMPLETED) {
            if (driver != null) {
                driver.setAvailable(true);
                driver.incrementRides();
            }
        }

        System.out.println("[Status Updated] " + bookingId + " → " + status);
    }

    // ── PAYABLE: processPayment()  ★ EXTENDED with WALLET support ──
    @Override
    public void processPayment(String bookingId, PaymentMethod method)
            throws BookingNotFoundException, InsufficientWalletBalanceException {

        Booking booking = getBookingOrThrow(bookingId);
        if (booking.isPaid()) {
            System.out.println("[Payment] " + bookingId + " is already paid.");
            return;
        }

        if (method == PaymentMethod.WALLET) {
            // ★ NEW: deduct from Rider's Wallet
            Rider rider = riders.get(booking.getRiderId());
            if (rider == null) throw new BookingNotFoundException("Rider not found for " + bookingId);

            // This throws InsufficientWalletBalanceException if balance < fare
            rider.getWallet().deduct(booking.getFare());
            System.out.println("[Payment] Wallet deducted ₹"
                + String.format("%.2f", booking.getFare())
                + "  | New wallet balance: ₹"
                + String.format("%.2f", rider.getWallet().getBalance()));
        }

        booking.setPaymentMethod(method);
        booking.setPaid(true);
        System.out.println("[Payment] " + bookingId + " paid via " + method
                           + " | Amount: ₹" + String.format("%.2f", booking.getFare()));
    }

    // ── PAYABLE: generateInvoice() ──
    @Override
    public void generateInvoice(String bookingId) throws BookingNotFoundException {
        Booking b = getBookingOrThrow(bookingId);
        double  baseFare     = b.getFare() / (b.isSurge() ? b.getSurgeMultiplier() : 1.0);
        double  surgePremium = b.getFare() - baseFare;
        double  tax          = b.getFare() * 0.05;
        double  platformFee  = 10.0;
        double  total        = b.getFare() + tax + platformFee;

        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║           INVOICE / RECEIPT          ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.printf( "║  Booking ID  : %-22s║%n", b.getBookingId());
        System.out.printf( "║  Route       : %-22s║%n", b.getPickupLocation() + "→" + b.getDropLocation());
        System.out.printf( "║  Distance    : %-22s║%n", b.getDistanceKm() + " km");
        System.out.printf( "║  Vehicle     : %-22s║%n", b.getVehicleType());
        System.out.println("╠══════════════════════════════════════╣");
        System.out.printf( "║  Base Fare   : ₹%-21s║%n", String.format("%.2f", baseFare));
        if (b.isSurge()) {
            System.out.printf("║  Surge (x%-4s): ₹%-21s║%n",
                b.getSurgeMultiplier(), String.format("%.2f", surgePremium));
        }
        System.out.printf( "║  Tax (5%%)    : ₹%-21s║%n", String.format("%.2f", tax));
        System.out.printf( "║  Platform Fee: ₹%-21s║%n", String.format("%.2f", platformFee));
        System.out.println("╠══════════════════════════════════════╣");
        System.out.printf( "║  TOTAL       : ₹%-21s║%n", String.format("%.2f", total));
        System.out.printf( "║  Payment     : %-22s║%n",
            b.getPaymentMethod() != null ? b.getPaymentMethod() : "PENDING");
        System.out.println("╚══════════════════════════════════════╝");
    }

    // ── Helpers ──
    private Booking getBookingOrThrow(String bookingId) throws BookingNotFoundException {
        Booking b = activeBookings.get(bookingId);
        if (b == null) throw new BookingNotFoundException("Booking not found: " + bookingId);
        return b;
    }

    public void showAllDrivers() {
        System.out.println("\n── Registered Drivers ──");
        drivers.values().forEach(d -> System.out.println("   " + d));
    }

    public void showRiderHistory(String riderId) {
        System.out.println("\n── Ride History for Rider: " + riderId + " ──");
        rideHistory.getByRider(riderId).forEach(b -> System.out.println("   " + b));
    }

    public RideHistory<Booking> getRideHistory() { return rideHistory; }

    public void showWalletBalance(String riderId) {
        Rider rider = riders.get(riderId);
        if (rider != null)
            System.out.println("[Wallet] " + rider.getName()
                               + " → ₹" + String.format("%.2f", rider.getWallet().getBalance()));
    }
}

// ============================================================
// SECTION 12: MAIN — DEMO RUNNER
// ============================================================

public class CabBookingSystem {

    public static void main(String[] args) {

        CabBookingEngine engine = CabBookingEngine.getInstance();
        RideHistoryFileManager fileManager = new RideHistoryFileManager();

        separator("1. REGISTERING DRIVERS");
        Driver d1 = new Driver("D001", "Rajesh Kumar",   "9876543210", "rajesh@cab.com",  VehicleType.SEDAN, "MH12-AB-1234");
        Driver d2 = new Driver("D002", "Suresh Yadav",   "9876543211", "suresh@cab.com",  VehicleType.BIKE,  "MH12-CD-5678");
        Driver d3 = new Driver("D003", "Manoj Patil",    "9876543212", "manoj@cab.com",   VehicleType.AUTO,  "MH12-EF-9012");
        Driver d4 = new Driver("D004", "Vikram Singh",   "9876543213", "vikram@cab.com",  VehicleType.SUV,   "MH12-GH-3456");
        engine.registerDriver(d1);
        engine.registerDriver(d2);
        engine.registerDriver(d3);
        engine.registerDriver(d4);
        engine.showAllDrivers();

        separator("2. REGISTERING RIDERS");
        Rider r1 = new Rider("R001", "Pranav Sharma", "8765432100", "pranav@mail.com", 500.00);
        Rider r2 = new Rider("R002", "Aisha Mehta",   "8765432101", "aisha@mail.com",  200.00);
        engine.registerRider(r1);
        engine.registerRider(r2);
        System.out.println("   Registered: " + r1);
        System.out.println("   Registered: " + r2);
        System.out.println("   " + r1.getName() + " wallet → " + r1.getWallet());
        System.out.println("   " + r2.getName() + " wallet → " + r2.getWallet());

        separator("3. FARE ESTIMATES — BASE FARES (10 km)");
        engine.showFareEstimates(10.0);

        // ── DEMO: Builder Pattern ──
        separator("4. BOOKING — SEDAN RIDE  (Builder Pattern demo)");
        try {
            String bid1 = engine.book("MG Road", "Airport", VehicleType.SEDAN, "R001");
            engine.updateStatus(bid1, RideStatus.IN_PROGRESS);
            engine.updateStatus(bid1, RideStatus.COMPLETED);
            engine.processPayment(bid1, PaymentMethod.UPI);
            engine.generateInvoice(bid1);
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }

        // ── DEMO: Wallet Payment ──
        separator("5. WALLET PAYMENT  ★ NEW Feature");
        System.out.println("   Pranav's wallet before ride: ₹" + String.format("%.2f", r1.getWallet().getBalance()));
        try {
            String bid2 = engine.book("Koregaon Park", "Viman Nagar", VehicleType.BIKE, "R001");
            engine.updateStatus(bid2, RideStatus.IN_PROGRESS);
            engine.updateStatus(bid2, RideStatus.COMPLETED);
            engine.processPayment(bid2, PaymentMethod.WALLET);   // ← Wallet deducted here
            engine.generateInvoice(bid2);
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
        engine.showWalletBalance("R001");

        // ── DEMO: Wallet Top-up ──
        separator("5a. WALLET RECHARGE  ★ NEW Feature");
        r1.getWallet().recharge(300.0);
        engine.showWalletBalance("R001");

        // ── DEMO: Insufficient Wallet Balance ──
        separator("5b. INSUFFICIENT WALLET BALANCE  (Exception Handling)");
        try {
            String bid3 = engine.book("Pune Station", "Lonavala", VehicleType.SUV, "R002");
            engine.updateStatus(bid3, RideStatus.IN_PROGRESS);
            engine.updateStatus(bid3, RideStatus.COMPLETED);
            System.out.println("   Aisha wallet: ₹" + String.format("%.2f", r2.getWallet().getBalance()));
            engine.processPayment(bid3, PaymentMethod.WALLET);   // ← will fail
        } catch (InsufficientWalletBalanceException e) {
            System.out.println("[Caught] InsufficientWalletBalanceException: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }

        // ── DEMO: Surge Pricing Decorator ──
        separator("6. SURGE PRICING  ★ NEW Feature (Decorator Pattern)");
        engine.enableSurge(1.5);
        engine.showFareEstimates(10.0);

        try {
            String bidSurge = engine.book("Hinjawadi", "Baner", VehicleType.AUTO, "R001");
            engine.updateStatus(bidSurge, RideStatus.IN_PROGRESS);
            engine.updateStatus(bidSurge, RideStatus.COMPLETED);
            engine.processPayment(bidSurge, PaymentMethod.CARD);
            engine.generateInvoice(bidSurge);  // ← invoice shows surge line item
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }

        engine.disableSurge();
        engine.showFareEstimates(10.0);  // back to base fares

        // ── DEMO: Ride Cancellation ──
        separator("7. RIDE CANCELLATION");
        try {
            String bidC = engine.book("Wakad", "Pimpri", VehicleType.SEDAN, "R002");
            engine.cancel(bidC);
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }

        // ── DEMO: All 5 Exceptions ──
        separator("8. EXCEPTION SCENARIOS");

        System.out.println("\n-- 8a. InvalidLocationException --");
        try {
            engine.book("", "Airport", VehicleType.SEDAN, "R001");
        } catch (InvalidLocationException e) {
            System.out.println("[Caught] " + e.getClass().getSimpleName() + ": " + e.getMessage());
        } catch (Exception e) { System.out.println(e.getMessage()); }

        System.out.println("\n-- 8b. InvalidLocationException (same location) --");
        try {
            engine.book("MG Road", "MG Road", VehicleType.AUTO, "R001");
        } catch (InvalidLocationException e) {
            System.out.println("[Caught] " + e.getClass().getSimpleName() + ": " + e.getMessage());
        } catch (Exception e) { System.out.println(e.getMessage()); }

        System.out.println("\n-- 8c. NoDriverAvailableException --");
        try {
            // All SEDAN drivers are currently busy
            engine.book("Camp", "Kothrud", VehicleType.SEDAN, "R001");
        } catch (NoDriverAvailableException e) {
            System.out.println("[Caught] " + e.getClass().getSimpleName() + ": " + e.getMessage());
        } catch (Exception e) { System.out.println(e.getMessage()); }

        System.out.println("\n-- 8d. BookingNotFoundException --");
        try {
            engine.getStatus("BK9999");
        } catch (BookingNotFoundException e) {
            System.out.println("[Caught] " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        System.out.println("\n-- 8e. RideAlreadyCancelledException --");
        try {
            // Cancel a completed booking
            engine.cancel("BK1001");
        } catch (BookingNotFoundException | RideAlreadyCancelledException e) {
            System.out.println("[Caught] " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        // ── DEMO: Ride history ──
        separator("9. RIDE HISTORY");
        engine.showRiderHistory("R001");
        engine.showRiderHistory("R002");

        // ── DEMO: File I/O ──
        separator("10. FILE I/O — SAVE & LOAD");
        fileManager.saveToFile(engine.getRideHistory());
        fileManager.loadFromFile();

        // ── Summary ──
        separator("OOP CONCEPT SUMMARY");
        System.out.println("""
            ┌──────────────────────────────────────────────────────────────┐
            │  #   OOP Concept          Where Applied                      │
            ├──────────────────────────────────────────────────────────────┤
            │  1   Inheritance          User → Rider, User → Driver        │
            │  2   Polymorphism         getRole() overridden per subclass   │
            │  3   Abstract Class       abstract User enforces getRole()    │
            │  4   Encapsulation        private fields + getters/setters    │
            │  5   Interfaces (5x)      Bookable,Trackable,Payable,etc.     │
            │  6   Generics             RideHistory<T>, BookingQueue<T>     │
            │  7   Collections          HashMap, ArrayList, LinkedList      │
            │  8   Exception Handling   5 custom checked exceptions         │
            │  9   Singleton Pattern    CabBookingEngine.getInstance()      │
            │ 10   Strategy Pattern     Bike/Auto/Sedan/SUV fare strategies │
            │ 11   Observer Pattern     Rider+Driver notified on update     │
            │ 12   File I/O             BufferedWriter / BufferedReader     │
            │ 13 ★ Builder Pattern      Booking.Builder fluent API          │
            │ 14 ★ Decorator Pattern    SurgePricingDecorator wraps Strategy│
            │ 15 ★ Wallet Feature       Wallet class, WALLET PaymentMethod  │
            └──────────────────────────────────────────────────────────────┘
            """);
    }

    private static void separator(String title) {
        System.out.println("\n══════════════════════════════════════════════");
        System.out.println("  " + title);
        System.out.println("══════════════════════════════════════════════");
    }
}
