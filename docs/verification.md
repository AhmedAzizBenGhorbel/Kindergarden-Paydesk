# Verification and reproducible lab checks

On 29 September 2026, [GitHub Actions run 36510643662](https://github.com/AhmedAzizBenGhorbel/Kindergarden-Paydesk/actions/runs/36510643662) compiled the application and passed **16 tests, with zero failures, errors, or skipped tests**, at commit `61888b1af805cf6fa08d14c7776e396572558013`. The runner used Ubuntu and Temurin JDK 25.

Run the same isolated suite from the project root:

```sh
mvn --batch-mode --no-transfer-progress clean test
```

The suite uses H2 in MySQL compatibility mode for transaction fixtures, temporary files, fake dump processes, and synthetic account/configuration values. It does not require a running MySQL server or a graphical desktop. H2 checks transaction behavior; it does not establish full MySQL compatibility.

| Area | Checks |
| --- | --- |
| Payments (4 tests) | Linked payment/receipt commit; duplicate receipt rolls back the second payment; invalid amounts avoid database access; partial balances, advances, and status calculations. |
| Configuration/login (5 tests) | Defaults and environment precedence; invalid host/database/port inputs; database failure differs from unknown user; invalid local configuration can be retried; safe error classification. |
| Backups (7 tests) | Unique files and shared connection settings; failed dumps remove partial files and redact the synthetic password; timeout stops the process; empty output is failure; invalid paths; missing executable; configuration reload between attempts. |

## Lab: duplicate receipt rolls back a payment

**Symptom:** a new payment is valid, but its receipt number already exists. Without a shared transaction, this can leave a payment without its intended receipt.

**Reproduction:** run the isolated fixture:

```sh
mvn "-Dtest=PaymentTransactionTest#duplicateReceiptRollsBackSecondPayment" test
```

**Evidence and cause:** the test first saves one linked pair, then reuses its receipt number for a second payment. The receipt uniqueness constraint rejects the second insert. The transaction service reports failure, and the database still contains one payment and one receipt.

**Implementation and verification:** both inserts share one JDBC connection with auto-commit disabled; a failed insert rolls back the unit of work. The regression test verifies row counts through a separate connection. This is a synthetic lab, not a customer incident.

## Lab: missing backup executable

```sh
mvn "-Dtest=BackupServiceTest#missingExecutableIsReproducibleWithoutDatabase" test
```

The test selects an intentionally nonexistent dump command. Starting the process fails, the backup reports failure with guidance about mysqldump/PATH/folder permissions, and the temporary destination contains no partial SQL file. A real installation would require correcting the executable path or installing the MySQL client tools, then checking a real dump and restore separately.

## Limits of this evidence

The JavaFX interface, actual MySQL initialization and privilege handling, real mysqldump output, Windows-specific temporary-file permissions, and a database restore were **not exercised** in this run. The [restore exercise](restore-exercise.md) remains proposed. Local Windows compilation was blocked by file-access restrictions; the successful compilation and test results above came from the GitHub runner. Older DAO operations still contain stack-trace logging, so inspect and redact local diagnostic captures before sharing them.
