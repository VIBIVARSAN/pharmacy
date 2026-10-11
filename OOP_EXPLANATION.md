# OOP concepts in the Pharmacy Prescription Management System

This document shows where each object-oriented concept is used in the project. Every example names a real class, and the code excerpts are shortened copies of the actual source under `src/main/java/com/pharmacy/`.

Inheritance was used only where classes genuinely share state or behaviour. Nothing was added just to tick a box.

| Concept | Main examples |
|---|---|
| [Encapsulation](#1-encapsulation) | Model classes, `Prescription`, `Validator`, `LoginAttemptTracker` |
| [Inheritance](#2-inheritance) | `BaseEntity → Person → Patient / Doctor / User`, `AbstractDao`, `BaseService`, `BaseServlet`, `AppException` |
| [Polymorphism](#3-polymorphism) | `Person.getDisplayName()`, `Auditable`, `StockAllocationStrategy`, `AbstractDao.mapRow()`, `BaseServlet.get()/post()` |
| [Abstraction](#4-abstraction) | Interfaces `CrudDao`, `ConnectionProvider`, `PasswordEncoder`, `StockAllocationStrategy`; abstract classes |
| [Constructors, overloading, overriding](#5-constructors-overloading-and-overriding) | `Patient`, `BCryptPasswordEncoder`, `AuditService.log`, `equals/hashCode` |
| [Composition](#6-composition) | `Prescription`, `Invoice`, services built from DAOs, `AppContext` |
| [Custom exceptions](#7-custom-exceptions-and-exception-handling) | `AppException` hierarchy, `TransactionManager`, `BaseServlet` |
| [Separation of concerns](#8-separation-of-concerns-and-reuse) | model / dao / service / servlet / filter / util packages |

---

## 1. Encapsulation

**Idea:** an object keeps its data private and exposes methods, so its rules cannot be bypassed.

**Model classes** (`model/`): every field is `private` and reached through methods.

```java
// model/MedicineBatch.java
public class MedicineBatch extends BaseEntity {
    private LocalDate expiryDate;
    private int quantity;

    public boolean isExpiredOn(LocalDate date) {            // the rule lives with the data
        return expiryDate != null && expiryDate.isBefore(date);
    }
}
```

Other classes ask the batch `isExpired()` instead of comparing dates themselves, so the expiry rule exists in exactly one place.

**`Prescription` protects its item list.** The list is `private final`; callers use `addItem(...)` or `setItems(...)` and can never replace the list object itself. `isFullyDispensed()` answers a question about the internal state without exposing how it is worked out.

**`User` hides the password hash.** The field is `private transient`, so it is not written when a session is serialized, and no JSP ever reads it.

**`Validator`** (`util/Validator.java`) keeps its error map private. Callers add checks with methods such as `required(...)`, `phone(...)`, `positive(...)` and finally call `throwIfInvalid()`.

**`LoginAttemptTracker`** (`service/`) hides its counters and lock times completely. The outside world sees only `isLocked`, `recordFailure` and `recordSuccess`.

## 2. Inheritance

**Idea:** a subclass reuses and extends what a parent class provides.

### 2.1 Entities

```
BaseEntity (abstract)              id, createdAt, updatedAt, isNew(), equals(), hashCode()
├── Person (abstract)              fullName, phone, email, active, getCode(), getDisplayName()
│   ├── Patient                    patientCode, dateOfBirth, gender, address, getAge()
│   ├── Doctor                     doctorCode, specialization, hospital, registrationNo
│   └── User                       username, passwordHash, role, can(Permission)
├── Medicine, MedicineBatch, Supplier, StockTransaction
├── Prescription, PrescriptionItem
├── Dispensing, DispensingItem
└── Invoice, InvoiceItem, Payment, AuditLog
```

`Patient`, `Doctor` and `User` really are people with a name, phone, email and an active flag, so `Person` holds those once. A `Supplier` is a company, so it extends `BaseEntity` directly and not `Person`.

### 2.2 Data access

```java
// dao/AbstractDao.java
public abstract class AbstractDao<T> {
    protected abstract T mapRow(ResultSet rs) throws SQLException;

    protected List<T> query(Connection con, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (rs.next()) { result.add(mapRow(rs)); }
                return result;
            }
        }
    }
}
```

All 15 DAOs (`PatientDao`, `MedicineDao`, `InvoiceDao` ...) extend `AbstractDao`. They inherit the JDBC plumbing (prepared statements, parameter binding, try-with-resources, paging) and contain only SQL and row mapping.

### 2.3 Services, servlets, exceptions

- `BaseService` gives every service its `TransactionManager`, its `AuditService` and the `require(actor, permission)` check. `PatientService`, `InventoryService`, `DispensingService`, `BillingService` and the others extend it.
- `BaseServlet` gives every page controller exception handling, flash messages, parameter parsing and paging. `PatientServlet`, `InvoiceServlet` and the others extend it.
- `AppException` is the root of the exception family (see section 7). `InsufficientStockException` extends `BusinessRuleException`, which extends `AppException`: three levels, each adding meaning.

## 3. Polymorphism

**Idea:** the same call behaves differently depending on the actual object.

### 3.1 Overridden methods in the `Person` family

```java
// model/Person.java
public abstract String getCode();                 // every person has a code, each kind its own
public String getDisplayName() { return fullName; }

// model/Doctor.java
@Override public String getCode() { return doctorCode; }
@Override public String getDisplayName() {        // doctors are shown with their title
    String name = getFullName();
    return name.regionMatches(true, 0, "Dr", 0, 2) ? name : "Dr. " + name;
}
```

The unit test `ModelBehaviourTest.personSubclassesAnswerTheSameQuestionsDifferently` puts a `Patient`, a `Doctor` and a `User` in one `List<Person>` and gets `PAT-000001`, `DOC-000001`, `admin` from the same `getCode()` call.

### 3.2 Interface polymorphism: `Auditable`

```java
// model/Auditable.java
public interface Auditable {
    Long getId();
    String getAuditType();      // "PATIENT", "INVOICE", ...
    String getAuditLabel();
}

// service/AuditService.java
public void log(Connection con, User actor, String action, Auditable entity, String description) { ... }
```

`AuditService` does not know about patients or invoices. It accepts anything `Auditable`: `Patient`, `Doctor`, `User`, `Supplier`, `Medicine`, `Prescription`, `Dispensing`, `Invoice`.

### 3.3 Strategy: `StockAllocationStrategy`

```java
// service/StockAllocationStrategy.java
public interface StockAllocationStrategy {
    List<Allocation> allocate(List<MedicineBatch> batches, int quantity, String medicineName, LocalDate today);
}

// service/FefoAllocationStrategy.java
public class FefoAllocationStrategy implements StockAllocationStrategy { ... }   // first expiry, first out
```

`DispensingService` holds a `StockAllocationStrategy` and simply calls `allocate(...)`. Which batches are chosen depends on the object it was given. A different policy could be plugged in inside `AppContext` without changing the dispensing code.

### 3.4 Template methods

- `AbstractDao.query(...)` calls the abstract `mapRow(rs)`; each DAO supplies its own version.
- `BaseServlet.doGet/doPost` are `final`. They wrap the call in the shared error handling and then call `get(path, req, resp)` or `post(...)`, which each servlet overrides.
- `AppException.getHttpStatus()` returns 500. `NotFoundException` overrides it with 404, `AuthorizationException` with 403, `ValidationException` with 400. `BaseServlet` just asks the exception.

### 3.5 Enums with behaviour

```java
// model/StockTransactionType.java
STOCK_IN("Stock in", true), STOCK_OUT("Stock out", false), DISPENSE("Dispensed", false) ...
public int apply(int balance, int quantity) { return inbound ? balance + quantity : balance - quantity; }
```

`PrescriptionStatus.isEditable()`, `isDispensable()` and `isCancellable()` work the same way: the status itself knows what it allows, so there are no long `if` chains scattered through the services.

## 4. Abstraction

**Idea:** depend on *what* something does, not *how* it does it.

| Abstraction | Kind | Implemented by | Who depends on it |
|---|---|---|---|
| `util/ConnectionProvider` | interface | `PooledConnectionProvider` (HikariCP); a lambda in the integration tests | `TransactionManager` |
| `util/PasswordEncoder` | interface | `BCryptPasswordEncoder` | `AuthService`, `UserService` |
| `util/TransactionCallback<T>` | functional interface | lambdas in every service | `TransactionManager` |
| `dao/CrudDao<T>` | interface | `PatientDao`, `DoctorDao`, `SupplierDao`, `MedicineDao`, `UserDao` | services |
| `dao/RowMapper<T>` | functional interface | method references such as `this::mapItem` | `AbstractDao` |
| `service/StockAllocationStrategy` | interface | `FefoAllocationStrategy` | `DispensingService` |
| `model/Auditable` | interface | 8 entity classes | `AuditService` |
| `BaseEntity`, `Person`, `AbstractDao`, `BaseService`, `BaseServlet` | abstract classes | see section 2 | |

Example: `TransactionManager` only knows `ConnectionProvider.getConnection()`. The web application gives it a connection pool; the integration test gives it `() -> DriverManager.getConnection(url, user, password)`. The same service code runs in both.

## 5. Constructors, overloading and overriding

**Constructors**

```java
// model/Patient.java
public Patient() { }                                              // empty, filled from a form
public Patient(Long id, String patientCode, String fullName) {    // lightweight reference
    super(id, fullName);                                          // calls the Person constructor
    this.patientCode = patientCode;
}
```

- `BCryptPasswordEncoder()` uses cost 12 by calling `this(12)`; `BCryptPasswordEncoder(int cost)` lets tests use a faster cost.
- `AppContext(ConnectionProvider)` and `AppContext(ConnectionProvider, PasswordEncoder)`.
- `ValidationException(Map<String,String> errors)` and `ValidationException(String field, String message)`.
- Services receive their collaborators through constructors (constructor injection); the fields are `final`.

**Method overloading** (same name, different parameters)

| Class | Overloads |
|---|---|
| `AuditService` | `log(con, actor, action, Auditable entity, description)` and `log(con, userId, action, entityType, entityId, description)` |
| `MedicineBatch` | `isExpired()` and `isExpiredOn(LocalDate)` |
| `AbstractDao` | `query(con, sql, params...)` and `query(con, sql, mapper, params...)`; two `page(...)` methods |
| `Validator` | `required(field, String, label)` and `required(field, Object, label)`; `nonNegative` for `Integer` and `BigDecimal` |

**Method overriding** (subclass replaces parent behaviour)

- `BaseEntity` overrides `equals`, `hashCode` and `toString` from `Object`: two entities are equal when they have the same class and the same database id.
- `Doctor.getDisplayName()`, every DAO's `mapRow`, every servlet's `get` / `post` / `homePath`, every exception's `getHttpStatus`.

## 6. Composition

**Idea:** build objects out of other objects ("has-a").

```java
// model/Prescription.java
public class Prescription extends BaseEntity implements Auditable {
    private Patient patient;                                           // has a patient
    private Doctor doctor;                                             // has a doctor
    private final List<PrescriptionItem> items = new ArrayList<>();    // owns its items
}
```

Prescription items have no life of their own: they are saved, loaded and deleted with their prescription (the foreign key uses `ON DELETE CASCADE`). In the same way an `Invoice` is composed of `InvoiceItem`s and `Payment`s, and a `Dispensing` of `DispensingItem`s.

**Services are composed of DAOs and other services:**

```java
// service/DispensingService.java
public DispensingService(TransactionManager tx, AuditService audit, PrescriptionDao prescriptionDao,
                         DispensingDao dispensingDao, BatchDao batchDao, SequenceDao sequenceDao,
                         InventoryService inventory, StockAllocationStrategy allocationStrategy) { ... }
```

`DispensingService` *has an* `InventoryService` and reuses its `recordMovement(...)`, so the "change the quantity and write the ledger row" rule is written once.

**`AppContext`** (`service/AppContext.java`) composes the whole application: it creates each DAO and service once and wires them together.

## 7. Custom exceptions and exception handling

```
RuntimeException
└── AppException                         getHttpStatus() = 500
    ├── ValidationException              400, carries one message per form field
    ├── NotFoundException                404
    ├── AuthenticationException          401, deliberately vague message
    ├── AuthorizationException           403
    ├── BusinessRuleException            409
    │   ├── InsufficientStockException   carries requested and available quantities
    │   ├── ExpiredBatchException
    │   └── DuplicateOperationException  carries the id of the record that already exists
    └── DataAccessException              wraps SQLException; isDuplicateKey(), isTransient()
```

**Throwing:** services throw the most specific type.

```java
// service/FefoAllocationStrategy.java
if (available < quantity) {
    throw new InsufficientStockException(medicineName, quantity, available);
}
```

**Rolling back:** `TransactionManager.inTransaction` catches anything, rolls the transaction back, and rethrows.

```java
// util/TransactionManager.java
try {
    T result = work.execute(con);
    con.commit();
    return result;
} catch (SQLException | RuntimeException | Error e) {
    rollbackQuietly(con, e);
    throw e;
}
```

**Handling in one place:** `BaseServlet.handle(...)` turns exceptions into the right response. A `ValidationException` or `BusinessRuleException` becomes a message on the page the user came from; `NotFoundException` becomes a 404 page; `AuthorizationException` a 403 page; a `DataAccessException` is logged with full detail and the user sees only a safe error page. Individual servlets catch an exception themselves only when they can do something better, such as redisplaying a form with the entered values.

**Resource safety:** every `Connection`, `PreparedStatement` and `ResultSet` is opened in a try-with-resources block, so it is closed even when an exception is thrown.

## 8. Separation of concerns and reuse

| Package | Responsibility | Must not |
|---|---|---|
| `model` | Data and the rules that belong to it | Know about SQL or HTTP |
| `dao` | SQL and row mapping | Contain business rules or open transactions |
| `service` | Business rules, permissions, transactions | Know about HTTP requests or HTML |
| `servlet` | Read the request, call a service, choose a view | Contain SQL |
| `filter` | Cross-cutting web concerns: encoding, login, CSRF | |
| `util` | Reusable helpers: transactions, validation, money, paging | |
| `exception` | The exception family | |
| JSP + tags | Display only | Contain SQL or business rules |

Reuse in practice:

- The JSP tags `ui:input`, `ui:pager`, `ui:th`, `ui:status` and `ui:empty` are used on every list and form page.
- `layout/header.jspf` and `layout/footer.jspf` give every page the same sidebar, top bar, toast area and confirmation dialog.
- One `reports/report.jsp` renders all eight reports from a `ReportResult` object.
- `Where` builds optional search filters for every list query, always with bind parameters.

Other language features used along the way: generics (`AbstractDao<T>`, `Page<T>`, `CrudDao<T>`), a record (`StockAllocationStrategy.Allocation`), static nested classes (`DashboardService.Summary`, `ReportResult.Column`), lambdas and method references, `Optional`, and enums with fields and methods (`Permission`, `PrescriptionStatus`, `StockTransactionType`, `PaymentStatus`).
