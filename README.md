# Pharmacy Prescription Management System

A database-backed pharmacy web application for an OOP college project. It manages patients, doctors, prescriptions, medicines, batch-level inventory, dispensing, billing, reports and audit logs.

Built with **Java Servlets + JSP + JDBC (PreparedStatement) + MySQL**, running on **Apache Tomcat 10.1**. No Spring, no Hibernate, no ORM.

---

## Contents

1. [Features](#1-features)
2. [Technology](#2-technology)
3. [Architecture and folder structure](#3-architecture-and-folder-structure)
4. [Quick start](#4-quick-start)
5. [Detailed setup](#5-detailed-setup)
6. [First administrator account](#6-first-administrator-account)
7. [Roles](#7-roles)
8. [How the main workflows behave](#8-how-the-main-workflows-behave)
9. [How a request flows through JDBC](#9-how-a-request-flows-through-jdbc)
10. [OOP concepts](#10-oop-concepts)
11. [Security](#11-security)
12. [Tests](#12-tests)
13. [What was verified, and what was not](#13-what-was-verified-and-what-was-not)
14. [Troubleshooting](#14-troubleshooting)
15. [Design decisions](#15-design-decisions)

---

## 1. Features

| Module | What it does |
|---|---|
| **Login and roles** | Database users, BCrypt password hashes, session login, Admin and Pharmacist roles enforced on the server |
| **Dashboard** | Patient, doctor, prescription and medicine counts, low stock, near-expiry and expired batches, today's and this month's sales, recent activity, sales-trend chart and stock-by-category chart. Every number comes from MySQL; an empty database shows zeros and empty states |
| **Patients** | Register with an automatic ID (`PAT-000001`), search by ID, name or phone, profile, edit, deactivate, prescription and invoice history, duplicate check |
| **Doctors** | Add, edit, search, deactivate (old prescriptions keep the doctor), automatic ID (`DOC-000001`), prescriptions per doctor |
| **Prescriptions** | Linked to a patient and doctor, automatic number (`RX-000001`), several medicine lines, Draft, Issued, Partially dispensed, Completed and Cancelled states, printable document |
| **Medicines and inventory** | Catalog plus separate batches (batch number, expiry, cost, price, quantity, supplier). Stock in, stock out, adjustments, a stock ledger that is only ever added to, low-stock and expiry alerts |
| **Suppliers** | Add, edit, search, deactivate; batches are linked to their supplier |
| **Dispensing** | Against issued prescriptions only, partial dispensing, FEFO batch selection, expired batches refused, no negative stock, double submission is harmless |
| **Billing** | Invoice per dispensing (`INV-000001`), line discounts (optional), configurable tax, server-side totals in `BigDecimal`, payments, payment status, cancellation with audit trail, printable invoice |
| **Reports** | Sales (day, week, month), billing and payment status, prescription history, inventory, low stock, expiry, stock movements, supplier receipts. Date filters, totals, paging, print layout |
| **Audit log** | Logins, patient changes, prescriptions, stock movements, dispensing, billing, user management |
| **Settings** | Pharmacy details, currency symbol, tax rate, discounts on or off, payment methods, near-expiry window, default reorder level |

The whole interface uses **Times New Roman**. No web font is loaded; icons are SVG shapes rather than an icon font.

## 2. Technology

| Layer | Used |
|---|---|
| Language | Java 17 or newer (builds on Java 17 and Java 21) |
| Web | Jakarta Servlet 6.0, JSP 3.1, JSTL, on Apache Tomcat 10.1 |
| Database | MySQL 8, accessed with JDBC `PreparedStatement` through MySQL Connector/J |
| Connection pool | HikariCP (a plain `javax.sql.DataSource`) |
| Passwords | jBCrypt |
| Front end | HTML5, CSS3, JavaScript, Bootstrap 5.3 with custom CSS, Chart.js 4. All bundled in the project, no CDN |
| Build | Maven, WAR packaging |
| Tests | JUnit 5 unit tests, JDBC integration tests, a Python end-to-end script |

## 3. Architecture and folder structure

```
Browser
   |  HTTP (forms, links, a few JSON calls for charts and lookups)
   v
Filters          EncodingFilter -> AuthFilter -> CsrfFilter
   v
Servlets         com.pharmacy.servlet   read the request, call a service, pick a JSP or redirect
   v
Services         com.pharmacy.service   business rules, permission checks, transaction boundaries
   v
DAOs             com.pharmacy.dao       SQL only, always PreparedStatement
   v
MySQL            database.sql           tables, foreign keys, CHECK and UNIQUE constraints
```

JSP pages only display data that a servlet prepared. There is no SQL in any JSP, and the browser never talks to MySQL.

```
pharmacy-management-system/
├── pom.xml                         Maven build (WAR)
├── database.sql                    Complete MySQL schema and default settings
├── .env.example                    Example environment variables (placeholders)
├── README.md
├── OOP_EXPLANATION.md              Where each OOP concept is used, with class names
├── src/main/java/com/pharmacy/
│   ├── model/        Entities and enums: Patient, Doctor, Medicine, MedicineBatch, Prescription, Invoice ...
│   ├── dao/          AbstractDao plus one DAO per table group; Where (safe filter builder)
│   ├── service/      PatientService, InventoryService, DispensingService, BillingService ... AppContext
│   ├── servlet/      BaseServlet plus one servlet per module (URL mappings by @WebServlet)
│   ├── filter/       EncodingFilter, AuthFilter, CsrfFilter, AppInitListener
│   ├── util/         TransactionManager, AppConfig, Validator, Money, PasswordEncoder, Page ...
│   └── exception/    AppException and its subclasses
├── src/main/resources/
│   └── db.properties.example       Copy to db.properties and enter your database login
├── src/main/webapp/
│   ├── index.jsp                   Redirects to the dashboard
│   ├── assets/css/app.css          All custom styling
│   ├── assets/js/app.js            Shared behaviour (dialogs, toasts, lookups, validation)
│   ├── assets/js/dashboard.js      Charts
│   ├── assets/vendor/              Bootstrap and Chart.js (local copies)
│   ├── assets/img/                 Icon sprite and favicon
│   └── WEB-INF/
│       ├── web.xml                 Filters, session settings, error pages
│       ├── pms.tld                 Formatting functions for JSP (dates, money)
│       ├── tags/                   Reusable JSP tags: input, pager, sortable header, status pill ...
│       └── views/                  All JSP pages (not reachable directly from a browser)
│           └── layout/             header.jspf, footer.jspf (sidebar, top bar, dialogs)
├── src/test/java/com/pharmacy/     JUnit tests
└── tests/e2e/e2e_test.py           End-to-end test of the running application
```

## 4. Quick start

For someone who already has JDK 17+, Maven, MySQL 8 and Tomcat 10.1 installed:

```bash
# 1. create the database and tables
mysql -u root -p < database.sql

# 2. tell the application how to log in to MySQL
cp src/main/resources/db.properties.example src/main/resources/db.properties
#    then edit db.properties: DB_USER, DB_PASSWORD and SETUP_TOKEN

# 3. build
mvn clean package

# 4. deploy: copy target/pharmacy.war into Tomcat's webapps folder and start Tomcat

# 5. open http://localhost:8080/pharmacy  ->  the first-time setup page appears
```

## 5. Detailed setup

### 5.1 Prerequisites

| Software | Version | Check with |
|---|---|---|
| JDK | 17 or 21 | `java -version` |
| Maven | 3.8 or newer | `mvn -version` |
| MySQL Server | 8.0 or newer | `mysql --version` |
| Apache Tomcat | **10.1.x** (not 9, not 11) | see `RELEASE-NOTES` in the Tomcat folder |

Tomcat 9 will not work: it uses the old `javax.servlet` package names, and this project uses `jakarta.servlet`.

### 5.2 MySQL: create the database

From the project folder:

```bash
mysql -u root -p < database.sql
```

On Windows, if `mysql` is not recognised, use the full path, for example:

```bat
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p < database.sql
```

You can also open `database.sql` in MySQL Workbench and run it. The script creates the database `pharmacy_db`, 15 application tables plus `settings` and `sequences`, and the default settings. It does **not** create any login account.

Recommended: create a separate MySQL user for the application instead of using `root`. In the `mysql` client:

```sql
CREATE USER 'pharmacy_app'@'localhost' IDENTIFIED BY 'choose-a-strong-password';
GRANT SELECT, INSERT, UPDATE, DELETE ON pharmacy_db.* TO 'pharmacy_app'@'localhost';
FLUSH PRIVILEGES;
```

### 5.3 Database configuration

The application reads these values, in this order of priority: environment variables, JVM system properties, then `db.properties`.

| Name | Meaning | Example |
|---|---|---|
| `DB_URL` | JDBC URL | `jdbc:mysql://localhost:3306/pharmacy_db?useUnicode=true&characterEncoding=UTF-8` |
| `DB_USER` | MySQL user | `pharmacy_app` |
| `DB_PASSWORD` | MySQL password | |
| `DB_DRIVER` | JDBC driver class (optional) | `com.mysql.cj.jdbc.Driver` |
| `DB_POOL_SIZE` | Maximum pooled connections (optional) | `10` |
| `SETUP_TOKEN` | Secret needed once to create the first administrator | any long random text |
| `COOKIE_SECURE` | `true` when served over HTTPS | `false` |

**Option A, properties file (simplest):**

1. Copy `src/main/resources/db.properties.example` to `src/main/resources/db.properties`.
2. Enter your values.
3. Rebuild with `mvn clean package`.

`db.properties` is listed in `.gitignore`, so your real password is never committed.

**Option B, environment variables:** set the names from the table before starting Tomcat, for example in `TOMCAT_HOME/bin/setenv.bat` (Windows):

```bat
set "DB_URL=jdbc:mysql://localhost:3306/pharmacy_db?useUnicode=true&characterEncoding=UTF-8"
set "DB_USER=pharmacy_app"
set "DB_PASSWORD=your-password"
set "SETUP_TOKEN=your-setup-token"
```

or `TOMCAT_HOME/bin/setenv.sh` (macOS / Linux) with `export DB_URL=...` lines.

### 5.4 Build

```bash
mvn clean package
```

The result is `target/pharmacy.war`. The first build downloads the libraries from Maven Central, so it needs an internet connection.

If Maven uses the wrong Java version, set `JAVA_HOME` to your JDK 17 or 21 folder and run `mvn -version` again to check.

### 5.5 Run on Tomcat 10.1

1. Copy `target/pharmacy.war` into `TOMCAT_HOME/webapps/`.
2. Start Tomcat: `bin\startup.bat` (Windows) or `bin/startup.sh` (macOS / Linux).
3. Open <http://localhost:8080/pharmacy>.
4. To stop: `bin\shutdown.bat` or `bin/shutdown.sh`.

After changing code or `db.properties`: rebuild, replace the WAR in `webapps`, and Tomcat redeploys it.

### 5.6 VS Code

1. Install the extensions **Extension Pack for Java** and **Community Server Connectors** (VS Code offers them automatically from `.vscode/extensions.json`).
2. Open the project folder. VS Code detects `pom.xml` and imports the project.
3. Terminal → New Terminal → `mvn clean package`.
4. In the **Servers** view: *Create New Server* → choose *No, use server on disk* → select your Tomcat 10.1 folder.
5. Right-click the Tomcat server → *Add Deployment* → choose `target/pharmacy.war` → *Start Server*.
6. Open <http://localhost:8080/pharmacy>.

## 6. First administrator account

No account is shipped with the project, and no default password exists.

1. Choose a `SETUP_TOKEN` and put it in `db.properties` (or an environment variable). If you do not set one, the application generates a random token at start-up and prints it once in the Tomcat log (`logs/catalina.out` or the Tomcat console window).
2. Open the application. While the `users` table is empty you are sent to **/setup**.
3. Enter the setup token, a username, your name and a password (at least 8 characters with a letter and a digit).
4. Sign in. The setup page closes permanently as soon as one user exists.

Afterwards, create the other accounts under **Users**.

Then open **Settings** and enter the **tax rate** (enter `0` if no tax applies). Invoices cannot be created until a tax rate has been saved, because the system must not assume one.

## 7. Roles

| Area | Administrator | Pharmacist |
|---|---|---|
| Patients | full, including deactivate | register, edit, view |
| Doctors, suppliers, medicine catalog | full | view only |
| Prescriptions | create, edit, issue, cancel | view only |
| Dispensing | yes | yes |
| Stock in, stock out | yes | yes |
| Stock adjustments | yes | no |
| Invoices and payments | yes | yes |
| Cancel an invoice | yes | no |
| Reports, users, audit log, settings | yes | no |

The rules live in one place, the `Permission` enum. They are enforced twice on the server (`AuthFilter` for whole areas, then every service method), not just by hiding buttons.

## 8. How the main workflows behave

**Prescription life-cycle:** Draft → Issued → Partially dispensed → Completed. A draft or issued prescription with nothing dispensed can still be edited or cancelled. After the first dispensing, the clinical details are frozen. The system records what an authorised person typed; it never suggests a diagnosis, medicine or dose.

**Stock:** quantities live only on batches. Every change writes a row to `stock_transactions` in the same database transaction as the quantity change, and ledger rows are never updated or deleted.

**Expiry:** a batch can be used up to and including its expiry date and counts as expired from the next day. Expired stock is excluded from "usable stock", cannot be received, and is never dispensed. It can only be removed with "Remove stock" (for disposal).

**Dispensing:** in one transaction the service locks the prescription row, checks the form's one-time token, validates quantities against what is still owed, locks the valid batches of each medicine (`SELECT ... FOR UPDATE`), takes stock from the earliest-expiring batch first (FEFO), writes dispensing rows and ledger rows, and updates the prescription status. Any failure rolls everything back.

**Billing:** an invoice is created from one dispensing. Stock is not touched, because it was deducted at dispensing. Amounts:

```
line total  = quantity x unit price - line discount
subtotal    = sum of quantity x unit price
tax         = (subtotal - discounts) x tax rate, rounded half-up to 2 decimals
grand total = subtotal - discounts + tax
```

A dispensing can have only one live invoice. This is checked in the service under a row lock and also guaranteed by a UNIQUE key in the database.

**Invoice cancellation rules:** administrators only, a reason is required, and an invoice with recorded payments cannot be cancelled. A cancelled invoice is kept with status Cancelled, is excluded from sales, and its dispensing can be billed again.

**Deactivation instead of deletion:** patients, doctors, suppliers, medicines and users are never deleted, so history stays intact.

## 9. How a request flows through JDBC

Example: a pharmacist presses **Dispense medicines**.

1. The browser posts the form to `/dispensing/save` with the session cookie and CSRF token.
2. `EncodingFilter` sets UTF-8 and security headers. `AuthFilter` loads the signed-in user from the database. `CsrfFilter` checks the token.
3. `DispensingServlet` reads the quantities and calls `DispensingService.dispense(...)`.
4. The service checks the `DISPENSE` permission and asks `TransactionManager.inTransaction(...)` to run its work.
5. `TransactionManager` borrows a `Connection` from the HikariCP pool, calls `setAutoCommit(false)`, and passes the connection to the service's code.
6. The service calls DAO methods with that connection. Each DAO method builds a `PreparedStatement`, binds parameters with `?` placeholders, executes it and maps the `ResultSet` to objects. All JDBC resources are closed by try-with-resources.
7. If everything succeeded, `TransactionManager` calls `commit()`. If anything threw an exception, it calls `rollback()` and nothing is saved.
8. The servlet stores a success message and redirects to the dispensing page (Post/Redirect/Get), which is rendered by a JSP.

The JDBC plumbing is in three classes: `util/PooledConnectionProvider.java` (connections), `util/TransactionManager.java` (commit and rollback), `dao/AbstractDao.java` (prepared statements, binding, mapping, paging).

## 10. OOP concepts

A full walkthrough with class names and code excerpts is in **[OOP_EXPLANATION.md](OOP_EXPLANATION.md)**. In short:

| Concept | Example in this project |
|---|---|
| Encapsulation | Private fields with methods in every model class; `Prescription` controls its own item list |
| Inheritance | `BaseEntity` → `Person` → `Patient`, `Doctor`, `User`; `AbstractDao` → 15 DAOs; `AppException` → specific exceptions |
| Polymorphism | `Person.getDisplayName()` overridden by `Doctor`; `StockAllocationStrategy` implemented by `FefoAllocationStrategy` |
| Abstraction | Interfaces `CrudDao`, `ConnectionProvider`, `PasswordEncoder`, `Auditable`; abstract classes `AbstractDao`, `BaseServlet` |
| Overloading / overriding | `AuditService.log(...)` twice; `MedicineBatch.isExpired()` and `isExpiredOn(date)`; `mapRow` overridden per DAO |
| Composition | `Prescription` has a `Patient`, a `Doctor` and items; services are composed of DAOs |
| Custom exceptions | `ValidationException`, `InsufficientStockException`, `DuplicateOperationException` ... handled centrally in `BaseServlet` |

## 11. Security

- **SQL injection:** every statement is a `PreparedStatement` with bound parameters. Sort columns come from a whitelist.
- **Passwords:** BCrypt (cost 12). Never stored or logged in plain text.
- **Sessions:** new session on login, invalidated on logout, 30-minute timeout, cookie is HttpOnly and SameSite=Lax, and Secure when `COOKIE_SECURE=true`. Session IDs are never put in URLs.
- **Authorization:** checked on the server for every request and again in every service method. The user is re-read from the database on each request, so deactivating an account takes effect immediately.
- **CSRF:** a random per-session token is required on every POST, including login and logout.
- **XSS:** all dynamic output is HTML-escaped (`<c:out>` / `fn:escapeXml`). A Content-Security-Policy allows scripts only from the application itself; there is no inline JavaScript.
- **Brute force:** five failed logins lock that username for five minutes. Error messages do not reveal whether the username exists.
- **Direct access:** JSP pages are under `WEB-INF` and cannot be requested by URL.
- **Secrets:** database credentials and the setup token come from configuration, not source code.
- **Audit log:** stores who, what, which record and when. No passwords, tokens or clinical details.

For a real deployment you would also put the application behind HTTPS and set `COOKIE_SECURE=true`.

## 12. Tests

### 12.1 Unit tests (no database needed)

```bash
mvn test
```

45 tests: FEFO allocation, invoice arithmetic and rounding, validation rules, BCrypt hashing, the permission matrix, model behaviour, safe SQL filter building, paging.

### 12.2 Database integration tests

13 more tests run real JDBC transactions against MySQL: FEFO with the ledger, expired batches, rollback of a half-finished dispensing, two simultaneous dispensings competing for the same stock, the same form submitted three times at once, duplicate billing under concurrency, unique ID generation under concurrency.

They are skipped unless you point them at a **test** database (its name must contain `test`; all its tables are dropped and re-created):

```sql
CREATE DATABASE pharmacy_test CHARACTER SET utf8mb4;
CREATE USER 'pharmacy_test'@'localhost' IDENTIFIED BY 'test-password';
GRANT ALL ON pharmacy_test.* TO 'pharmacy_test'@'localhost';
```

```bash
# macOS / Linux
TEST_DB_URL="jdbc:mysql://localhost:3306/pharmacy_test" TEST_DB_USER=pharmacy_test TEST_DB_PASSWORD=test-password mvn test
```

```bat
:: Windows (cmd)
set TEST_DB_URL=jdbc:mysql://localhost:3306/pharmacy_test
set TEST_DB_USER=pharmacy_test
set TEST_DB_PASSWORD=test-password
mvn test
```

### 12.3 End-to-end test of the running application

`tests/e2e/e2e_test.py` behaves like a browser (session cookie, CSRF token, form posts) and runs 151 checks: setup, login, lockout, role restrictions, CRUD and validation, search, paging and sorting, stock rules, prescriptions, FEFO dispensing, partial and repeated dispensing, billing totals, duplicate billing, payments, cancellation, reports, the JSON API, audit log and logout.

Run it only against a **fresh test database** (it creates users, patients and invoices). Python 3.8+ is needed, with no extra packages:

```bash
python3 tests/e2e/e2e_test.py --base http://localhost:8080/pharmacy --setup-token YOUR_SETUP_TOKEN
```

## 13. What was verified, and what was not

This project was built and tested in a sandbox with **Java 21, Maven 3.9.11, MySQL 8.0.46 and Tomcat 10.1.55**.

**Verified by actually running it:**

- All 115 Java source files compile with `javac --release 17`.
- `database.sql` imports cleanly into MySQL 8.0.46.
- The application deploys on Tomcat 10.1.55 and connects to MySQL through JDBC and the connection pool.
- All 41 JSP pages compile.
- 45 unit tests pass.
- 13 database integration tests pass against MySQL 8.0.46, repeated 6 times to check that the concurrency tests are stable.
- 151 end-to-end checks pass against the deployed application on a fresh database, with no errors in the Tomcat log.
- In headless Chromium: 39 pages loaded with no JavaScript errors, no Content-Security-Policy violations and no failed requests; the computed `font-family` of every element on every page starts with Times New Roman; charts, stock alerts, autocomplete, dialogs, client-side validation and the sidebar work; a full prescription → dispense → invoice → payment flow was driven through the real forms; no horizontal scrolling at phone width.

**Not verified exactly as shipped (please read):**

The sandbox could not reach Maven Central, so the `pom.xml` in this project could not be run with its declared library versions. Instead:

- A Maven build **was** run offline using the same project layout with library and plugin versions available locally. It produced `pharmacy.war` and ran the unit tests through Surefire.
- The runtime tests used substitute libraries of the same kind:

| Purpose | Declared in `pom.xml` | Used in the sandbox tests |
|---|---|---|
| JDBC driver | MySQL Connector/J 8.4.0 | MariaDB Connector/J 2.7.6 (talking to real MySQL 8.0.46) |
| JSTL | Glassfish JSTL 2.0.0 | Apache Taglibs 1.2.5, Jakarta build (same taglib URIs) |
| Connection pool | HikariCP 5.1.0 | HikariCP 2.7.9 |
| Logging bridge | SLF4J 2.0.13 | SLF4J 1.7.32 |
| JUnit | 5.10.2 | 5.10.1 |

The code uses only standard JDBC, Servlet, JSP and JSTL APIs, so no difference is expected. Even so, your first `mvn clean package` will be the first time these exact versions are downloaded and combined. If something fails there, section 14 lists the likely causes.

Also not tested: Windows and macOS (only Linux), browsers other than Chromium, and real printers (the print layout was checked with print-media emulation only).

## 14. Troubleshooting

| Problem | Likely cause and fix |
|---|---|
| Page says "The application is not configured yet" | `db.properties` is missing or `DB_URL` / `DB_USER` are not set. Follow section 5.3 and rebuild. |
| "Something went wrong" on every page; log shows `Access denied for user` | Wrong `DB_USER` or `DB_PASSWORD`, or the MySQL user has no rights on `pharmacy_db`. Re-run the `GRANT` in section 5.2. |
| Log shows `Communications link failure` | MySQL is not running, or the host or port in `DB_URL` is wrong. |
| Log shows `Public Key Retrieval is not allowed` | For a local development database, add `&allowPublicKeyRetrieval=true&useSSL=false` to the end of `DB_URL`. |
| Log shows `Unknown database 'pharmacy_db'` or `Table ... doesn't exist` | `database.sql` was not imported. Run section 5.2. |
| Log shows `Unknown sequence` | The seed rows are missing: import `database.sql` again (it is safe to re-run). |
| HTTP 404 for `/pharmacy` | The WAR is not in `webapps`, or it has a different name. The address is `http://localhost:8080/<war-name>`. |
| `ClassNotFoundException: javax.servlet...` or the app does not start on Tomcat 9 | Use Tomcat **10.1**. |
| JSP error `The absolute uri: [http://java.sun.com/jsp/jstl/core] cannot be resolved` | The JSTL jars are missing from the WAR. Run `mvn clean package` again and check that `WEB-INF/lib` in the WAR contains two `jakarta.servlet.jsp.jstl` jars. |
| Port 8080 already in use | Stop the other program, or change the port in Tomcat's `conf/server.xml`. |
| "The setup token is not correct" | Use the `SETUP_TOKEN` value you configured. If you did not configure one, look for "one-time setup token" in the Tomcat log. |
| Forgot the only administrator password | There is no back door. On a development database you can start again: `DROP DATABASE pharmacy_db;`, import `database.sql` and run the setup page again. This erases all data, so create a second administrator account early. |
| "No tax rate has been configured" when creating an invoice | Sign in as administrator → Settings → enter the tax rate (0 is allowed) → Save. |
| HTTP 403 after leaving a page open for a long time | The session expired, so the form's CSRF token is no longer valid. Reload the page and sign in again. |
| Times shown are off by some hours | The JVM and MySQL use different time zones. The application writes timestamps from Java, so set the JVM time zone (for example `-Duser.timezone=Asia/Kolkata` in `CATALINA_OPTS`). |
| `mvn` reports `invalid target release: 17` | Maven is running on an older JDK. Point `JAVA_HOME` at JDK 17 or 21. |

Tomcat's log files are in `TOMCAT_HOME/logs` (`catalina.*.log` and `localhost.*.log`). The application logs the technical cause there and shows users only a safe message.

## 15. Design decisions

Choices made where the specification left room:

- **Who enters prescriptions:** administrators only; pharmacists view and dispense. This follows the role list in the specification. To let pharmacists enter prescriptions, add `Role.PHARMACIST` to `PRESCRIPTION_MANAGE` in `model/Permission.java`.
- **Dispensing always needs a prescription.** There is no over-the-counter sale screen.
- **One invoice per dispensing**, covering all of its lines.
- **Two extra tables** beyond the 15 required: `settings` (configurable values) and `sequences` (counters for the readable IDs).
- **Invoice totals are stored**, on purpose. An invoice is a frozen record of what was charged; prices and the tax rate can change later. The totals are computed on the server and saved in the same transaction as the lines.
- **Readable numbers have no gaps on failure:** the counter is updated inside the business transaction, so a rolled-back operation gives its number back. The cost is that two dispensings are saved one after the other rather than truly in parallel, which is fine at pharmacy scale.
- **Reports are administrator-only.** Pharmacists still see low-stock and expiry information on the dashboard, the stock alerts menu and the inventory filters.
- **Sales** on the dashboard and in reports means invoiced totals of non-cancelled invoices, by invoice date.
- **No sample data is shipped**, so nothing on screen is invented. The end-to-end test creates its own data in a test database.
- **JSTL 2.0** is used rather than 3.0 because it registers the classic `http://java.sun.com/jsp/jstl/core` taglib URI that the pages use, and it runs on Tomcat 10.1.
