# Kindergarden Paydesk

Kindergarden Paydesk is a desktop JavaFX application for tracking kindergarten payments at Garderie Almohtadine. It focuses on monthly payment records, partial payments, extras, receipts data, local backups, and activity logs. The app is built as a university integration project and is intentionally kept simple and beginner-friendly.

## Key Features

- Login, logout, and change password
- Role handling for `Administrateur` and `Personnel`
- Dashboard with summary totals and recent payments
- Children management
- School years and active months
- Monthly payment record generation
- Extras and recurring fees
- Payment entries with receipt data storage
- Search and filters
- Local backup using `mysqldump`
- Settings screen
- Activity logs

## Tech Stack

- Java 25
- JavaFX
- FXML
- CSS
- Maven
- JDBC
- MySQL
- `mysql-connector-j` 9.5.0

## Prerequisites

- JDK 25
- Maven 3.9+ recommended
- MySQL Server running locally on `localhost:3306`
- phpMyAdmin optional, for inspecting the database
- `mysqldump` available through MySQL, XAMPP, or the system `PATH` if you want to use the backup feature

## Database Setup

The application uses this database name:

```text
almohtadine_paydesk_db
```

Create it with:

```sql
CREATE DATABASE IF NOT EXISTS almohtadine_paydesk_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

Then initialize the schema by importing these files in order:

1. `src/main/resources/database/schema.sql`
2. `src/main/resources/database/seed.sql`

If you are using XAMPP on Windows, you can also run:

```bash
C:\xampp\mysql\bin\mysql.exe -u root < src/main/resources/database/schema.sql
C:\xampp\mysql\bin\mysql.exe -u root almohtadine_paydesk_db < src/main/resources/database/seed.sql
```

The seed file creates the default admin account. On first successful login, the app upgrades the temporary seeded password storage to PBKDF2 hashing automatically.

Default login:

- Username: `admin`
- Password: `admin123`

## How to Run

From the project root:

```bash
mvn clean javafx:run
```

## Project Structure

- `src/main/java/almohtadinepaydesk` contains the Java source code
- `src/main/java/almohtadinepaydesk/controllers` contains JavaFX controllers
- `src/main/java/almohtadinepaydesk/dao` contains database access classes
- `src/main/java/almohtadinepaydesk/models` contains domain models and enums
- `src/main/java/almohtadinepaydesk/services` contains business logic
- `src/main/java/almohtadinepaydesk/utils` contains shared helpers
- `src/main/java/almohtadinepaydesk/security` contains session and password utilities
- `src/main/resources/fxml` contains the JavaFX layouts
- `src/main/resources/css` contains the application stylesheet
- `src/main/resources/database` contains the SQL schema and seed files

## University Project Note

This application is a university integration project. It is designed for learning and demonstration purposes, with a desktop-only workflow and a simplified architecture suitable for student-level maintenance.
